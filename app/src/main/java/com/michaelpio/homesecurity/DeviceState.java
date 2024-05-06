package com.michaelpio.homesecurity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class DeviceState {
    private MutableLiveData<Integer> temperature = new MutableLiveData<>();
    private MutableLiveData<Integer> humidity = new MutableLiveData<>();
    private MutableLiveData<Boolean> isArmed = new MutableLiveData<>();
    private MutableLiveData<Boolean> isIntruderDetected = new MutableLiveData<>();
    private MutableLiveData<Boolean> isDeviceConnected = new MutableLiveData<>();
    private MutableLiveData<Boolean> isMobileConnected = new MutableLiveData<>();
    public DeviceState() {
        temperature.setValue(36);
        humidity.setValue(70);
        isArmed.setValue(true);
        isIntruderDetected.setValue(false);
        isDeviceConnected.setValue(false);
        isMobileConnected.setValue(false);
    }

    public LiveData<Integer> getTemperature() {
        return temperature;
    }

    public LiveData<Integer> getHumidity() {
        return humidity;
    }

    public LiveData<Boolean> getIsArmed() {
        return isArmed;
    }

    public LiveData<Boolean> getIsDeviceConnected() {
        return isDeviceConnected;
    }

    public LiveData<Boolean> getIsMobileConnected() {
        return isMobileConnected;
    }
    public LiveData<Boolean> getIsIntruderDetected(){
        return isIntruderDetected;
    }
    public void setTemperature(int temperature) {
        this.temperature.setValue(temperature);
    }

    public void setHumidity(int humidity) {
        this.humidity.setValue(humidity);
    }

    public void setIsArmed(boolean isArmed) {
        this.isArmed.setValue(isArmed);
    }

    public void setIsDeviceConnected(boolean isDeviceConnected) {
        this.isDeviceConnected.setValue(isDeviceConnected);
    }

    public void setIsMobileConnected(boolean isMobileConnected) {
        this.isMobileConnected.setValue(isMobileConnected);
    }

    public void setIsIntruderDetected(Boolean isIntruderDetected) {
        this.isIntruderDetected.setValue(isIntruderDetected);
    }
}
