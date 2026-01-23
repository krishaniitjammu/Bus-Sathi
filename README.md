# Bus Tracker App

An Android application for bus drivers to track GPS routes during trips and upload them to Firebase.

## ⚠️ Important Security Notice

This repository does **NOT** include sensitive configuration files. Before building the app, you must:

1. Create `local.properties` with your Android SDK path (see `local.properties.template`)
2. Add your Firebase `google-services.json` file to `app/` directory (see `app/google-services.json.template`)
3. **Never commit these files to version control** - they are already in `.gitignore`

## Features

- **Driver Authentication**: Secure login using Firebase Authentication
- **GPS Tracking**: Continuous location tracking during trips with 5-second intervals
- **Foreground Service**: Reliable tracking even when app is in background
- **Trip Management**: Easy start/stop controls for trips
- **Real-time Statistics**: View points recorded and distance traveled during trip
- **Firebase Integration**: Automatic upload of route data to Cloud Firestore
- **Material Design UI**: Clean, modern interface

## Prerequisites

Before running this app, you need:

1. **Android Studio** (latest version recommended)
2. **Firebase Project** with:
   - Firebase Authentication enabled (Email/Password provider)
   - Cloud Firestore database created
3. **Android Device or Emulator** running Android 7.0 (API 24) or higher

## Firebase Setup

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project" and follow the setup wizard
3. Once created, click on "Android" icon to add an Android app

### 2. Register Your App

1. Enter package name: `com.bustracker`
2. Download the `google-services.json` file
3. Replace the placeholder `app/google-services.json` with your downloaded file

### 3. Enable Authentication

1. In Firebase Console, go to **Authentication** → **Sign-in method**
2. Enable **Email/Password** provider
3. Click **Save**

### 4. Create Test User

1. Go to **Authentication** → **Users**
2. Click **Add user**
3. Enter email and password (e.g., `driver@test.com` / `password123`)
4. Click **Add user**

### 5. Setup Firestore

1. In Firebase Console, go to **Firestore Database**
2. Click **Create database**
3. Choose **Start in test mode** (for development)
4. Select a location and click **Enable**

### 6. Firestore Security Rules (Optional - for production)

For production, update Firestore rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /trips/{tripId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/BusTrackerApp.git
cd BusTrackerApp
```

### 2. Configure Local Properties

Create a `local.properties` file in the project root (use `local.properties.template` as reference):

```properties
sdk.dir=YOUR_ANDROID_SDK_PATH
# Example Windows: sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
# Example macOS/Linux: sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
```

### 3. Setup Firebase Configuration

1. Download your `google-services.json` file from Firebase Console
2. Copy it to `app/google-services.json` (use `app/google-services.json.template` as reference)
3. **IMPORTANT**: Never commit the actual `google-services.json` to version control

### 4. Build and Run

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Connect an Android device or start an emulator
4. Click **Run** (or press Shift+F10)

## Usage

### Login

1. Launch the app
2. Enter the email and password of a user created in Firebase Authentication
3. Click **Login**

### Start a Trip

1. Grant location permissions when prompted
2. Ensure GPS is enabled on your device
3. Click **Start Trip**
4. A notification will appear showing the trip is being tracked
5. Move around to record GPS points (or use emulator location simulation)

### End a Trip

1. Click **End Trip**
2. The app will upload the route data to Firebase
3. You'll see a success message when upload completes

### View Trip Data

1. Go to Firebase Console → Firestore Database
2. Open the `trips` collection
3. You'll see documents with trip data including:
   - Driver information
   - Start/end timestamps
   - Array of GPS coordinates with timestamps
   - Total distance traveled

## Project Structure

```
app/src/main/java/com/bustracker/
├── data/
│   ├── model/
│   │   ├── LocationPoint.kt      # GPS coordinate data class
│   │   ├── Trip.kt                # Trip data class
│   │   └── TripStatus.kt          # Trip status enum
│   └── repository/
│       ├── AuthRepository.kt      # Firebase Auth operations
│       ├── LocationRepository.kt  # GPS tracking logic
│       └── TripRepository.kt      # Firestore operations
├── service/
│   └── LocationTrackingService.kt # Foreground service for GPS
├── ui/
│   ├── auth/
│   │   └── LoginActivity.kt       # Login screen
│   └── main/
│       └── MainActivity.kt        # Main trip management screen
└── util/
    ├── DistanceCalculator.kt      # Haversine distance calculation
    └── PermissionHelper.kt        # Runtime permission handling
```

## Permissions

The app requires the following permissions:

- `ACCESS_FINE_LOCATION` - For GPS tracking
- `ACCESS_COARSE_LOCATION` - Fallback location
- `FOREGROUND_SERVICE` - To track in background
- `FOREGROUND_SERVICE_LOCATION` - Location service type
- `POST_NOTIFICATIONS` - For Android 13+ notification permission
- `INTERNET` - For Firebase communication

## Technologies Used

- **Kotlin** - Programming language
- **MVVM Architecture** - Clean separation of concerns
- **Firebase Authentication** - User authentication
- **Cloud Firestore** - NoSQL database for trip storage
- **Google Play Services Location** - GPS tracking
- **Coroutines & Flow** - Asynchronous programming
- **Material Design Components** - Modern UI
- **ViewBinding** - Type-safe view access

## Troubleshooting

### Location not updating

- Ensure GPS is enabled on device
- Check that location permissions are granted
- For emulator, use Extended Controls → Location to simulate movement

### Firebase upload fails

- Verify internet connection
- Check that `google-services.json` is correctly configured
- Ensure Firestore database is created and accessible

### App crashes on start

- Verify `google-services.json` is present and valid
- Check that all dependencies are properly synced
- Ensure minimum SDK version is met (API 24+)

## Future Enhancements

- Add bus number and route name fields
- Display route on a map
- Trip history view
- Offline support with sync when online
- Export trip data as GPX/KML files
- Driver statistics and analytics

## License

This project is provided as-is for educational and development purposes.
