package com.microntek.weatherapp.model;

import java.util.List;

/**
 * 天气数据模型类
 */
public class Weather {
    // 城市信息
    private String cityName;
    
    // 温度信息
    private int currentTemp;
    private int highTemp;
    private int lowTemp;
    private int feelsLike; // 体感温度
    
    // 天气描述
    private String weatherDesc;
    private String weatherIcon;
    
    // 其他天气信息
    private String wind;
    private int humidity;
    private String sunrise;
    private String sunset;
    
    // 空气质量
    private String airQuality;
    private int aqi;
    private int pm25;
    private int pm10;
    private double co;  // 一氧化碳
    private int so2;    // 二氧化硫
    private int no2;    // 二氧化氮
    private int o3;     // 臭氧
    
    // 生活指数
    private String clothesIndex;    // 穿衣指数
    private String sportIndex;      // 运动指数
    private String uvIndex;         // 紫外线指数
    private String washCarIndex;    // 洗车指数
    private String travelIndex;     // 旅游指数
    private String comfortIndex;    // 舒适度指数
    private String airPollutionIndex; // 空气污染扩散条件
    private String trafficIndex;    // 交通指数
    
    // 天气预报
    private List<DailyForecast> dailyForecasts;
    
    // Getter和Setter方法
    public String getCityName() {
        return cityName;
    }
    
    public void setCityName(String cityName) {
        this.cityName = cityName;
    }
    
    public int getCurrentTemp() {
        return currentTemp;
    }
    
    public void setCurrentTemp(int currentTemp) {
        this.currentTemp = currentTemp;
    }
    
    public int getHighTemp() {
        return highTemp;
    }
    
    public void setHighTemp(int highTemp) {
        this.highTemp = highTemp;
    }
    
    public int getLowTemp() {
        return lowTemp;
    }
    
    public void setLowTemp(int lowTemp) {
        this.lowTemp = lowTemp;
    }
    
    public int getFeelsLike() {
        return feelsLike;
    }
    
    public void setFeelsLike(int feelsLike) {
        this.feelsLike = feelsLike;
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
    
    public String getWind() {
        return wind;
    }
    
    public void setWind(String wind) {
        this.wind = wind;
    }
    
    public int getHumidity() {
        return humidity;
    }
    
    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }
    
    public String getSunrise() {
        return sunrise;
    }
    
    public void setSunrise(String sunrise) {
        this.sunrise = sunrise;
    }
    
    public String getSunset() {
        return sunset;
    }
    
    public void setSunset(String sunset) {
        this.sunset = sunset;
    }
    
    public String getAirQuality() {
        return airQuality;
    }
    
    public void setAirQuality(String airQuality) {
        this.airQuality = airQuality;
    }
    
    public int getAqi() {
        return aqi;
    }
    
    public void setAqi(int aqi) {
        this.aqi = aqi;
    }
    
    public int getPm25() {
        return pm25;
    }
    
    public void setPm25(int pm25) {
        this.pm25 = pm25;
    }
    
    public int getPm10() {
        return pm10;
    }
    
    public void setPm10(int pm10) {
        this.pm10 = pm10;
    }
    
    public double getCo() {
        return co;
    }
    
    public void setCo(double co) {
        this.co = co;
    }
    
    public int getSo2() {
        return so2;
    }
    
    public void setSo2(int so2) {
        this.so2 = so2;
    }
    
    public int getNo2() {
        return no2;
    }
    
    public void setNo2(int no2) {
        this.no2 = no2;
    }
    
    public int getO3() {
        return o3;
    }
    
    public void setO3(int o3) {
        this.o3 = o3;
    }
    
    public String getClothesIndex() {
        return clothesIndex;
    }
    
    public void setClothesIndex(String clothesIndex) {
        this.clothesIndex = clothesIndex;
    }
    
    public String getSportIndex() {
        return sportIndex;
    }
    
    public void setSportIndex(String sportIndex) {
        this.sportIndex = sportIndex;
    }
    
    public String getUvIndex() {
        return uvIndex;
    }
    
    public void setUvIndex(String uvIndex) {
        this.uvIndex = uvIndex;
    }
    
    public String getWashCarIndex() {
        return washCarIndex;
    }
    
    public void setWashCarIndex(String washCarIndex) {
        this.washCarIndex = washCarIndex;
    }
    
    public String getTravelIndex() {
        return travelIndex;
    }
    
    public void setTravelIndex(String travelIndex) {
        this.travelIndex = travelIndex;
    }
    
    public String getComfortIndex() {
        return comfortIndex;
    }
    
    public void setComfortIndex(String comfortIndex) {
        this.comfortIndex = comfortIndex;
    }
    
    public String getAirPollutionIndex() {
        return airPollutionIndex;
    }
    
    public void setAirPollutionIndex(String airPollutionIndex) {
        this.airPollutionIndex = airPollutionIndex;
    }
    
    public String getTrafficIndex() {
        return trafficIndex;
    }
    
    public void setTrafficIndex(String trafficIndex) {
        this.trafficIndex = trafficIndex;
    }
    
    public List<DailyForecast> getDailyForecasts() {
        return dailyForecasts;
    }
    
    public void setDailyForecasts(List<DailyForecast> dailyForecasts) {
        this.dailyForecasts = dailyForecasts;
    }
    
    /**
     * 天气预报数据模型
     */
    public static class DailyForecast {
        private String date;
        private String dayOfWeek;
        private int highTemp;
        private int lowTemp;
        private String weatherDesc;
        private String weatherIcon;
        
        public String getDate() {
            return date;
        }
        
        public void setDate(String date) {
            this.date = date;
        }
        
        public String getDayOfWeek() {
            return dayOfWeek;
        }
        
        public void setDayOfWeek(String dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }
        
        public int getHighTemp() {
            return highTemp;
        }
        
        public void setHighTemp(int highTemp) {
            this.highTemp = highTemp;
        }
        
        public int getLowTemp() {
            return lowTemp;
        }
        
        public void setLowTemp(int lowTemp) {
            this.lowTemp = lowTemp;
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
    }
}