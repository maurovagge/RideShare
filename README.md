# RideShare

RideShare is an Android application for coordinating shared journeys. Users can publish and discover rides, join available trips, communicate with other passengers, follow routes on a map, and send an SOS alert to a configured emergency contact.

The project is composed of:

- an Android client written in Kotlin;
- Firebase Authentication and Cloud Firestore for identity and realtime data;
- Google Maps, Places, and Directions APIs for location search, route calculation, and map rendering;
- Firebase Cloud Messaging for notifications;
- a Firebase Cloud Function that forwards active SOS alerts to the destination user's device.

## Features

- Sign in and user profile management through Firebase Authentication.
- Create rides with origin, destination, date, departure time, and available seats.
- Select locations using Google Places Autocomplete.
- Browse upcoming rides or filter the list to rides created by or joined by the current user.
- Join and leave rides, manage passengers, and remove rides created by the current user.
- Realtime ride-specific chat using Firestore subcollections.
- View ride routes, stops, and live location information on Google Maps.
- Receive push notifications through Firebase Cloud Messaging.
- Trigger and monitor an SOS flow with foreground location services and emergency-contact notifications.
- Background reminders and ride monitoring using Android WorkManager and foreground services.

## Technology stack

| Area | Technologies |
| --- | --- |
| Mobile app | Kotlin, Android SDK, AndroidX, Material Components |
| UI and navigation | View Binding, Data Binding, Fragments, Navigation Component |
| Backend | Firebase Authentication, Cloud Firestore, Firebase Cloud Messaging |
| Maps and routing | Google Maps SDK, Places SDK, Directions API |
| Notifications and background work | FCM, WorkManager, Android foreground services |
| Serverless backend | Firebase Cloud Functions, Node.js 24 |
| Build tooling | Gradle, Android Gradle Plugin, Kotlin |

## Repository structure

```text
.
├── app/                         # Android application module
├── CloudFunctions/
│   ├── firebase.json             # Firebase deployment configuration
│   └── functions/                # Cloud Functions source and npm project
├── gradle/                       # Gradle version catalog and wrapper
├── build.gradle.kts              # Root Gradle configuration
└── settings.gradle.kts           # Gradle project settings
```

## Prerequisites

Install the following before building the project:

- Android Studio with Android SDK 36 and an emulator or Android device;
- JDK 11;
- Node.js 24 for Firebase Functions;
- a Firebase project configured for Android;
- a Google Cloud project with Maps SDK for Android, Places API, and Directions API enabled;
- Firebase CLI, if you need to run or deploy the Cloud Functions.

The Android module currently targets Android API 36 and supports Android API 30 and newer.

## Configuration

### Firebase Android configuration

1. Create or select a Firebase project.
2. Register the Android application with package name `mau.app.rideshare`.
3. Download the generated `google-services.json` into `app/`.
4. Enable the Firebase services used by the app, including Authentication, Cloud Firestore, and Cloud Messaging.
5. Configure Firestore security rules and indexes for your environment.

Do not commit credentials, private keys, or environment-specific configuration that is not intended to be public. If you replace the Firebase project, use the appropriate project-specific configuration file locally.

### Google Maps and Places

The project uses the Secrets Gradle Plugin. Provide the Maps key through the local properties mechanism expected by that plugin, for example:

```properties
MAPS_API_KEY=your_google_maps_api_key
```

Keep API keys restricted by application, package name, SHA-1 certificate, API, and environment wherever possible.

### Firebase Functions

The Functions project uses the Firebase project selected in `CloudFunctions/.firebaserc`. Review that file and update the project alias for your environment before deploying.

## Build and run the Android app

From the repository root:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

To install the debug build on a connected device or emulator:

```bash
./gradlew installDebug
```

Open the root directory in Android Studio to run the application, execute unit tests, or use the Android emulator.

## Test the Android app

Run JVM unit tests with:

```bash
./gradlew test
```

Run instrumented tests on a connected device or emulator with:

```bash
./gradlew connectedAndroidTest
```

## Develop and deploy Cloud Functions

Install the Functions dependencies:

```bash
cd CloudFunctions/functions
npm ci
```

Run linting:

```bash
npm run lint
```

Start the local Functions emulator:

```bash
npm run serve
```

Deploy only the Functions configured by this repository:

```bash
npm run deploy
```

The `checkNewSOS` function listens for new documents in the `SOS` Firestore collection. It validates the alert state and timestamp, retrieves the destination user's FCM token from `UsersTre`, and sends a data notification when the alert is active and recent.

## Data model overview

The Android client uses the following Firestore areas:

- `UsersTre`: user profiles and FCM tokens;
- `RideDataTRE`: rides, passenger references, route information, and ride metadata;
- `RideDataTRE/{rideId}/messages`: messages for an individual ride;
- `SOS`: SOS requests and their lifecycle state.

Keep the collection names and field names aligned between the Android client, Firestore rules, indexes, and Cloud Functions.

## Security and privacy

RideShare handles account information, contact details, location data, messages, and emergency alerts. Before using it outside a development environment:

- review and harden Firestore security rules;
- restrict Google API keys and Firebase project access;
- verify foreground-location and notification permission flows on supported Android versions;
- define retention and deletion policies for rides, messages, location data, and SOS records;
- test the SOS workflow end to end with a controlled emergency contact.
