# How to View Your Trip Data in Firebase

## Accessing Firebase Console

1. **Open Firebase Console**

   - Go to [https://console.firebase.google.com](https://console.firebase.google.com)
   - Sign in with your Google account (the same one you used to create the Firebase project)

2. **Select Your Project**
   - Click on your project: **"bus-tracker-1c0bb"**

## Viewing Trip Data in Firestore

### Method 1: Firebase Console (Web Interface)

1. **Navigate to Firestore Database**

   - In the left sidebar, click **"Firestore Database"**
   - You'll see the Firestore data viewer

2. **Browse the Trips Collection**

   - Click on the **"trips"** collection
   - You'll see a list of all trip documents
   - Each document represents one completed trip

3. **View a Specific Trip**

   - Click on any trip document ID to expand it
   - You'll see all the trip data:
     - `id`: Unique trip identifier
     - `driverId`: Firebase Auth UID of the driver
     - `driverName`: Name of the driver who recorded the trip (preferred over email)
   - `driverEmail`: Email of the driver (kept for backward compatibility)
     - `startTime`: Timestamp when trip started (milliseconds)
     - `endTime`: Timestamp when trip ended (milliseconds)
     - `totalDistance`: Total distance traveled in kilometers
     - `status`: Trip status ("COMPLETED")
     - `routePoints`: Array of GPS coordinates

4. **View Route Points**
   - Click the arrow next to **"routePoints"** to expand the array
   - Each item in the array contains:
     - `latitude`: GPS latitude
     - `longitude`: GPS longitude
     - `timestamp`: When this point was recorded
     - `accuracy`: GPS accuracy in meters

### Method 2: Using Firebase CLI (Command Line)

If you have Firebase CLI installed:

```bash
# Login to Firebase
firebase login

# Get all trips
firebase firestore:get trips

# Get a specific trip by ID
firebase firestore:get trips/YOUR_TRIP_ID
```

### Method 3: Export Data

**Export as JSON:**

1. In Firestore console, click on a trip document
2. Click the **three dots menu** (⋮) in the top right
3. Select **"Export document"**
4. Choose JSON format
5. Save the file

**Example exported trip data:**

```json
{
  "id": "abc123-def456-ghi789",
  "id": "BUS123_20260107_153000",  # Trip document ID is now `busNumber_yyyyMMdd_HHmmss` when possible
  "driverId": "firebase_user_id",
  "driverName": "John Doe",
  "driverEmail": "driver@test.com",
  "startTime": 1610000000000,
  "startTimeString": "07:01:2026 15:30:00",
  "endTime": 1610000030000,
  "endTimeString": "07:01:2026 15:35:00",
  "startTime": 1704567890000,
  "endTime": 1704568990000,
  "totalDistance": 5.42,
  "status": "COMPLETED",
  "routePoints": [
    {
      "latitude": 37.422,
      "longitude": -122.084,
      "timestamp": 1704567890000,
      "accuracy": 10.5
    },
    {
      "latitude": 37.423,
      "longitude": -122.085,
      "timestamp": 1704567895000,
      "accuracy": 8.2
    }
    // ... more points
  ]
}
```

## Visualizing Route on a Map

### Using Google My Maps

1. **Export route points** from Firebase (as shown above)
2. **Create a GPX file** from the route points:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1">
  <trk>
    <name>Bus Trip - [Trip ID]</name>
    <trkseg>
      <trkpt lat="37.422" lon="-122.084">
        <time>2024-01-06T12:00:00Z</time>
      </trkpt>
      <trkpt lat="37.423" lon="-122.085">
        <time>2024-01-06T12:00:05Z</time>
      </trkpt>
      <!-- Add more points -->
    </trkseg>
  </trk>
</gpx>
```

3. **Import to Google My Maps**:
   - Go to [Google My Maps](https://www.google.com/maps/d/)
   - Click **"Create a new map"**
   - Click **"Import"**
   - Upload your GPX file
   - The route will be displayed on the map!

### Using Online Tools

You can also use these online tools to visualize GPS data:

- [GPS Visualizer](https://www.gpsvisualizer.com/) - Paste coordinates or upload GPX
- [Mapbox](https://www.mapbox.com/) - Create custom maps
- [Kepler.gl](https://kepler.gl/) - Advanced data visualization

## Querying Trip Data

### Filter Trips by Driver

In Firestore console you can filter by `driverId` (recommended) or `driverName`:

1. Click on **"trips"** collection
2. Click **"Start collection"** or the filter icon
3. Add a filter (example using `driverId`):
   - Field: `driverId`
   - Operator: `==`
   - Value: The driver's UID (e.g., `firebase_user_id`)
4. Click **"Apply"**

Or filter by name:

1. Add a filter:
   - Field: `driverName`
   - Operator: `==`
   - Value: Driver's name (e.g., `John Doe`)

### Filter Trips by Date

1. Add a filter:
   - Field: `startTime`
   - Operator: `>=`
   - Value: Timestamp in milliseconds (e.g., `1704067200000` for Jan 1, 2024)
2. Add another filter for end date if needed

### Sort Trips

1. Click the column header to sort by:
   - `startTime` - Sort by when trips started
   - `totalDistance` - Sort by distance traveled
   - `endTime` - Sort by when trips ended

## Understanding the Data

### Timestamps

- Stored as milliseconds since Unix epoch
- Convert to readable date:
  - In JavaScript: `new Date(timestamp)`
  - Online: Use [Epoch Converter](https://www.epochconverter.com/)

### Distance

- Stored in kilometers
- To convert to miles: `distance * 0.621371`

### Accuracy

- Stored in meters
- Lower values = more accurate GPS reading
- Typical values: 5-50 meters

## Downloading All Trip Data

### Using Firebase Console

1. Go to **Firestore Database**
2. Click the **three dots menu** (⋮) next to "Cloud Firestore"
3. Select **"Export"**
4. Choose a Cloud Storage bucket
5. Select the **"trips"** collection
6. Click **"Export"**

This creates a backup of all your trip data.

### Using Firebase Admin SDK (for developers)

If you want to programmatically access the data:

```javascript
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();

// Get all trips
db.collection("trips")
  .get()
  .then((snapshot) => {
    snapshot.forEach((doc) => {
      console.log(doc.id, "=>", doc.data());
    });
  });
```

## Quick Access Links

Once you're logged into Firebase Console:

- **Firestore Database**: `https://console.firebase.google.com/project/bus-tracker-1c0bb/firestore`
- **Authentication Users**: `https://console.firebase.google.com/project/bus-tracker-1c0bb/authentication/users`
- **Project Settings**: `https://console.firebase.google.com/project/bus-tracker-1c0bb/settings/general`

## Troubleshooting

**Can't see any trips?**

- Make sure you've ended at least one trip in the app
- Check that the upload was successful (you should see a success toast message)
- Verify you're looking at the correct Firebase project

**Empty routePoints array?**

- This means no GPS points were recorded during the trip
- Make sure you simulated GPS movement in the emulator (see TESTING_GPS_ON_EMULATOR.md)

**Permission denied?**

- Check your Firestore security rules
- For testing, you can temporarily use test mode rules (not recommended for production)
