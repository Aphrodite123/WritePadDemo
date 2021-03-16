package com.aphrodite.writepaddemo.model.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Aphrodite on 2021/2/1.
 */
public class PointBean implements Parcelable {
    public int state;
    public float x;
    public float y;
    public float pressure;

    public PointBean() {
    }

    protected PointBean(Parcel in) {
        state = in.readInt();
        x = in.readFloat();
        y = in.readFloat();
        pressure = in.readFloat();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(state);
        dest.writeFloat(x);
        dest.writeFloat(y);
        dest.writeFloat(pressure);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PointBean> CREATOR = new Creator<PointBean>() {
        @Override
        public PointBean createFromParcel(Parcel in) {
            return new PointBean(in);
        }

        @Override
        public PointBean[] newArray(int size) {
            return new PointBean[size];
        }
    };

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }
}
