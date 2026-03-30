# Setup Guide for Bus Tracker App

This guide will help you set up the Bus Tracker App for development.

## Prerequisites

- Android Studio (latest version recommended)
- Java Development Kit (JDK) 11 or higher
- Android SDK (API 24 or higher)
- A Firebase account

## Step-by-Step Setup

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/BusTrackerApp.git
cd BusTrackerApp
```

### 2. Configure Android SDK Path

Create a `local.properties` file in the project root directory:

**Windows:**

```properties
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
```

**macOS:**

```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
```

**Linux:**

```properties
sdk.dir=/home/YOUR_USERNAME/Android/Sdk
```

> **Note:** A template file `local.properties.template` is provided for reference.

### 3. Setup Firebase Project

#### 3.1 Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project"
3. Enter a project name (e.g., "Bus Tracker")
4. Follow the setup wizard (you can disable Google Analytics if not needed)

#### 3.2 Add Android App to Firebase

1. In your Firebase project, click the Android icon
2. Register your app with package name: `com.karroh.bussathi`
3. Download the `google-services.json` file
4. Place it in the `app/` directory (replacing the `.template` file)

#### 3.3 Enable Firebase Authentication

1. In Firebase Console, navigate to **Authentication** → **Sign-in method**
2. Click on **Email/Password**
3. Enable the provider
4. Click **Save**

#### 3.4 Create Test Users

1. Go to **Authentication** → **Users**
2. Click **Add user**
3. Create test accounts:
   - Email: `driver@test.com`
   - Password: `password123` (or your choice)

#### 3.5 Setup Cloud Firestore

1. In Firebase Console, go to **Firestore Database**
2. Click **Create database**
3. Choose **Start in test mode** for development
4. Select your preferred location
5. Click **Enable**

#### 3.6 Configure Firestore Security Rules (Production)

For production deployment, update your Firestore rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow authenticated users to read/write their own trips
    match /trips/{tripId} {
      allow read, write: if request.auth != null;
    }

    // Allow authenticated users to read/write their driver profile
    match /drivers/{driverId} {
      allow read, write: if request.auth != null && request.auth.uid == driverId;
    }
  }
}
```

### 4. Build the Project

1. Open Android Studio
2. Select **File** → **Open** and choose the project directory
3. Wait for Gradle sync to complete (this may take a few minutes on first run)
4. If you see any SDK or dependency errors, follow Android Studio's prompts to install them

### 5. Run the App

#### On Physical Device:

1. Enable **Developer Options** on your Android device:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
2. Enable **USB Debugging** in Developer Options
3. Connect your device via USB
4. Click **Run** in Android Studio (or press `Shift+F10`)

#### On Emulator:

1. Click **Device Manager** in Android Studio
2. Create a new virtual device (recommended: Pixel 5 with API 24+)
3. Start the emulator
4. Click **Run** in Android Studio

## Verification

To verify everything is set up correctly:

1. Launch the app
2. You should see the login screen
3. Enter the test credentials you created in Firebase
4. Grant location permissions when prompted
5. Try starting and stopping a trip
6. Check Firebase Console → Firestore to see the trip data

## Troubleshooting

### Build Errors

**Problem:** "SDK location not found"

- **Solution:** Ensure `local.properties` exists with the correct `sdk.dir` path

**Problem:** "google-services.json is missing"

- **Solution:** Download the file from Firebase Console and place it in `app/` directory

**Problem:** Gradle sync fails

- **Solution:** Try **File** → **Invalidate Caches and Restart**

### Runtime Errors

**Problem:** Authentication fails

- **Solution:** Verify Firebase Authentication is enabled and test user exists

**Problem:** Location not updating

- **Solution:** Ensure location permissions are granted and GPS is enabled

**Problem:** Trip data not uploading

- **Solution:** Check Firestore rules and ensure user is authenticated

## Security Best Practices

1. **Never commit** `google-services.json` to version control
2. **Never commit** `local.properties` to version control
3. Keep Firebase API keys secure
4. Use Firestore security rules in production
5. Regularly rotate API keys and credentials
6. Review Firebase Console security settings

## Additional Resources

- [Firebase Documentation](https://firebase.google.com/docs)
- [Android Developer Guide](https://developer.android.com/guide)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)

## Support

If you encounter issues:

1. Check the [Issues](https://github.com/YOUR_USERNAME/BusTrackerApp/issues) page
2. Review Firebase Console logs
3. Check Android Studio's Logcat for error messages
