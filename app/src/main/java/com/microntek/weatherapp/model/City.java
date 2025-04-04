package com.microntek.weatherapp.model;

/**
 * 城市数据模型类
 */
public class City {
    private String name;         // 城市名称
    private String id;           // 城市ID
    private String province;     // 所在省份
    private String district;     // 所在区/县
    private double latitude;     // 纬度
    private double longitude;    // 经度
    private boolean isCurrentLocation; // 是否为当前位置
    
    // 临时天气信息，用于城市列表显示
    private int temperature;     // 当前温度
    private String weatherDesc;  // 天气描述
    private String weatherIcon;  // 天气图标

    // 构造函数
    public City() {
    }

    public City(String name, String id, String province, double latitude, double longitude) {
        this.name = name;
        this.id = id;
        this.province = province;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isCurrentLocation = false;
    }

    // Getter和Setter方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isCurrentLocation() {
        return isCurrentLocation;
    }

    public void setCurrentLocation(boolean currentLocation) {
        isCurrentLocation = currentLocation;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public String getWeatherDesc() {
        return weatherDesc;
    }

    public void setWeatherDesc(String weatherDesc) {
        this.weatherDesc = weatherDesc;
    }

    public String getWeatherIcon() {
        return weatherIcon;
    }

    public void setWeatherIcon(String weatherIcon) {
        this.weatherIcon = weatherIcon;
    }
    
    // 显示在UI中的名称
    public String getDisplayName() {
        if (province != null && !province.isEmpty()) {
            if (district != null && !district.isEmpty() && !district.equals(name)) {
                return name + ", " + district + ", " + province;
            }
            return name + ", " + province;
        }
        return name;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return id.equals(city.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
} 