package com.microntek.weatherapp.api;

import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.model.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 天气API接口类，使用和风天气API
 * 需要替换成您自己的API_KEY
 */
public class WeatherApi {
    // 和风天气API密钥和基础URL
    private static final String API_KEY = "03aa77458548412d8922be7971b8b9ff"; // 请替换为您自己的API密钥
    private static final String BASE_URL = "https://devapi.qweather.com/v7";
    private static final String GEO_URL = "https://geoapi.qweather.com/v2";
    
    // 请求客户端
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    /**
     * 根据城市ID获取当前天气
     * @param cityId 城市ID
     * @return Weather对象
     */
    public static Weather getCurrentWeather(String cityId) throws IOException, JSONException {
        String url = BASE_URL + "/weather/now?location=" + cityId + "&key=" + API_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            
            // 检查返回码
            if (!"200".equals(json.getString("code"))) {
                throw new IOException("API返回错误: " + json.getString("code"));
            }
            
            // 获取城市信息
            String cityName = getCityName(cityId);
            
            return parseCurrentWeather(json, cityName, cityId);
        }
    }
    
    /**
     * 根据城市经纬度获取当前天气
     * @param lat 纬度
     * @param lon 经度
     * @return Weather对象
     */
    public static Weather getCurrentWeatherByLocation(double lat, double lon) throws IOException, JSONException {
        String location = lon + "," + lat; // 和风天气API使用经度,纬度格式
        String url = BASE_URL + "/weather/now?location=" + location + "&key=" + API_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            
            // 检查返回码
            if (!"200".equals(json.getString("code"))) {
                throw new IOException("API返回错误: " + json.getString("code"));
            }
            
            // 获取城市信息
            String cityId = location;
            String cityName = getCityNameByLocation(lat, lon);
            
            return parseCurrentWeather(json, cityName, cityId);
        }
    }
    
    /**
     * 获取7天天气预报
     * @param cityId 城市ID
     * @return Weather对象，包含预报数据
     */
    public static Weather getForecast(String cityId) throws IOException, JSONException {
        String url = BASE_URL + "/weather/7d?location=" + cityId + "&key=" + API_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            
            // 检查返回码
            if (!"200".equals(json.getString("code"))) {
                throw new IOException("API返回错误: " + json.getString("code"));
            }
            
            // 获取城市信息
            String cityName = getCityName(cityId);
            
            return parseForecast(json, cityName, cityId);
        }
    }
    
    /**
     * 根据城市经纬度获取7天天气预报
     * @param lat 纬度
     * @param lon 经度
     * @return Weather对象，包含预报数据
     */
    public static Weather getForecastByLocation(double lat, double lon) throws IOException, JSONException {
        String location = lon + "," + lat; // 和风天气API使用经度,纬度格式
        String url = BASE_URL + "/weather/7d?location=" + location + "&key=" + API_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            
            // 检查返回码
            if (!"200".equals(json.getString("code"))) {
                throw new IOException("API返回错误: " + json.getString("code"));
            }
            
            // 获取城市信息
            String cityId = location;
            String cityName = getCityNameByLocation(lat, lon);
            
            return parseForecast(json, cityName, cityId);
        }
    }
    
    /**
     * 获取空气质量
     * @param cityId 城市ID
     * @return 更新的Weather对象
     */
    public static Weather getAirQuality(String cityId, Weather weather) throws IOException, JSONException {
        String url = BASE_URL + "/air/now?location=" + cityId + "&key=" + API_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            
            // 检查返回码
            if (!"200".equals(json.getString("code"))) {
                throw new IOException("API返回错误: " + json.getString("code"));
            }
            
            return parseAirQuality(json, weather);
        }
    }
    
    /**
     * 获取生活指数
     * @param cityId 城市ID
     * @return 更新的Weather对象
     */
    public static Weather getLifeIndices(String cityId, Weather weather) throws IOException, JSONException {
        // 类型：1.运动 2.洗车 3.穿衣 4.钓鱼 5.紫外线 6.旅游 7.过敏 8.舒适度 9.感冒 10.空调 11.空气污染扩散条件 12.太阳镜 13.化妆 14.晾晒 15.交通 16.防晒
        String indices = "1,2,3,5,6,8,11,15";
        String url = BASE_URL + "/indices/1d?location=" + cityId + "&type=" + indices + "&key=" + API_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            
            // 检查返回码
            if (!"200".equals(json.getString("code"))) {
                throw new IOException("API返回错误: " + json.getString("code"));
            }
            
            return parseLifeIndices(json, weather);
        }
    }
    
    /**
     * 搜索城市
     * @param cityName 城市名或拼音
     * @return 城市列表
     */
    public static List<City> searchCity(String cityName) throws IOException, JSONException {
        List<City> cities = new ArrayList<>();
        
        // 处理拼音搜索，转换可能的拼音错误
        String normalizedCityName = cityName;
        
        // 常见拼音错误修正映射表
        Map<String, String> pinyinCorrections = new HashMap<>();
        pinyinCorrections.put("tianjing", "tianjin");
        pinyinCorrections.put("shanhai", "shanghai");
        pinyinCorrections.put("beijin", "beijing");
        pinyinCorrections.put("shenzen", "shenzhen");
        pinyinCorrections.put("guangzou", "guangzhou");
        pinyinCorrections.put("chengdu", "chengdu");
        
        // 查找并修正可能的拼音错误
        for (Map.Entry<String, String> entry : pinyinCorrections.entrySet()) {
            if (normalizedCityName.toLowerCase().contains(entry.getKey())) {
                normalizedCityName = normalizedCityName.toLowerCase().replace(entry.getKey(), entry.getValue());
                break;
            }
        }
        
        String url = GEO_URL + "/city/lookup?location=" + normalizedCityName + "&key=" + API_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("网络请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            cities = parseCityResults(responseBody);
            
            // 如果没有结果，尝试使用更宽松的匹配
            if (cities.isEmpty() && !normalizedCityName.equals(cityName)) {
                // 如果修正拼音后仍无结果，尝试原始输入
                url = GEO_URL + "/city/lookup?location=" + cityName + "&key=" + API_KEY;
                
                request = new Request.Builder()
                        .url(url)
                        .build();
                
                try (Response retryResponse = client.newCall(request).execute()) {
                    if (retryResponse.isSuccessful()) {
                        cities = parseCityResults(retryResponse.body().string());
                    }
                }
            }
        }
        
        return cities;
    }
    
    /**
     * 通过经纬度获取城市名称
     */
    private static String getCityNameByLocation(double lat, double lon) throws IOException, JSONException {
        String location = lon + "," + lat;
        String url = GEO_URL + "/city/lookup?location=" + location + "&key=" + API_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "未知位置";
            }
            
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            
            // 检查返回码
            if (!"200".equals(json.getString("code"))) {
                return "未知位置";
            }
            
            JSONArray locations = json.getJSONArray("location");
            if (locations.length() > 0) {
                JSONObject location0 = locations.getJSONObject(0);
                // 返回上级市名称，而不是区县名称
                String adminArea = location0.getString("adm1"); // 省级
                String cityName = location0.getString("adm2"); // 市级
                String districtName = location0.getString("name"); // 区县级
                
                // 如果是直辖市，则直接返回省级名称
                if (adminArea.equals("北京") || adminArea.equals("上海") || 
                    adminArea.equals("天津") || adminArea.equals("重庆")) {
                    return adminArea;
                }
                
                // 如果市名和区县名相同，或区县级是全市，则返回市名
                if (cityName.equals(districtName) || districtName.endsWith("全市")) {
                    return cityName;
                }
                
                // 地级市返回市名
                return cityName;
            }
            
            return "未知位置";
        }
    }
    
    /**
     * 通过城市ID获取城市名称
     */
    private static String getCityName(String cityId) throws IOException, JSONException {
        String url = GEO_URL + "/city/lookup?location=" + cityId + "&key=" + API_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "未知位置";
            }
            
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            
            // 检查返回码
            if (!"200".equals(json.getString("code"))) {
                return "未知位置";
            }
            
            JSONArray locations = json.getJSONArray("location");
            if (locations.length() > 0) {
                JSONObject location0 = locations.getJSONObject(0);
                return location0.getString("name");
            }
            
            return "未知位置";
        }
    }
    
    /**
     * 解析当前天气数据
     */
    private static Weather parseCurrentWeather(JSONObject json, String cityName, String cityId) throws JSONException {
        Weather weather = new Weather();
        
        // 城市信息
        weather.setCityName(cityName);
        
        // 天气信息
        JSONObject now = json.getJSONObject("now");
        
        // 温度信息
        weather.setCurrentTemp(Integer.parseInt(now.getString("temp")));
        weather.setFeelsLike(Integer.parseInt(now.getString("feelsLike")));
        
        // 由于实况天气没有最高最低温度，我们先设置为当前温度
        // 这些将在预报数据中被更新
        weather.setHighTemp(weather.getCurrentTemp());
        weather.setLowTemp(weather.getCurrentTemp());
        
        // 湿度
        weather.setHumidity(Integer.parseInt(now.getString("humidity")));
        
        // 天气描述
        weather.setWeatherDesc(now.getString("text"));
        
        // 图标
        weather.setWeatherIcon(getWeatherIcon(now.getString("icon")));
        
        // 风力信息
        String windDir = now.getString("windDir");
        String windScale = now.getString("windScale");
        weather.setWind(windDir + " " + windScale + "级");
        
        // 设置默认空气质量（会在后续请求中更新）
        weather.setAirQuality("未知");
        weather.setAqi(0);
        weather.setPm25(0);
        weather.setPm10(0);
        
        // 设置生活指数（会在后续请求中更新）
        weather.setClothesIndex("数据加载中...");
        weather.setSportIndex("数据加载中...");
        weather.setUvIndex("数据加载中...");
        weather.setWashCarIndex("数据加载中...");
        weather.setTravelIndex("数据加载中...");
        weather.setComfortIndex("数据加载中...");
        weather.setAirPollutionIndex("数据加载中...");
        weather.setTrafficIndex("数据加载中...");
        
        try {
            // 获取空气质量
            weather = getAirQuality(cityId, weather);
            
            // 获取生活指数
            weather = getLifeIndices(cityId, weather);
        } catch (Exception e) {
            e.printStackTrace();
            // 错误处理，但不影响主要天气数据的返回
        }
        
        return weather;
    }
    
    /**
     * 解析天气预报数据
     */
    private static Weather parseForecast(JSONObject json, String cityName, String cityId) throws JSONException {
        Weather weather = new Weather();
        
        // 城市信息
        weather.setCityName(cityName);
        
        // 获取当日天气数据（用于更新最高最低温度）
        JSONArray daily = json.getJSONArray("daily");
        if (daily.length() > 0) {
            JSONObject today = daily.getJSONObject(0);
            weather.setHighTemp(Integer.parseInt(today.getString("tempMax")));
            weather.setLowTemp(Integer.parseInt(today.getString("tempMin")));
            weather.setSunrise(today.getString("sunrise"));
            weather.setSunset(today.getString("sunset"));
        }
        
        // 提取预报列表
        List<Weather.DailyForecast> forecasts = new ArrayList<>();
        
        for (int i = 0; i < daily.length() && i < 7; i++) {
            JSONObject day = daily.getJSONObject(i);
            
            Weather.DailyForecast forecast = new Weather.DailyForecast();
            forecast.setDate(day.getString("fxDate"));
            
            // 设置星期几
            try {
                java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd");
                java.util.Date dateObj = format.parse(day.getString("fxDate"));
                java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("EEEE", java.util.Locale.CHINA);
                forecast.setDayOfWeek(dayFormat.format(dateObj));
            } catch (Exception e) {
                forecast.setDayOfWeek("未知");
            }
            
            // 温度信息
            forecast.setHighTemp(Integer.parseInt(day.getString("tempMax")));
            forecast.setLowTemp(Integer.parseInt(day.getString("tempMin")));
            
            // 天气描述和图标
            forecast.setWeatherDesc(day.getString("textDay"));
            forecast.setWeatherIcon(getWeatherIcon(day.getString("iconDay")));
            
            forecasts.add(forecast);
        }
        
        weather.setDailyForecasts(forecasts);
        return weather;
    }
    
    /**
     * 解析空气质量数据
     */
    private static Weather parseAirQuality(JSONObject json, Weather weather) throws JSONException {
        JSONObject now = json.getJSONObject("now");
        
        int aqi = Integer.parseInt(now.getString("aqi"));
        int pm25 = Integer.parseInt(now.getString("pm2p5"));
        int pm10 = Integer.parseInt(now.getString("pm10"));
        
        // 解析新增的空气质量数据
        double co = Double.parseDouble(now.getString("co"));
        int so2 = Integer.parseInt(now.getString("so2"));
        int no2 = Integer.parseInt(now.getString("no2"));
        int o3 = Integer.parseInt(now.getString("o3"));
        
        weather.setAqi(aqi);
        weather.setPm25(pm25);
        weather.setPm10(pm10);
        weather.setCo(co);
        weather.setSo2(so2);
        weather.setNo2(no2);
        weather.setO3(o3);
        
        // 设置空气质量描述
        String category = now.getString("category");
        weather.setAirQuality(category);
        
        return weather;
    }
    
    /**
     * 解析生活指数数据
     */
    private static Weather parseLifeIndices(JSONObject json, Weather weather) throws JSONException {
        JSONArray daily = json.getJSONArray("daily");
        
        for (int i = 0; i < daily.length(); i++) {
            JSONObject index = daily.getJSONObject(i);
            String type = index.getString("type");
            String category = index.getString("category");
            String text = index.getString("text");
            
            switch (type) {
                case "1": // 运动
                    weather.setSportIndex(text);
                    break;
                case "2": // 洗车
                    weather.setWashCarIndex(text);
                    break;
                case "3": // 穿衣
                    weather.setClothesIndex(text);
                    break;
                case "5": // 紫外线
                    weather.setUvIndex(text);
                    break;
                case "6": // 旅游
                    weather.setTravelIndex(text);
                    break;
                case "8": // 舒适度
                    weather.setComfortIndex(text);
                    break;
                case "11": // 空气污染扩散条件
                    weather.setAirPollutionIndex(text);
                    break;
                case "15": // 交通
                    weather.setTrafficIndex(text);
                    break;
            }
        }
        
        return weather;
    }
    
    /**
     * 解析城市搜索结果
     */
    private static List<City> parseCityResults(String responseBody) throws JSONException {
        List<City> cities = new ArrayList<>();
        
        JSONObject json = new JSONObject(responseBody);
        
        // 检查返回码
        if (!"200".equals(json.getString("code"))) {
            return cities; // 返回空列表
        }
        
        JSONArray locations = json.getJSONArray("location");
        for (int i = 0; i < locations.length(); i++) {
            JSONObject location = locations.getJSONObject(i);
            
            String id = location.getString("id");
            String name = location.getString("name");
            double lat = location.getDouble("lat");
            double lon = location.getDouble("lon");
            
            // 获取省份和区县信息
            String province = location.has("adm1") ? location.getString("adm1") : "";
            String district = location.has("adm2") ? location.getString("adm2") : "";
            
            City city = new City(name, id, province, lat, lon);
            city.setDistrict(district);
            
            cities.add(city);
        }
        
        return cities;
    }
    
    /**
     * 根据天气代码返回对应的天气图标
     */
    private static String getWeatherIcon(String iconCode) {
        // 和风天气图标编码与emoji映射
        // 这里只列出了常见的几种，可以根据需要扩展
        switch (iconCode) {
            case "100": // 晴天
                return "☀️";
            case "101": // 多云
            case "102": // 少云
            case "103": // 晴间多云
                return "🌤️";
            case "104": // 阴天
                return "☁️";
            case "150": // 晴天夜间
            case "151": // 多云夜间
            case "152": // 少云夜间
            case "153": // 晴间多云夜间
                return "🌙";
            case "300": // 阵雨
            case "301": // 强阵雨
            case "302": // 雷阵雨
            case "303": // 强雷阵雨
            case "304": // 雷阵雨伴有冰雹
                return "⛈️";
            case "305": // 小雨
            case "306": // 中雨
            case "307": // 大雨
            case "308": // 极端降雨
            case "309": // 毛毛雨/细雨
            case "310": // 暴雨
            case "311": // 大暴雨
            case "312": // 特大暴雨
            case "313": // 冻雨
            case "314": // 小到中雨
            case "315": // 中到大雨
            case "316": // 大到暴雨
            case "317": // 暴雨到大暴雨
            case "318": // 大暴雨到特大暴雨
                return "🌧️";
            case "400": // 小雪
            case "401": // 中雪
            case "402": // 大雪
            case "403": // 暴雪
            case "404": // 雨夹雪
            case "405": // 雨雪天气
            case "406": // 阵雨夹雪
            case "407": // 阵雪
            case "408": // 小到中雪
            case "409": // 中到大雪
            case "410": // 大到暴雪
                return "❄️";
            case "500": // 薄雾
            case "501": // 雾
            case "502": // 霾
            case "503": // 扬沙
            case "504": // 浮尘
            case "507": // 沙尘暴
            case "508": // 强沙尘暴
                return "🌫️";
            case "900": // 热
                return "🔥";
            case "901": // 冷
                return "❄️";
            default:
                return "❓";
        }
    }
} 