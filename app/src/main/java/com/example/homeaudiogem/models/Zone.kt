package com.example.homeaudiogem.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Zone(
    @SerializedName("zone")
    val zone: String,

    @SerializedName("name")
    var name: String? = null,

    @SerializedName("pa")
    var pa: String = "00", // PA attribute

    @SerializedName("pr")
    var pr: String = "00", // Power

    @SerializedName("mu")
    var mu: String = "00", // Mute

    @SerializedName("dt")
    var dt: String = "00", // DT attribute

    @SerializedName("vo")
    var vo: String = "15", // Volume

    @SerializedName("tr")
    var tr: String = "07", // Treble

    @SerializedName("bs")
    var bs: String = "07", // Bass

    @SerializedName("bl")
    var bl: String = "10", // Balance

    @SerializedName("ch")
    var ch: String = "01", // Channel/Source

    @SerializedName("ls")
    var ls: String = "00", // Keypad

    var order: Int = 0 // For sorting
) : Parcelable {
    // Helper functions
    fun isOn(): Boolean = pr == "01"

    fun isMuted(): Boolean = mu == "01"

    // Parcelable implementation
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString() ?: "00",
        parcel.readString() ?: "00",
        parcel.readString() ?: "00",
        parcel.readString() ?: "00",
        parcel.readString() ?: "15",
        parcel.readString() ?: "07",
        parcel.readString() ?: "07",
        parcel.readString() ?: "10",
        parcel.readString() ?: "01",
        parcel.readString() ?: "00",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(zone)
        parcel.writeString(name)
        parcel.writeString(pa)
        parcel.writeString(pr)
        parcel.writeString(mu)
        parcel.writeString(dt)
        parcel.writeString(vo)
        parcel.writeString(tr)
        parcel.writeString(bs)
        parcel.writeString(bl)
        parcel.writeString(ch)
        parcel.writeString(ls)
        parcel.writeInt(order)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<Zone> {
            override fun createFromParcel(parcel: Parcel): Zone {
                return Zone(parcel)
            }

            override fun newArray(size: Int): Array<Zone?> {
                return arrayOfNulls(size)
            }
        }

        fun createMasterZone(): Zone {
            return Zone(
                zone = "all",
                pa = "00",
                pr = "00",
                mu = "00",
                dt = "00",
                vo = "15",
                tr = "07",
                bs = "07",
                bl = "10",
                ch = "01",
                ls = "00"
            )
        }
    }
}