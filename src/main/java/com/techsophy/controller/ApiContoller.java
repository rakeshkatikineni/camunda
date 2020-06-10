package com.techsophy.controller;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techsophy.models.ApiErrorResponse;
import com.techsophy.models.ApiSuccessResponse;
import com.techsophy.models.BPMModel;
import com.techsophy.models.IApiResponse;
import com.techsophy.util.ComponentUtils;

@RestController
@RequestMapping("/v1")
public class ApiContoller {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	Environment env;

	@Autowired
	RestTemplate restTemplate;

	@PostMapping(value = "/loan-process")
	public ResponseEntity<IApiResponse> triggerBPM(@RequestBody String details) throws IOException, ParseException {
		JSONParser parser = new JSONParser();
		Path file = ResourceUtils.getFile("classpath:config.json").toPath();
		Object obj = parser.parse(new FileReader(file.toString()));
		System.out.println(obj);
		final String ROOT_URI = env.getProperty("camunda-url");
		final String bpmKey = env.getProperty("bpm-key");
		try {

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> request = new HttpEntity<String>(details, headers);
			String response = restTemplate.postForObject(ROOT_URI + "/process-definition/key/" + bpmKey + "/start",
					request, String.class);
			JSONObject json = new JSONObject(response);
			String pid = (String) json.get("id");
			String variables = fetchPid("app", pid);
			String p = fetchChildTasks(pid);
			Map<String, Object> variables1 = fetchActiveTasks(p, variables, true);
			return ResponseEntity.status(HttpStatus.OK)
					.body(new ApiSuccessResponse(variables1, true, "Sucessfully triggered", HttpStatus.OK));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(new ApiErrorResponse(null, false, "Error Occured", HttpStatus.INTERNAL_SERVER_ERROR));

		}

	}

	@PostMapping(value = "/claim/{id}")
	public ResponseEntity<IApiResponse> claimTask(@PathVariable String id, @RequestBody String details) {
		BPMModel model = new BPMModel();
		Map<String, String> map = new HashMap<>();
		final String ROOT_URI = env.getProperty("camunda-url");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> request = new HttpEntity<String>(details, headers);
		String s = restTemplate.postForObject(ROOT_URI + "/task/" + id + "/claim", request, String.class);

		return ResponseEntity.status(HttpStatus.OK)
				.body(new ApiSuccessResponse(s, true, "Sucessfully triggered", HttpStatus.OK));
	}

