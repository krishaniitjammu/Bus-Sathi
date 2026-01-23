#!/usr/bin/env python3
"""
export_trip_csv.py

Standalone Python script to export a Firestore trip document's `routePoints`
into a CSV file and optionally upload it to Google Cloud Storage (Firebase Storage).

Usage:
  python tools/export_trip_csv.py --tripId <TRIP_ID> [--out <file.csv>] [--upload] [--bucket <bucketName>] [--days <signedUrlDays>] [--key <serviceAccount.json>]

Dependencies:
  pip install firebase-admin google-cloud-storage

Security:
  Use a service account JSON key or set GOOGLE_APPLICATION_CREDENTIALS to a valid path.
"""

import argparse
import csv
import math
import os
import sys
import html
from datetime import datetime, timedelta, timezone

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
    from google.cloud import storage as gcs
except Exception as e:
    print('Missing dependencies. Run: pip install firebase-admin google-cloud-storage')
    raise


def parse_args():
    p = argparse.ArgumentParser(description='Export trip routePoints to CSV and optionally upload to Cloud Storage')
    p.add_argument('--tripId', required=True, help='Firestore trip document ID')
    p.add_argument('--out', help='Output CSV path (default: trip_<tripId>.csv)')
    p.add_argument('--upload', action='store_true', help='Upload CSV to Cloud Storage')
    p.add_argument('--bucket', help='GCS bucket name to upload to (defaults to project default bucket)')
    p.add_argument('--days', type=int, default=7, help='Signed URL expiry in days (default 7)')
    p.add_argument('--key', help='Path to service account JSON file (optional)')
    p.add_argument('--gpx', action='store_true', help='Write GPX file alongside CSV')
    p.add_argument('--gpx-out', help='Output GPX path (default: <tripDocId>.gpx)')
    return p.parse_args()


def haversine_distance_m(lat1, lon1, lat2, lon2):
    R = 6371000
    to_rad = math.radians
    dlat = to_rad(lat2 - lat1)
    dlon = to_rad(lon2 - lon1)
    a = math.sin(dlat / 2) ** 2 + math.cos(to_rad(lat1)) * math.cos(to_rad(lat2)) * math.sin(dlon / 2) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c


def points_to_csv_str(points):
    lines = []
    lines.append(['timestamp', 'latitude', 'longitude', 'accuracy', 'cumulative_m'])
    cum = 0.0
    for i, p in enumerate(points):
        lat = None
        lon = None
        acc = ''
        ts = None
        if isinstance(p, dict):
            lat = p.get('latitude', p.get('lat'))
            lon = p.get('longitude', p.get('lon'))
            acc = p.get('accuracy', '')
            ts = p.get('timestamp', p.get('time'))
        else:
            continue

        iso = ''
        if ts is not None:
            try:
                # timestamps appear to be in millis; use timezone-aware UTC
                iso = datetime.fromtimestamp(int(ts) / 1000, timezone.utc).isoformat().replace('+00:00', 'Z')
            except Exception:
                iso = str(ts)

        if i > 0 and lat is not None and lon is not None:
            prev = points[i - 1]
            prev_lat = prev.get('latitude', prev.get('lat'))
            prev_lon = prev.get('longitude', prev.get('lon'))
            if prev_lat is not None and prev_lon is not None:
                try:
                    d = haversine_distance_m(float(prev_lat), float(prev_lon), float(lat), float(lon))
                    cum += d
                except Exception:
                    pass

        lines.append([iso, lat if lat is not None else '', lon if lon is not None else '', acc, f"{cum:.2f}"])

    # Convert to CSV string
    out = []
    for row in lines:
        out.append(','.join(str(x) for x in row))
    return '\n'.join(out) + '\n'


