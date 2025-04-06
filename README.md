# 天气应用 (Weather App)

一个功能全面的天气应用，提供实时天气信息、多城市管理、定位服务和第三方应用接口支持。

## 功能特点

- 实时天气数据显示（温度、天气状况、湿度、风向等）
- 多城市管理（添加、删除、排序）
- 当前位置自动定位和天气获取
- 空气质量指数 (AQI) 信息
- 天气预报（未来几天）
- 系统级天气数据广播服务，支持第三方应用集成
- 开机自启动，后台自动更新天气数据
- 美观易用的界面设计

## 系统架构

应用遵循 MVC 架构设计：

- **模型层 (Model)**: 数据对象如城市和天气信息
- **视图层 (View)**: Activity、Fragment和布局文件
- **控制层 (Controller)**: 业务逻辑处理和数据管理

同时采用服务化设计，将天气数据更新和广播功能封装在系统服务中，保证数据一致性和高效共享。

## 第三方应用集成

### 概述

本应用提供了系统级广播服务，使第三方应用可以方便地访问天气数据而无需自行实现天气API调用。主要特点：

- 定时广播最新天气数据（每小时更新）
- 支持按需请求天气数据更新
- 开机自启动，确保数据持续可用
- 广播内容包含完整的天气和城市信息

### 集成步骤

#### 1. 添加广播接收器

在您的应用中创建BroadcastReceiver来接收天气数据：

```java
private final BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("com.microntek.weatherapp.WEATHER_DATA".equals(action)) {
            // 处理天气数据
            String cityName = intent.getStringExtra("city_name");
            int currentTemp = intent.getIntExtra("current_temp", 0);
            String weatherDesc = intent.getStringExtra("weather_desc");
            
            // 更新UI或处理数据...
        }
    }
};
```

#### 2. 注册和注销接收器

在Activity或Service的生命周期方法中注册接收器：

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // 其他初始化代码...
    
    IntentFilter filter = new IntentFilter();
    filter.addAction("com.microntek.weatherapp.WEATHER_DATA");
    registerReceiver(weatherReceiver, filter);
}

@Override
protected void onDestroy() {
    unregisterReceiver(weatherReceiver);
    super.onDestroy();
}
```

#### 3. 请求天气数据更新（可选）

如需主动请求天气数据更新：

```java
private void requestWeatherUpdate() {
    Intent intent = new Intent("com.microntek.weatherapp.REQUEST_UPDATE");
    sendBroadcast(intent);
}
```

### 天气数据广播字段说明

广播Intent (`com.microntek.weatherapp.WEATHER_DATA`) 包含以下数据字段：

| 字段名称 | 类型 | 说明 |
|---------|------|------|
| `city_name` | String | 城市名称 |
| `city_id` | String | 城市唯一标识 |
| `is_current_location` | boolean | 是否为当前位置城市 |
| `current_temp` | int | 当前温度（摄氏度） |
| `high_temp` | int | 最高温度（摄氏度） |
| `low_temp` | int | 最低温度（摄氏度） |
| `feels_like_temp` | int | 体感温度（摄氏度） |
| `weather_desc` | String | 天气状况描述（如"晴"、"多云"） |
| `weather_icon` | String | 天气图标代码 |
| `weather_code` | int | 天气状况代码 |
| `humidity` | int | 湿度百分比（0-100） |
| `wind_direction` | String | 风向（如"东北风"） |
| `wind_speed` | String | 风速（如"3级"） |
| `aqi` | int | 空气质量指数 |
| `air_quality` | String | 空气质量级别（如"优"、"良"） |
| `last_update_time` | long | 天气数据最后更新时间（毫秒时间戳） |
| `update_time` | long | 广播发送时间（毫秒时间戳） |

### 广播Action常量

| Action | 说明 |
|--------|------|
| `com.microntek.weatherapp.WEATHER_DATA` | 天气数据广播 |
| `com.microntek.weatherapp.REQUEST_UPDATE` | 请求更新天气数据 |
| `com.microntek.weatherapp.CITY_CHANGED` | 当前城市已变更 |
| `com.microntek.weatherapp.SERVICE_STATUS` | 服务状态信息 |

### 提供的辅助类

为简化集成过程，我们提供了`WeatherDataManager`类，封装了广播接收和发送的逻辑：

```java
// 初始化
WeatherDataManager weatherManager = new WeatherDataManager(context);
weatherManager.setWeatherDataListener(new WeatherDataManager.WeatherDataListener() {
    @Override
    public void onWeatherDataReceived(WeatherDataManager.WeatherData data) {
        // 处理接收到的天气数据
        String cityName = data.cityName;
        int temperature = data.currentTemp;
        // 更新UI...
    }
    
    @Override
    public void onServiceStatusReceived(boolean isRunning, long lastUpdateTime, boolean hasData) {
        // 处理服务状态
    }
});

// 注册接收器
weatherManager.register();

// 请求更新数据
weatherManager.requestWeatherUpdate(null);

// 在组件销毁时注销接收器
@Override
protected void onDestroy() {
    weatherManager.unregister();
    super.onDestroy();
}
```

### 完整集成示例

请参考项目中的示例代码：
- `WeatherDataReceiverDemo.java` - 完整的Activity示例
- `WeatherDataManager.java` - 便捷的数据管理工具类
- `WeatherDataUsageExample.java` - 最简单的实现示例

## 开发环境

- Android Studio 4.2+
- Gradle 7.0+
- minSdkVersion: 21 (Android 5.0)
- targetSdkVersion: 31 (Android 12)
- Java 8

## 项目结构

```
app/
├── src/main/
│   ├── java/com/microntek/weatherapp/
│   │   ├── api/             # 天气API相关类
│   │   ├── model/           # 数据模型类
│   │   ├── service/         # 服务类（包含天气数据广播服务）
│   │   ├── receiver/        # 广播接收器类
│   │   ├── ui/              # Activity和Fragment等界面类
│   │   ├── util/            # 工具类
│   │   ├── demo/            # 第三方集成示例代码
│   │   └── MainActivity.java# 主界面Activity
│   └── res/                 # 资源文件
└── build.gradle            # 构建配置
```

## 版权和许可

© 2023 HCT Weather App

本项目遵循Apache 2.0许可协议
