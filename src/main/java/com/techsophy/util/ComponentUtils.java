package com.techsophy.util;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.util.ResourceUtils;

public class ComponentUtils {

	public static JSONObject fetchComponent(String name) throws IOException, ParseException {
		JSONObject nextComponent=new JSONObject();
		JSONParser parser = new JSONParser();
		String a=ComponentUtils.class.getClassLoader().getResource("config.json").getPath();
		Path file = ResourceUtils.getFile("classpath:config.json").toPath();
		Object obj = parser.parse(new FileReader(file.toString()));
		JSONObject jsonObject = (JSONObject) obj;
		
		nextComponent=  (JSONObject) jsonObject.get(name);
		return nextComponent;
	}
}