def main():
    args = parse_args()

    # Initialize Firebase Admin
    try:
        if args.key:
            cred = credentials.Certificate(args.key)
            firebase_admin.initialize_app(cred, {
                # If bucket provided we will use it later explicitly
            })
        else:
            # Use ADC if available
            firebase_admin.initialize_app()
    except Exception as e:
        print('Failed to initialize Firebase Admin SDK:', e)
        sys.exit(1)

    db = firestore.client()

    doc_ref = db.collection('trips').document(args.tripId)
    snap = doc_ref.get()
    if not snap.exists:
        print(f'Trip document {args.tripId} not found')
        sys.exit(1)

    trip = snap.to_dict()
    points = trip.get('routePoints') or trip.get('route_points') or []
    if not isinstance(points, list):
        print('routePoints is not a list, aborting')
        sys.exit(1)

    csv_str = points_to_csv_str(points)

    # Use stored trip id (e.g., BUS123_20260107_153000) when available for filename
    doc_trip_id = trip.get('id') or snap.id or args.tripId
    safe_doc_trip_id = str(doc_trip_id)
    out_path = args.out or f'{safe_doc_trip_id}.csv'

    # Build metadata header (human-readable fields) and include formatted start/end times
    def _fmt_epoch(ms):
        try:
            # timezone-aware UTC
            return datetime.fromtimestamp(int(ms) / 1000, timezone.utc).strftime('%d:%m:%Y %H:%M:%S')
        except Exception:
            return str(ms)

    meta_lines = []
    meta_lines.append(f'# tripId: {safe_doc_trip_id}')
    meta_lines.append(f'# firestoreDocId: {snap.id}')
    if trip.get('driverId'):
        meta_lines.append(f'# driverId: {trip.get("driverId")}')
    if trip.get('driverName'):
        meta_lines.append(f'# driverName: {trip.get("driverName")}')

    start_str = trip.get('startTimeString') or ( _fmt_epoch(trip.get('startTime')) if trip.get('startTime') else '' )
    end_str = trip.get('endTimeString') or ( _fmt_epoch(trip.get('endTime')) if trip.get('endTime') else '' )

    if trip.get('startTime'):
        meta_lines.append(f'# startTime: {trip.get("startTime")}')
    if start_str:
        meta_lines.append(f'# startTimeString: {start_str}')
    if trip.get('endTime'):
        meta_lines.append(f'# endTime: {trip.get("endTime")}')
    if end_str:
        meta_lines.append(f'# endTimeString: {end_str}')

    meta_lines.append('')  # blank line before CSV header

    with open(out_path, 'w', encoding='utf-8', newline='') as f:
        for ml in meta_lines:
            f.write(ml + '\n')
        f.write(csv_str)

    print('CSV written to', out_path)

    # Create GPX file if requested
    if args.gpx:
        try:
            def _ts_iso(ms):
                try:
                    # ISO 8601 UTC with trailing Z
                    return datetime.fromtimestamp(int(ms) / 1000, timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
                except Exception:
                    return ''

            def points_to_gpx(points, trip):
                parts = []
                parts.append('<?xml version="1.0" encoding="UTF-8"?>')
                parts.append('<gpx version="1.1" creator="BusTrackerApp" xmlns="http://www.topografix.com/GPX/1/1">')
                parts.append('  <metadata>')
                parts.append(f'    <name>{html.escape(safe_doc_trip_id)}</name>')
                desc_items = []
                if trip.get('driverName'):
                    desc_items.append(f'Driver: {trip.get("driverName")}')
                if trip.get('startTimeString'):
                    desc_items.append(f'Start: {trip.get("startTimeString")}')
                if trip.get('endTimeString'):
                    desc_items.append(f'End: {trip.get("endTimeString")}')
                if desc_items:
                    parts.append(f'    <desc>{html.escape(" | ".join(desc_items))}</desc>')
                parts.append('  </metadata>')
                parts.append('  <trk>')
                parts.append('    <name>Route</name>')
                parts.append('    <trkseg>')

                point_written = 0
                for p in points:
                    try:
                        if not isinstance(p, dict):
                            continue
                        lat = p.get('latitude', p.get('lat'))
                        lon = p.get('longitude', p.get('lon'))
                        acc = p.get('accuracy') or ''
                        ts = p.get('timestamp', p.get('time'))
                        if lat is None or lon is None:
                            continue
                        time_iso = _ts_iso(ts) if ts is not None else ''
                        parts.append(f'      <trkpt lat="{lat}" lon="{lon}">')
                        if time_iso:
                            parts.append(f'        <time>{time_iso}</time>')
                        if acc != '':
                            parts.append('        <extensions>')
                            parts.append(f'          <accuracy>{html.escape(str(acc))}</accuracy>')
                            parts.append('        </extensions>')
                        parts.append('      </trkpt>')
                        point_written += 1
                    except Exception as e:
                        print('Warning: failed to write a GPX trkpt:', e)
                        continue

                parts.append('    </trkseg>')
                parts.append('  </trk>')
                parts.append('</gpx>')
                gpx_body = '\n'.join(parts) + '\n'
                return gpx_body, point_written

            gpx_path = args.gpx_out or f'{safe_doc_trip_id}.gpx'
            gpx_str, written = points_to_gpx(points, trip)
            if written == 0:
                print('Warning: GPX had no track points (no lat/lon found), not writing GPX file')
                gpx_path = None
            else:
                with open(gpx_path, 'w', encoding='utf-8') as f:
                    f.write(gpx_str)
                print('GPX written to', gpx_path)
        except Exception as e:
            print('Failed to generate GPX:', e)
            gpx_path = None
    if args.upload:
        # Upload using google-cloud-storage client (needs credentials)
        try:
            if args.key:
                gcs_client = gcs.Client.from_service_account_json(args.key)
            else:
                gcs_client = gcs.Client()
        except Exception as e:
            print('Failed to initialize GCS client:', e)
            sys.exit(1)

        bucket_name = args.bucket
        if not bucket_name:
            # try to get default bucket from firebase app options
            try:
                bucket_name = firebase_admin.get_app().project_id + '.appspot.com'
            except Exception:
                pass

        if not bucket_name:
            print('Bucket name not specified and default bucket not found; use --bucket')
            sys.exit(1)

        bucket = gcs_client.bucket(bucket_name)
        ts_suffix = int(datetime.utcnow().timestamp())
        csv_dest = f'trips_csv/{safe_doc_trip_id}_{ts_suffix}.csv'
        blob_csv = bucket.blob(csv_dest)
        blob_csv.upload_from_filename(out_path, content_type='text/csv')
        print('Uploaded CSV to gs://{}/{}'.format(bucket.name, csv_dest))

        # Generate signed URL for CSV
        csv_url = None
        try:
            csv_url = blob_csv.generate_signed_url(expiration=timedelta(days=args.days), version='v4')
            print(f'Signed CSV URL (expires in {args.days} days): {csv_url}')
        except Exception as e:
            print('Warning: failed to generate signed CSV URL:', e)

        upd = {'csvPath': csv_dest}
        if csv_url:
            upd['csvUrl'] = csv_url

        gpx_dest = None
        gpx_url = None
        if args.gpx:
            gpx_dest = f'trips_gpx/{safe_doc_trip_id}_{ts_suffix}.gpx'
            blob_gpx = bucket.blob(gpx_dest)
            blob_gpx.upload_from_filename(gpx_path, content_type='application/gpx+xml')
            print('Uploaded GPX to gs://{}/{}'.format(bucket.name, gpx_dest))
            try:
                gpx_url = blob_gpx.generate_signed_url(expiration=timedelta(days=args.days), version='v4')
                print(f'Signed GPX URL (expires in {args.days} days): {gpx_url}')
            except Exception as e:
                print('Warning: failed to generate signed GPX URL:', e)
            if gpx_dest:
                upd['gpxPath'] = gpx_dest
            if gpx_url:
                upd['gpxUrl'] = gpx_url

        doc_ref.update(upd)
        print('Trip document updated with csvPath/csvUrl and gpxPath/gpxUrl (if available)')


if __name__ == '__main__':
    main()