	@PostMapping(value = "/actions")
	public ResponseEntity<IApiResponse> completeTask(@RequestParam String applicationNo, @RequestBody String details) {
		final String ROOT_URI = env.getProperty("camunda-url");
		try {
			String variables = fetchProcessByVariable("app", applicationNo);
			String p = fetchChildTasks(variables);
			String taskId = fetchTaskId(p, true);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> request = new HttpEntity<String>(details, headers);
			String s = restTemplate.postForObject(ROOT_URI + "/task/" + taskId + "/complete", request, String.class);
			Map<String, Object> sd = fetchActiveTasks(p, applicationNo, true);
			if (sd != null) {
				return ResponseEntity.status(HttpStatus.OK)
						.body(new ApiSuccessResponse(sd, true, "Sucessfully triggered", HttpStatus.OK));
			} else {
				return ResponseEntity.status(HttpStatus.OK)
						.body(new ApiSuccessResponse(sd, false, "No Component Found", HttpStatus.OK));
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(new ApiErrorResponse(null, false, "Error  Occured", HttpStatus.OK));
		}
	}

	public String fetchTaskId(String processInstanceId, boolean unfinished) {
		final String ROOT_URI = env.getProperty("camunda-url");
		String s = restTemplate.getForObject(
				ROOT_URI + "/history/task?processInstanceId=" + processInstanceId + "&unfinished=" + unfinished,
				String.class);
		JSONArray json = new JSONArray(s);
		String a = null;
		JSONObject j1 = new JSONObject(json.get(0).toString());
		String id = (String) j1.get("id");

		return id;
	}

	public Map<String, Object> fetchActiveTasks(String processInstanceId, String applicationNo, boolean unfinished)
			throws IOException, ParseException {
		final String ROOT_URI = env.getProperty("camunda-url");
		String s = restTemplate.getForObject(
				ROOT_URI + "/history/task?processInstanceId=" + processInstanceId + "&unfinished=" + unfinished,
				String.class);
		JSONArray json = new JSONArray(s);
		Double eligibleAmount = null;
		org.json.simple.JSONObject componentName = null;
		JSONObject j1 = new JSONObject(json.get(0).toString());
		String name = (String) j1.get("name");
		String taskId = (String) j1.get("id");
		ComponentUtils c=new ComponentUtils();
		componentName = c.fetchComponent(name);
		if (name.equals(env.getProperty("eligiblekey"))) {
			// a="EligibleAmount";
			Double amountPercentage = fetchProcessByVariablev("amount", processInstanceId);
			Double amountExpense = fetchProcessByVariablev("grossAnnualExpense", processInstanceId);
			Double amountIncome = fetchProcessByVariablev("grossAnnualIncome", processInstanceId);
			Double v = amountPercentage / 100;
			eligibleAmount = v * amountIncome;

		}
		Map<String, Object> json1 = null;
		if (componentName != null) {
			json1 = new HashMap<>();
			json1.put("nextComponent", componentName);
			json1.put("applicationNo", applicationNo);
			if (name.equals(env.getProperty("eligiblekey"))) {
				json1.put("eligibleAmount", eligibleAmount.toString());
			}
		}
		return json1;
	}

	private String fetchChildTasks(String processInstanceId) {
		final String ROOT_URI = env.getProperty("camunda-url");
		String s = restTemplate.getForObject(
				ROOT_URI + "/history/activity-instance?processInstanceId=" + processInstanceId + "&unfinished=true",
				String.class);
		String a = null;
		if (s != null && !s.isEmpty()) {
			JSONArray ar = new JSONArray(s);
			JSONObject j = new JSONObject(ar.get(0).toString());
			a = (String) j.get("calledProcessInstanceId");
		}
		return a;
	}

	private String fetchPid(String applicationNo, String pid) {
		final String ROOT_URI = env.getProperty("camunda-url");
		String value = null;
		String s = restTemplate.getForObject(
				ROOT_URI + " /history/variable-instance?variableName=" + applicationNo + "&processInstanceId=" + pid,
				String.class);
		if (s != null && !s.isEmpty()) {

			JSONArray json = new JSONArray(s);
			JSONObject j = new JSONObject(json.get(0).toString());
			value = j.getString("value");
		}
		return value;
	}

	private Double fetchProcessByVariablev(String variableName, String pid) {
		final String ROOT_URI = env.getProperty("camunda-url");
		String s = restTemplate.getForObject(
				ROOT_URI + "/history/variable-instance?processInstanceId=" + pid + "&variableName=" + variableName,
				String.class);
		Double value = null;
		if (s != null && !s.isEmpty()) {

			JSONArray json = new JSONArray(s);
			JSONObject j = new JSONObject(json.get(0).toString());
			if (variableName.equals("amount")) {
				JSONObject v = j.getJSONObject("value");
				value = v.getDouble("amountpercentage");
			} else {
				value = j.getDouble("value");
			}

		}
		return value;
	}

	private String fetchProcessByVariable(String variableName, String variable) {
		final String ROOT_URI = env.getProperty("camunda-url");
		String s = restTemplate.getForObject(
				ROOT_URI + "/history/variable-instance?variableName=" + variableName + "&variableValue=" + variable,
				String.class);
		String value = null;
		if (s != null && !s.isEmpty()) {

			JSONArray json = new JSONArray(s);
			JSONObject j = new JSONObject(json.get(0).toString());
			value = j.getString("processInstanceId");
		}
		return value;
	}

}
