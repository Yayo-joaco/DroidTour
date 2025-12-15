package com.example.droidtour.admin;

import android.os.Parcel;
import android.os.Parcelable;


// esto es para el mapa de ubicacion que se creara el tour tener en cuenta ojo ojito
//esto es el modelo de datoa pero ya no lo movi de carpeta ara que se entienda rapidamente
//creado por dan xd, es para la creacion de touuuuuuurs
public class TourLocation implements Parcelable {

    public double lat;
    public double lng;
    public String name;
    public int order;
    public String time;        // Hora de llegada (ej: "09:00 AM")
    public String description; // Descripción de la parada
    public int stopDuration;   // Duración en minutos de esta parada

    public TourLocation(double lat, double lng, String name, int order) {
        this.lat = lat;
        this.lng = lng;
        this.name = name;
        this.order = order;
        this.time = "";
        this.description = "";
        this.stopDuration = 0;
    }

    public TourLocation(double lat, double lng, String name, int order, String time, String description) {
        this.lat = lat;
        this.lng = lng;
        this.name = name;
        this.order = order;
        this.time = time;
        this.description = description;
        this.stopDuration = 0;
    }
    
    public TourLocation(double lat, double lng, String name, int order, String time, String description, int stopDuration) {
        this.lat = lat;
        this.lng = lng;
        this.name = name;
        this.order = order;
        this.time = time;
        this.description = description;
        this.stopDuration = stopDuration;
    }

    protected TourLocation(Parcel in) {
        lat = in.readDouble();
        lng = in.readDouble();
        name = in.readString();
        order = in.readInt();
        time = in.readString();
        description = in.readString();
        stopDuration = in.readInt();
    }

    public static final Creator<TourLocation> CREATOR = new Creator<TourLocation>() {
        @Override
        public TourLocation createFromParcel(Parcel in) {
            return new TourLocation(in);
        }

        @Override
        public TourLocation[] newArray(int size) {
            return new TourLocation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(lat);
        parcel.writeDouble(lng);
        parcel.writeString(name);
        parcel.writeInt(order);
        parcel.writeString(time);
        parcel.writeString(description);
        parcel.writeInt(stopDuration);
    }

    // Getters y Setters
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getStopDuration() { return stopDuration; }
    public void setStopDuration(int stopDuration) { this.stopDuration = stopDuration; }
}
