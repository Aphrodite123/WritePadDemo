package com.aphrodite.writepaddemo.model.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by Aphrodite on 2021/2/1.
 */
public class PointsBean implements Parcelable {
    private List<PointBean> data;

    public PointsBean() {
    }

    protected PointsBean(Parcel in) {
        data = in.createTypedArrayList(PointBean.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(data);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PointsBean> CREATOR = new Creator<PointsBean>() {
        @Override
        public PointsBean createFromParcel(Parcel in) {
            return new PointsBean(in);
        }

        @Override
        public PointsBean[] newArray(int size) {
            return new PointsBean[size];
        }
    };

    public List<PointBean> getData() {
        return data;
    }

    public void setData(List<PointBean> data) {
        this.data = data;
    }
}
