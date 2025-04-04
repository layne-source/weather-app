# 天气应用 (Weather App)

## 项目概述

这是一个功能全面的天气预报应用，为用户提供实时天气信息、空气质量数据和未来天气预报。应用采用现代化的UI设计，支持深色模式，并提供丰富的天气相关数据。

## 功能特点

- **实时天气**: 显示当前温度、天气状况、体感温度等信息
- **天气预报**: 提供未来7天的天气预报
- **空气质量**: 详细展示AQI、PM2.5、PM10等空气质量指标
- **城市管理**: 支持添加、删除和切换多个城市
- **生活指数**: 提供穿衣、运动、旅行、洗车等多种生活指数
- **深色模式**: 支持深色主题，保护夜间视力
- **定位功能**: 支持使用当前位置获取天气信息

## 技术栈

- **开发语言**: Java
- **最低SDK版本**: Android 10 (API 29)
- **目标SDK版本**: Android 14 (API 34)
- **网络请求**: OkHttp 4.10.0
- **JSON解析**: Gson 2.9.0
- **UI组件**: 
  - Material Components 1.9.0
  - AndroidX AppCompat 1.6.1
  - ConstraintLayout 2.1.4
  - RecyclerView 1.3.0
  - CardView 1.0.0

## 项目结构

```
com.microntek.weatherapp/
├── adapter/
│   └── CityAdapter.java       # 城市列表适配器
├── api/
│   └── WeatherApi.java        # 天气API接口封装
├── model/
│   ├── City.java              # 城市数据模型
│   └── Weather.java           # 天气数据模型
├── ui/
│   ├── AirQualityActivity.java # 空气质量详情页面
│   └── SettingsActivity.java   # 设置页面
├── util/
│   ├── CityPreferences.java    # 城市偏好设置工具类
│   └── ThemeHelper.java        # 主题管理工具类
├── MainActivity.java           # 主页面
└── CityManagerActivity.java    # 城市管理页面
```

## 使用说明

1. 首次启动时，应用会默认显示北京的天气情况
2. 点击城市名称可进入城市管理页面添加或切换城市
3. 通过底部导航栏可以快速访问首页、城市管理、空气质量和设置页面
4. 在设置页面可以切换深色模式

## 开发配置

### 前提条件

- Android Studio Hedgehog | 2023.1.1或更高版本
- JDK 1.8或更高版本
- 和风天气API密钥（需要在WeatherApi.java中替换为自己的API_KEY）

### 构建步骤

1. 克隆项目到本地
2. 在Android Studio中打开项目
3. 在WeatherApi.java中替换API_KEY为自己的和风天气API密钥
4. 构建并运行项目

## API密钥设置

在使用应用前，需要在`WeatherApi.java`文件中替换为自己的和风天气API密钥：

```java
// 和风天气API密钥和基础URL
private static final String API_KEY = "YOUR_API_KEY"; // 请替换为您自己的API密钥
```

## 注意事项

- 应用使用和风天气API，请确保有足够的API调用额度
- 位置权限用于获取当前位置的天气信息，请在使用时授予应用相关权限 "# weather-app" 
