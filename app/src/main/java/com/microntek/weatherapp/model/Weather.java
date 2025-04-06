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
    private int weatherIconResource; // 新增图标资源ID属性
    
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
    private String clothesCategory; // 穿衣指数简洁描述
    private String sportIndex;      // 运动指数
    private String sportCategory;   // 运动指数简洁描述
    private String uvIndex;         // 紫外线指数
    private String uvCategory;      // 紫外线指数简洁描述
    private String washCarIndex;    // 洗车指数
    private String washCarCategory; // 洗车指数简洁描述
    private String travelIndex;     // 旅游指数
    private String travelCategory;  // 旅游指数简洁描述
    private String comfortIndex;    // 舒适度指数
    private String comfortCategory; // 舒适度指数简洁描述
    private String airPollutionIndex; // 空气污染扩散条件
    private String airPollutionCategory; // 空气污染扩散条件简洁描述
    private String trafficIndex;    // 交通指数
    private String trafficCategory; // 交通指数简洁描述
    private String fluIndex;        // 感冒指数
    private String fluCategory;     // 感冒指数简洁描述
    
    // 天气预报
    private List<DailyForecast> dailyForecasts;
    
    // 新增：时间戳字段
    private long updateTimestamp;
    
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
    
    /**
     * 获取体感温度（别名方法，用于与外部系统兼容）
     */
    public int getFeelsLikeTemp() {
        return feelsLike;
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
    
    /**
     * 获取天气代码（从图标获取对应代码）
     */
    public String getWeatherCode() {
        if (weatherIcon == null) {
            return "";
        }
        // 从weatherIcon获取代码，通常已经包含代码信息
        return weatherIcon;
    }
    
    public int getWeatherIconResource() {
        return weatherIconResource;
    }
    
    public void setWeatherIconResource(int weatherIconResource) {
        this.weatherIconResource = weatherIconResource;
    }
    
    public String getWind() {
        return wind;
    }
    
    public void setWind(String wind) {
        this.wind = wind;
    }
    
    /**
     * 获取风向
     */
    public String getWindDirection() {
        if (wind == null || wind.isEmpty()) {
            return "";
        }
        // 尝试从完整的风信息中提取风向部分
        // 通常格式为"东北风 3级"，提取"东北风"部分
        String[] parts = wind.split(" ");
        if (parts.length > 0) {
            return parts[0]; // 返回风向部分
        }
        return wind; // 如果无法解析，返回完整字符串
    }
    
    /**
     * 获取风速
     */
    public String getWindSpeed() {
        if (wind == null || wind.isEmpty()) {
            return "";
        }
        // 尝试从完整的风信息中提取风速部分
        // 通常格式为"东北风 3级"，提取"3级"部分
        String[] parts = wind.split(" ");
        if (parts.length > 1) {
            return parts[1]; // 返回风速部分
        }
        return ""; // 如果无法解析，返回空字符串
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
    
    public String getClothesCategory() {
        return clothesCategory;
    }
    
    public void setClothesCategory(String clothesCategory) {
        this.clothesCategory = clothesCategory;
    }
    
    public String getSportIndex() {
        return sportIndex;
    }
    
    public void setSportIndex(String sportIndex) {
        this.sportIndex = sportIndex;
    }
    
    public String getSportCategory() {
        return sportCategory;
    }
    
    public void setSportCategory(String sportCategory) {
        this.sportCategory = sportCategory;
    }
    
    public String getUvIndex() {
        return uvIndex;
    }
    
    public void setUvIndex(String uvIndex) {
        this.uvIndex = uvIndex;
    }
    
    public String getUvCategory() {
        return uvCategory;
    }
    
    public void setUvCategory(String uvCategory) {
        this.uvCategory = uvCategory;
    }
    
    public String getWashCarIndex() {
        return washCarIndex;
    }
    
    public void setWashCarIndex(String washCarIndex) {
        this.washCarIndex = washCarIndex;
    }
    
    public String getWashCarCategory() {
        return washCarCategory;
    }
    
    public void setWashCarCategory(String washCarCategory) {
        this.washCarCategory = washCarCategory;
    }
    
    public String getTravelIndex() {
        return travelIndex;
    }
    
    public void setTravelIndex(String travelIndex) {
        this.travelIndex = travelIndex;
    }
    
    public String getTravelCategory() {
        return travelCategory;
    }
    
    public void setTravelCategory(String travelCategory) {
        this.travelCategory = travelCategory;
    }
    
    public String getComfortIndex() {
        return comfortIndex;
    }
    
    public void setComfortIndex(String comfortIndex) {
        this.comfortIndex = comfortIndex;
    }
    
    public String getComfortCategory() {
        return comfortCategory;
    }
    
    public void setComfortCategory(String comfortCategory) {
        this.comfortCategory = comfortCategory;
    }
    
    public String getAirPollutionIndex() {
        return airPollutionIndex;
    }
    
    public void setAirPollutionIndex(String airPollutionIndex) {
        this.airPollutionIndex = airPollutionIndex;
    }
    
    public String getAirPollutionCategory() {
        return airPollutionCategory;
    }
    
    public void setAirPollutionCategory(String airPollutionCategory) {
        this.airPollutionCategory = airPollutionCategory;
    }
    
    public String getTrafficIndex() {
        return trafficIndex;
    }
    
    public void setTrafficIndex(String trafficIndex) {
        this.trafficIndex = trafficIndex;
    }
    
    public String getTrafficCategory() {
        return trafficCategory;
    }
    
    public void setTrafficCategory(String trafficCategory) {
        this.trafficCategory = trafficCategory;
    }
    
    public String getFluIndex() {
        return fluIndex;
    }
    
    public void setFluIndex(String fluIndex) {
        this.fluIndex = fluIndex;
    }
    
    public String getFluCategory() {
        return fluCategory;
    }
    
    public void setFluCategory(String fluCategory) {
        this.fluCategory = fluCategory;
    }
    
    public List<DailyForecast> getDailyForecasts() {
        return dailyForecasts;
    }
    
    public void setDailyForecasts(List<DailyForecast> dailyForecasts) {
        this.dailyForecasts = dailyForecasts;
    }
    
    public long getUpdateTimestamp() {
        return updateTimestamp;
    }
    
    public void setUpdateTimestamp(long updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }
    
    /**
     * 获取更新时间戳（别名方法，用于与外部系统兼容）
     */
    public long getUpdateTime() {
        return updateTimestamp;
    }
    
    /**
     * 获取格式化的更新时间字符串
     */
    public String getUpdateTimeString() {
        if (updateTimestamp == 0) {
            return "未知时间";
        }
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(updateTimestamp));
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
        private int weatherIconResource; // 新增图标资源ID属性
        
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
        
        public int getWeatherIconResource() {
            return weatherIconResource;
        }
        
        public void setWeatherIconResource(int weatherIconResource) {
            this.weatherIconResource = weatherIconResource;
        }
    }
}