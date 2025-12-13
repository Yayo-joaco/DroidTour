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

    public TourLocation(double lat, double lng, String name, int order) {
        this.lat = lat;
        this.lng = lng;
        this.name = name;
        this.order = order;
    }

    protected TourLocation(Parcel in) {
        lat = in.readDouble();
        lng = in.readDouble();
        name = in.readString();
        order = in.readInt();
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
    }
}
