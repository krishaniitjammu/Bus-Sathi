# Export Trip CSV (Admin script)

This folder contains a standalone Node.js script to export a single trip's `routePoints` from Firestore into a CSV file and optionally upload it to Firebase Storage.

## Prerequisites

- Node.js 16+ (LTS recommended)
- Install dependency:

```bash
npm install firebase-admin
```

- Service account key JSON with Firestore and Storage access OR set `GOOGLE_APPLICATION_CREDENTIALS` to point to the JSON file path.

## Usage

```
node export_trip_csv.js --tripId <TRIP_ID> [--out <file.csv>] [--upload] [--bucket <bucketName>] [--days <signedUrlDays>] [--key <serviceAccount.json>]
```

Examples:

Node.js script:

- Export locally:

```
node export_trip_csv.js --tripId abc123
```

- Export and upload to default Firebase Storage bucket with 7-day signed URL:

```
node export_trip_csv.js --tripId abc123 --upload
```

- Export, upload, and write signed URL to trip document, specifying a bucket and a custom key:

```
node export_trip_csv.js --tripId abc123 --upload --bucket my-bucket --key ./serviceAccount.json --days 14
```

Python script:

- Export locally:

```
python export_trip_csv.py --tripId abc123
```

- Export and upload to default Firebase Storage bucket with 7-day signed URL:

```
python export_trip_csv.py --tripId abc123 --upload
```

- Export, upload, and write signed URL to trip document (explicit key/bucket):

```
python export_trip_csv.py --tripId abc123 --upload --bucket my-bucket --key ./serviceAccount.json --days 14
```

## Notes

- The script writes a `csvPath` and `csvUrl` to the trip document when uploading, so you can find the CSV from Firestore.
- Keep service account keys secure. Do not commit them to source control.
- For large/automated workflows, consider using a Cloud Function (recommended) or BigQuery export instead.
