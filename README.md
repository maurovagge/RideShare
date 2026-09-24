# RideShare

RideShare is an Android application that helps people organize shared journeys. Users can publish and find rides, join available trips, communicate with other passengers, view routes on a map, and send an SOS alert when needed.

## Features

- User registration and login
- Profile management
- Ride creation with:
  - departure and arrival locations
  - date and time
  - available seats
- Ride discovery and filtering
- Joining and leaving rides
- Passenger management
- Realtime chat for each ride
- Google Maps route visualization
- Route and ride monitoring
- Push notifications
- SOS alerts sent to a configured emergency contact
- Background reminders and ride monitoring

## How to use the app

1. Sign in or create an account.
2. Complete your profile, including the phone number and SOS contact if you want to use the emergency feature.
3. Browse upcoming rides from the main list.
4. Open a ride to view its details, route, available seats, and passengers.
5. Join a ride or leave it when your plans change.
6. Create your own ride by entering the departure point, destination, date, time, and number of seats.
7. Use the ride chat to communicate with the driver and other passengers.
8. Open the map to view the route and ride status.
9. Use the SOS action during a ride to notify the configured emergency contact.

Some features require location, notification, and microphone permissions. Grant only the permissions required by your use of the app.

## Technology stack

### Android application

- Kotlin
- Android SDK
- AndroidX
- Material Components
- Fragments and Navigation Component
- View Binding and Data Binding
- WorkManager
- Android foreground services

### Backend and services

- Firebase Authentication
- Cloud Firestore
- Firebase Cloud Messaging

### Maps and location

- Google Maps SDK for Android
- Google Places SDK
- Google Directions API
- Google Maps Android Utils

## Project structure

```text
.
├── app/                         # Android application
├── gradle/                       # Gradle wrapper and version catalog
├── build.gradle.kts              # Root Gradle configuration
└── settings.gradle.kts           # Project settings
```

## Getting started

### Requirements

- Android Studio
- Android SDK 36
- JDK 11
- A Firebase project
- A Google Cloud project with Maps SDK, Places API, and Directions API enabled

The application requires Android API 30 or newer.

### Firebase setup

1. Create or select a Firebase project.
2. Register an Android app with the package name `mau.app.rideshare`.
3. Download `google-services.json` and place it in the `app/` directory.
4. Enable Firebase Authentication, Cloud Firestore, and Firebase Cloud Messaging.

### Google Maps setup

Configure the Maps API key using the Secrets Gradle Plugin and local properties:

```properties
MAPS_API_KEY=your_google_maps_api_key
```

Restrict the key to the required APIs and Android application before using the app outside development.

### Build the Android app

From the project root:

```powershell
.\gradlew.bat assembleDebug
```

To install the debug build on a connected device or emulator:

```powershell
.\gradlew.bat installDebug
```
