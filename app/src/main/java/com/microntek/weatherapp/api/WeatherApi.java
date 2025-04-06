package com.microntek.weatherapp.api;

import android.content.Context;
import android.util.Log;

import com.microntek.weatherapp.R;
import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.model.Weather;
import com.microntek.weatherapp.util.WeatherDataCache;

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

    // 缓存管理器实例
    private static WeatherDataCache weatherDataCache;
    
    /**
     * 初始化缓存管理器
     */
    public static void initCache(Context context) {
        if (weatherDataCache == null) {
            weatherDataCache = WeatherDataCache.getInstance(context);
        }
    }

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
        String indices = "1,2,3,5,6,8,9,11,15";
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
        
        // 设置当前天气描述和图标
        String code = now.getString("icon");
        weather.setWeatherDesc(now.getString("text"));
        weather.setWeatherIcon(getWeatherIcon(code));
        weather.setWeatherIconResource(getWeatherIconResource(code));
        
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
        weather.setFluIndex("数据加载中...");
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
            forecast.setWeatherIconResource(getWeatherIconResource(day.getString("iconDay")));
            
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
                    weather.setSportCategory(category);
                    break;
                case "2": // 洗车
                    weather.setWashCarIndex(text);
                    weather.setWashCarCategory(category);
                    break;
                case "3": // 穿衣
                    weather.setClothesIndex(text);
                    weather.setClothesCategory(category);
                    break;
                case "5": // 紫外线
                    weather.setUvIndex(text);
                    weather.setUvCategory(category);
                    break;
                case "6": // 旅游
                    weather.setTravelIndex(text);
                    weather.setTravelCategory(category);
                    break;
                case "8": // 舒适度
                    weather.setComfortIndex(text);
                    weather.setComfortCategory(category);
                    break;
                case "9": // 感冒
                    weather.setFluIndex(text);
                    weather.setFluCategory(category);
                    break;
                case "11": // 空气污染扩散条件
                    weather.setAirPollutionIndex(text);
                    weather.setAirPollutionCategory(category);
                    break;
                case "15": // 交通
                    weather.setTrafficIndex(text);
                    weather.setTrafficCategory(category);
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
     * 根据天气代码返回对应的图标资源ID
     */
    private static int getWeatherIconResource(String iconCode) {
        try {
            // 添加前缀"icon_"，因为Android资源名称必须以字母开头
            String resourceName = "icon_" + iconCode;
            Class<?> drawableClass = R.drawable.class;
            java.lang.reflect.Field field = drawableClass.getField(resourceName);
            return field.getInt(null);
        } catch (Exception e) {
            // 如果找不到对应的资源，返回默认图标
            try {
                // 尝试使用默认图标
                return R.drawable.class.getField("icon_399").getInt(null);
            } catch (Exception ex) {
                // 如果默认图标也不存在，返回0
                return 0;
            }
        }
    }
    
    /**
     * 根据天气代码返回对应的天气图标（Emoji）
     * 保留此方法是为了兼容性考虑
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

    /**
     * 根据城市ID获取当前天气（带缓存）
     */
    public static Weather getCurrentWeatherWithCache(Context context, String cityId) 
            throws IOException, JSONException {
        
        initCache(context);
        
        // 先尝试从缓存获取
        Weather cachedWeather = weatherDataCache.getCachedCurrentWeather(cityId);
        if (cachedWeather != null) {
            return cachedWeather;
        }
        
        // 缓存不存在或已过期，从API获取
        Weather weather = getCurrentWeather(cityId);
        
        // 保存到缓存
        if (weather != null) {
            weatherDataCache.cacheCurrentWeather(cityId, weather);
        }
        
        return weather;
    }
    
    /**
     * 根据经纬度获取当前天气（带缓存）
     */
    public static Weather getCurrentWeatherByLocationWithCache(Context context, double lat, double lon) 
            throws IOException, JSONException {
        
        initCache(context);
        
        String locationId = lon + "," + lat; // 和风天气API使用经度,纬度格式
        
        // 先尝试从缓存获取
        Weather cachedWeather = weatherDataCache.getCachedCurrentWeather(locationId);
        if (cachedWeather != null) {
            return cachedWeather;
        }
        
        // 缓存不存在或已过期，从API获取
        Weather weather = getCurrentWeatherByLocation(lat, lon);
        
        // 保存到缓存
        if (weather != null) {
            weatherDataCache.cacheCurrentWeather(locationId, weather);
        }
        
        return weather;
    }
    
    /**
     * 获取7天天气预报（带缓存）
     */
    public static Weather getForecastWithCache(Context context, String cityId) 
            throws IOException, JSONException {
        
        initCache(context);
        
        // 先尝试从缓存获取
        Weather cachedWeather = weatherDataCache.getCachedForecastWeather(cityId);
        if (cachedWeather != null) {
            return cachedWeather;
        }
        
        // 缓存不存在或已过期，从API获取
        Weather weather = getForecast(cityId);
        
        // 保存到缓存
        if (weather != null) {
            weatherDataCache.cacheForecastWeather(cityId, weather);
        }
        
        return weather;
    }
    
    /**
     * 根据经纬度获取7天天气预报（带缓存）
     */
    public static Weather getForecastByLocationWithCache(Context context, double lat, double lon) 
            throws IOException, JSONException {
        
        initCache(context);
        
        String locationId = lon + "," + lat;
        
        // 先尝试从缓存获取
        Weather cachedWeather = weatherDataCache.getCachedForecastWeather(locationId);
        if (cachedWeather != null) {
            return cachedWeather;
        }
        
        // 缓存不存在或已过期，从API获取
        Weather weather = getForecastByLocation(lat, lon);
        
        // 保存到缓存
        if (weather != null) {
            weatherDataCache.cacheForecastWeather(locationId, weather);
        }
        
        return weather;
    }
    
    /**
     * 获取空气质量（带缓存）
     */
    public static Weather getAirQualityWithCache(Context context, String cityId, Weather weather) 
            throws IOException, JSONException {
        
        initCache(context);
        
        // 先尝试从缓存获取
        Weather cachedAir = weatherDataCache.getCachedAirQuality(cityId);
        if (cachedAir != null) {
            // 将缓存的空气质量数据合并到当前天气对象
            weather.setAirQuality(cachedAir.getAirQuality());
            weather.setAqi(cachedAir.getAqi());
            weather.setPm25(cachedAir.getPm25());
            weather.setPm10(cachedAir.getPm10());
            weather.setCo(cachedAir.getCo());
            weather.setSo2(cachedAir.getSo2());
            weather.setNo2(cachedAir.getNo2());
            weather.setO3(cachedAir.getO3());
            return weather;
        }
        
        // 缓存不存在或已过期，从API获取
        Weather updatedWeather = getAirQuality(cityId, weather);
        
        // 保存到缓存
        if (updatedWeather != null) {
            weatherDataCache.cacheAirQuality(cityId, updatedWeather);
        }
        
        return updatedWeather;
    }
    
    /**
     * 获取生活指数（带缓存）
     */
    public static Weather getLifeIndicesWithCache(Context context, String cityId, Weather weather) 
            throws IOException, JSONException {
        
        initCache(context);
        
        // 先尝试从缓存获取
        Weather cachedIndices = weatherDataCache.getCachedLifeIndices(cityId);
        if (cachedIndices != null) {
            // 将缓存的生活指数数据合并到当前天气对象
            mergeIndicesData(weather, cachedIndices);
            return weather;
        }
        
        // 缓存不存在或已过期，从API获取
        Weather updatedWeather = getLifeIndices(cityId, weather);
        
        // 保存到缓存
        if (updatedWeather != null) {
            weatherDataCache.cacheLifeIndices(cityId, updatedWeather);
        }
        
        return updatedWeather;
    }
    
    /**
     * 合并生活指数数据到天气对象
     */
    private static void mergeIndicesData(Weather target, Weather source) {
        target.setClothesIndex(source.getClothesIndex());
        target.setClothesCategory(source.getClothesCategory());
        target.setSportIndex(source.getSportIndex());
        target.setSportCategory(source.getSportCategory());
        target.setUvIndex(source.getUvIndex());
        target.setUvCategory(source.getUvCategory());
        target.setWashCarIndex(source.getWashCarIndex());
        target.setWashCarCategory(source.getWashCarCategory());
        target.setTravelIndex(source.getTravelIndex());
        target.setTravelCategory(source.getTravelCategory());
        target.setComfortIndex(source.getComfortIndex());
        target.setComfortCategory(source.getComfortCategory());
        target.setAirPollutionIndex(source.getAirPollutionIndex());
        target.setAirPollutionCategory(source.getAirPollutionCategory());
        target.setTrafficIndex(source.getTrafficIndex());
        target.setTrafficCategory(source.getTrafficCategory());
        target.setFluIndex(source.getFluIndex());
        target.setFluCategory(source.getFluCategory());
    }
    
    /**
     * 合并天气数据（将天气预报数据合并到当前天气）
     */
    public static void mergeWeatherData(Weather target, Weather source) {
        // 更新当天的最高最低温度
        target.setHighTemp(source.getHighTemp());
        target.setLowTemp(source.getLowTemp());
        
        // 如果预报中的日出日落时间存在，也更新它们
        if (source.getSunrise() != null && !source.getSunrise().isEmpty()) {
            target.setSunrise(source.getSunrise());
        }
        if (source.getSunset() != null && !source.getSunset().isEmpty()) {
            target.setSunset(source.getSunset());
        }
        
        // 更新预报列表
        target.setDailyForecasts(source.getDailyForecasts());
        
        // 如果当天预报数据中有更详细的天气描述，也可以更新
        if (source.getDailyForecasts() != null && source.getDailyForecasts().size() > 0) {
            Weather.DailyForecast todayForecast = source.getDailyForecasts().get(0);
            if (target.getWeatherDesc() == null || target.getWeatherDesc().isEmpty()) {
                target.setWeatherDesc(todayForecast.getWeatherDesc());
            }
        }
    }
    
    /**
     * 搜索城市（带缓存）
     */
    public static List<City> searchCityWithCache(Context context, String cityName) 
            throws IOException, JSONException {
        
        initCache(context);
        
        // 先尝试从缓存获取
        List<City> cachedCities = weatherDataCache.getCachedCitySearchResult(cityName);
        if (cachedCities != null) {
            return cachedCities;
        }
        
        // 缓存不存在或已过期，从API获取
        List<City> cities = searchCity(cityName);
        
        // 保存到缓存
        if (cities != null && !cities.isEmpty()) {
            weatherDataCache.cacheCitySearchResult(cityName, cities);
        }
        
        return cities;
    }
    
    /**
     * 刷新天气数据（忽略缓存）
     */
    public static Weather refreshWeatherData(Context context, String cityId) 
            throws IOException, JSONException {
        
        initCache(context);
        
        // 从API获取最新数据
        Weather currentWeather = getCurrentWeather(cityId);
        
        if (currentWeather != null) {
            // 获取并合并天气预报
            try {
                Weather forecastWeather = getForecast(cityId);
                if (forecastWeather != null) {
                    mergeWeatherData(currentWeather, forecastWeather);
                    weatherDataCache.cacheForecastWeather(cityId, forecastWeather);
                }
            } catch (Exception e) {
                Log.e("WeatherApi", "获取天气预报失败: " + e.getMessage());
            }
            
            // 获取并合并空气质量
            try {
                currentWeather = getAirQuality(cityId, currentWeather);
                weatherDataCache.cacheAirQuality(cityId, currentWeather);
            } catch (Exception e) {
                Log.e("WeatherApi", "获取空气质量失败: " + e.getMessage());
            }
            
            // 获取并合并生活指数
            try {
                currentWeather = getLifeIndices(cityId, currentWeather);
                weatherDataCache.cacheLifeIndices(cityId, currentWeather);
            } catch (Exception e) {
                Log.e("WeatherApi", "获取生活指数失败: " + e.getMessage());
            }
            
            // 保存当前天气到缓存
            weatherDataCache.cacheCurrentWeather(cityId, currentWeather);
        }
        
        return currentWeather;
    }
    
    /**
     * 根据经纬度刷新天气数据（忽略缓存）
     */
    public static Weather refreshWeatherDataByLocation(Context context, double lat, double lon) 
            throws IOException, JSONException {
        
        initCache(context);
        String locationId = lon + "," + lat;
        
        // 从API获取最新数据
        Weather currentWeather = getCurrentWeatherByLocation(lat, lon);
        
        if (currentWeather != null) {
            // 获取并合并天气预报
            try {
                Weather forecastWeather = getForecastByLocation(lat, lon);
                if (forecastWeather != null) {
                    mergeWeatherData(currentWeather, forecastWeather);
                    weatherDataCache.cacheForecastWeather(locationId, forecastWeather);
                }
            } catch (Exception e) {
                Log.e("WeatherApi", "获取天气预报失败: " + e.getMessage());
            }
            
            // 获取并合并空气质量
            try {
                currentWeather = getAirQuality(locationId, currentWeather);
                weatherDataCache.cacheAirQuality(locationId, currentWeather);
            } catch (Exception e) {
                Log.e("WeatherApi", "获取空气质量失败: " + e.getMessage());
            }
            
            // 获取并合并生活指数
            try {
                currentWeather = getLifeIndices(locationId, currentWeather);
                weatherDataCache.cacheLifeIndices(locationId, currentWeather);
            } catch (Exception e) {
                Log.e("WeatherApi", "获取生活指数失败: " + e.getMessage());
            }
            
            // 保存当前天气到缓存
            weatherDataCache.cacheCurrentWeather(locationId, currentWeather);
        }
        
        return currentWeather;
    }
    
    /**
     * 验证并修复缓存数据
     * 检查指定城市的缓存完整性，尝试修复损坏的缓存
     * @param context 上下文
     * @param cityId 城市ID或经纬度ID
     * @return 是否有缓存被修复
     */
    public static boolean verifyAndRepairCache(Context context, String cityId) {
        initCache(context);
        
        boolean repaired = weatherDataCache.checkAndRepairCache(cityId);
        
        if (repaired) {
            Log.i("WeatherApi", "已修复城市 " + cityId + " 的部分缓存数据");
        }
        
        // 创建备份
        weatherDataCache.createBackup();
        
        return repaired;
    }
    
    /**
     * 验证并修复缓存数据
     * 检查指定城市的缓存完整性，尝试修复损坏的缓存
     * @param context 上下文
     * @param lat 纬度
     * @param lon 经度
     * @return 是否有缓存被修复
     */
    public static boolean verifyAndRepairCacheByLocation(Context context, double lat, double lon) {
        String locationId = lon + "," + lat;
        return verifyAndRepairCache(context, locationId);
    }

    /**
     * 通过经纬度获取城市对象
     * @param lat 纬度
     * @param lon 经度
     * @return 城市对象
     */
    public static City getCityByLocation(double lat, double lon) throws IOException, JSONException {
        String location = lon + "," + lat;
        String url = GEO_URL + "/city/lookup?location=" + location + "&key=" + API_KEY;
        
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
            
            JSONArray locations = json.getJSONArray("location");
            if (locations.length() > 0) {
                JSONObject location0 = locations.getJSONObject(0);
                
                City city = new City();
                city.setId(location0.getString("id"));
                
                // 设置经纬度
                city.setLatitude(lat);
                city.setLongitude(lon);
                
                // 设置省份
                String adminArea = location0.getString("adm1"); // 省级
                city.setProvince(adminArea);
                
                // 设置城市
                String cityName = location0.getString("adm2"); // 市级
                String districtName = location0.getString("name"); // 区县级
                
                // 如果是直辖市，则使用省级名称作为城市名
                if (adminArea.equals("北京") || adminArea.equals("上海") || 
                    adminArea.equals("天津") || adminArea.equals("重庆")) {
                    city.setName(adminArea);
                    // 区县级信息可以保留在district字段但不显示
                    city.setDistrict("");
                }
                // 其他城市都只精确到市级别
                else {
                    city.setName(cityName);
                    city.setDistrict("");
                }
                
                // 设置为当前位置标记
                city.setCurrentLocation(true);
                
                return city;
            }
            
            throw new IOException("未找到位置信息");
        }
    }
} 