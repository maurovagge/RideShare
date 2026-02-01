package mau.app.rideshare

class RideShareLocation {
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    constructor()
    {
        this.latitude = 0.0
        this.longitude = 0.0
    }

    constructor(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
    }
}

fun RideShareLocation.toMapLatLng() = com.google.android.gms.maps.model.LatLng(latitude, longitude)

fun com.google.android.gms.maps.model.LatLng.toRideShareLocation() = RideShareLocation(latitude, longitude)