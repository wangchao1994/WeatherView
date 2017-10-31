package com.android.topview.bean;

/**
 * 
 * 实体类
 * **/
public class Cityweather {
	private String code;// 天气图片code
	private String date;// 日期
	private String hightemp;// 最高温度
	private String lowtemp;// 最低温度
	private String currentTemp;//当前温度
	private String day;// 
	private String weathertext;// 天气信息

	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getHightemp() {
		return hightemp;
	}

	public void setHightemp(String hightemp) {
		this.hightemp = hightemp;
	}

	public String getLowtemp() {
		return lowtemp;
	}

	public void setLowtemp(String lowtemp) {
		this.lowtemp = lowtemp;
	}

	public String getCurrentTemp() {
		return currentTemp;
	}

	public void setCurrentTemp(String currentTemp) {
		this.currentTemp = currentTemp;
	}

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public String getWeathertext() {
		return weathertext;
	}

	public void setWeathertext(String weathertext) {
		this.weathertext = weathertext;
	}

}


