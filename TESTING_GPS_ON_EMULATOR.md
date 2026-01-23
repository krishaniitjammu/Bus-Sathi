# Testing GPS Tracking on Android Emulator

## The Issue

Android emulators don't automatically generate GPS location updates. When you start a trip, the app is working correctly, but the emulator is staying at a fixed location, so no new location points are being recorded.

## Solution: Simulate GPS Movement

### Method 1: Using Extended Controls (Recommended)

1. **Open Extended Controls**
   - While the emulator is running, click the **"..."** (More) button on the emulator toolbar
   - Or press `Ctrl + Shift + P` (Windows) / `Cmd + Shift + P` (Mac)

2. **Navigate to Location**
   - In the Extended Controls window, select **"Location"** from the left sidebar

3. **Simulate Movement**
   
   **Option A: Single Point Updates**
   - Enter a latitude and longitude manually
   - Click **"Send"** to update the location
   - Wait a few seconds, then change the coordinates slightly
   - Click **"Send"** again
   - Repeat this process to simulate movement
   
   **Option B: Route Playback (Best for Testing)**
   - Click the **"Routes"** tab
   - Click **"Load GPX/KML"** to load a pre-recorded route file
   - Or manually create a route by clicking points on the map
   - Click **"Play Route"** to simulate movement along the route
   - The emulator will automatically update GPS coordinates as it follows the route

4. **Verify in Your App**
   - Start a trip in your Bus Tracker app
   - As you send location updates or play a route, you should see:
     - Point count increasing in the UI
     - Distance increasing
     - Notification updating with new values

### Method 2: Using ADB Commands

You can also send GPS coordinates via ADB commands:

```bash
# Send a single GPS coordinate (latitude, longitude)
adb emu geo fix -122.084 37.422

# Example: Simulate movement with multiple commands
adb emu geo fix -122.084 37.422
# Wait 5 seconds
adb emu geo fix -122.085 37.423
# Wait 5 seconds
adb emu geo fix -122.086 37.424
```

### Method 3: Create a GPX Route File

Create a file named `test_route.gpx`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="Manual">
  <trk>
    <name>Test Bus Route</name>
    <trkseg>
      <trkpt lat="37.422" lon="-122.084">
        <ele>0</ele>
      </trkpt>
      <trkpt lat="37.423" lon="-122.085">
        <ele>0</ele>
      </trkpt>
      <trkpt lat="37.424" lon="-122.086">
        <ele>0</ele>
      </trkpt>
      <trkpt lat="37.425" lon="-122.087">
        <ele>0</ele>
      </trkpt>
      <trkpt lat="37.426" lon="-122.088">
        <ele>0</ele>
      </trkpt>
    </trkseg>
  </trk>
</gpx>
```

Then load this file in the emulator's Extended Controls → Location → Routes tab.

## Testing Steps

1. **Start the app** on the emulator
2. **Login** with your test credentials
3. **Open Extended Controls** (...  button on emulator)
4. **Go to Location tab**
5. **Start Trip** in your app
6. **Send location updates** or **play a route** in Extended Controls
7. **Watch the UI update** with point count and distance
8. **End Trip** and verify data is uploaded to Firebase

## Expected Behavior

- **Points Recorded**: Should increase as you send location updates (every 3-5 seconds based on your update interval)
- **Distance**: Should increase as the GPS coordinates change
- **Notification**: Should update with current stats
- **Firebase**: After ending trip, all points should be uploaded

## Troubleshooting

### Still No Points Being Recorded?

1. **Check Logcat** for errors:
   - In Android Studio, open the **Logcat** tab
   - Filter by your package: `com.bustracker`
   - Look for any errors related to location permissions or GPS

2. **Verify Permissions**:
   - Make sure you granted location permissions when the app started
   - Check Settings → Apps → Bus Tracker → Permissions → Location is set to "Allow all the time" or "Allow only while using the app"

3. **Check GPS is Enabled**:
   - Swipe down on the emulator
   - Make sure Location/GPS is turned ON

4. **Restart the Service**:
   - End the trip
   - Force stop the app
   - Restart and try again

### Testing on a Real Device

For the most accurate testing, use a real Android device:

1. Enable **Developer Options** on your device
2. Enable **USB Debugging**
3. Connect via USB to your computer
4. Run the app from Android Studio on the physical device
5. Actually walk around or drive to test real GPS tracking

Real devices will automatically generate GPS updates as you move, making testing much more realistic.

## Quick Test Script

Here's a quick way to test with ADB (run in terminal while trip is active):

```bash
# Send 10 location updates with slight changes
for i in {0..9}; do
  lat=$(echo "37.422 + $i * 0.001" | bc)
  lon=$(echo "-122.084 + $i * 0.001" | bc)
  adb emu geo fix $lon $lat
  echo "Sent location: $lat, $lon"
  sleep 5
done
```

This will simulate movement over about 1 km in 50 seconds.
