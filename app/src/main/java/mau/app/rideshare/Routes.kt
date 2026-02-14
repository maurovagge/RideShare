package mau.app.rideshare


// Irequest body
data class RouteRequest(
    val origin: Waypoint,
    val destination: Waypoint,
    val travelMode: String = "DRIVE",
    val routingPreference: String = "TRAFFIC_AWARE"
)

data class Waypoint(
    val location: LocationPoint
)

data class LocationPoint(
    val latLng: RideShareLocation
)

// answer from Google
data class RouteResponse(
    val routes: List<Route>?
)

data class Route(
    val duration: String?,       // Arriva come "1200s"
    val distanceMeters: Int?
)



