package controllers;

import java.util.ArrayList;
import java.util.Iterator;

import models.cmu.sv.sensor.DBHandler;
import models.cmu.sv.sensor.MessageBusHandler;
//import models.cmu.sv.sensor.SensorReading;

import org.codehaus.jackson.JsonNode;

import play.mvc.Controller;
import play.mvc.Result;


public class SensorReading extends Controller {
	private static DBHandler dbHandler = null;
	private static boolean testDBHandler(){
		if(dbHandler == null){
			dbHandler = new DBHandler("conf/database.properties");
		}
		return true;
	}
	
	public static Result add(Boolean publish) {
		JsonNode json = request().body().asJson();
		 if(json == null) {
			    return badRequest("Expecting Json data");
		 } 
		 if(!testDBHandler()){
			 return internalServerError("database conf file not found");
		 }
		 
		 // Parse JSON FIle 
		 String deviceId = json.findPath("id").getTextValue();
		 Long timeStamp = json.findPath("timestamp").getLongValue();
		 Iterator<String> it = json.getFieldNames();
		 ArrayList<String> error = new ArrayList<String>();
		 while(it.hasNext()){
			 String sensorType = it.next();
			 if(sensorType == "id" || sensorType == "timestamp") continue;
			 double value = json.findPath(sensorType).getDoubleValue();
			 models.cmu.sv.sensor.SensorReading reading = new models.cmu.sv.sensor.SensorReading(deviceId, timeStamp, sensorType, value); 
			 if(!reading.save()){
				 error.add(sensorType);
			 }
			 
			 if(publish){
				 MessageBusHandler mb = new MessageBusHandler();
				 if(!mb.publish(reading)){
					 error.add("publish failed");
				 }
			 }
		 }
		 if(error.size() == 0){
			 System.out.println("saved");
             return ok("saved");
         }
		 else{
			 System.out.println("some not saved: " + error.toString());
			 return ok("some not saved: " + error.toString());
		 }
	}
	
	// query for readings
	public static Result sql_query(){
		String resultStr = "";
		
		if(!testDBHandler()){
			return internalServerError("database conf file not found");
		}
		try {
			response().setHeader("Access-Control-Allow-Origin", "*");
			JsonNode sql_json = request().body().asJson();
			if (sql_json == null) {
				return badRequest("Expect sql in valid json");
			}
			String sql = sql_json.findPath("sql").getTextValue();
			int number_of_result_columns = sql_json.findPath("number_of_result_columns").getIntValue();			
			resultStr = dbHandler.runQuery(sql, number_of_result_columns);
		} catch(Exception e){
			e.printStackTrace();
		}
		return ok(resultStr);		
	}
	
	// search reading at a specific timestamp
	public static Result searchAtTime(String deviceId, Long timeStamp, String sensorType, String format){
		if(!testDBHandler()){
			return internalServerError("database conf file not found");
		}
		response().setHeader("Access-Control-Allow-Origin", "*");
		models.cmu.sv.sensor.SensorReading reading = dbHandler.searchReading(deviceId, timeStamp, sensorType);
		if(reading == null){
			return notFound("no reading found");
		}
		String ret = format.equals("json") ? reading.toJSONString() : reading.toCSVString(); 
		return ok(ret);
	}

	// search readings of time range [startTime, endTime]
	public static Result searchInTimeRange(String deviceId, Long startTime, long endTime, String sensorType, String format){
		if(!testDBHandler()){
			return internalServerError("database conf file not found");
		}
		response().setHeader("Access-Control-Allow-Origin", "*");
		ArrayList<models.cmu.sv.sensor.SensorReading> readings = dbHandler.searchReading(deviceId, startTime, endTime, sensorType);
		if(readings == null || readings.isEmpty()){
			return notFound("no reading found");
		}
		String ret = new String();
		if (format.equals("json"))
		{			
			for (models.cmu.sv.sensor.SensorReading reading : readings) {
				if (ret.isEmpty())
					ret += "[";
				else				
					ret += ',';			
				ret += reading.toJSONString();
			}
			ret += "]";			
		} else {
			for (models.cmu.sv.sensor.SensorReading reading : readings) {
				if (!ret.isEmpty())
					ret += '\n';
				ret += reading.toCSVString();
			}
		}

		return ok(ret);
	}

	public static Result lastReadingFromAllDevices(Long timeStamp, String sensorType, String format) {
		if(!testDBHandler()){
			return internalServerError("database conf file not found");
		}
		response().setHeader("Access-Control-Allow-Origin", "*");
		ArrayList<models.cmu.sv.sensor.SensorReading> readings = dbHandler.lastReadingFromAllDevices(timeStamp, sensorType);
		if(readings == null || readings.isEmpty()){
			return notFound("no reading found");
		}
		String ret = new String();
		if (format.equals("json"))
		{			
			for (models.cmu.sv.sensor.SensorReading reading : readings) {
				if (ret.isEmpty())
					ret += "[";
				else				
					ret += ',';			
				ret += reading.toJSONString();
			}
			ret += "]";			
		} else {
			for (models.cmu.sv.sensor.SensorReading reading : readings) {
				if (!ret.isEmpty())
					ret += '\n';
				ret += reading.toCSVString();
			}
		}
		return ok(ret);
	}
	
	public static Result lastestReadingFromAllDevices(String sensorType, String format) {
		if(!testDBHandler()){
			return internalServerError("database conf file not found");
		}
		response().setHeader("Access-Control-Allow-Origin", "*");
		ArrayList<models.cmu.sv.sensor.SensorReading> readings = dbHandler.lastestReadingFromAllDevices(sensorType);
		if(readings == null || readings.isEmpty()){
			return notFound("no reading found");
		}
		String ret = new String();
		if (format.equals("json"))
		{			
			for (models.cmu.sv.sensor.SensorReading reading : readings) {
				if (ret.isEmpty())
					ret += "[";
				else				
					ret += ',';			
				ret += reading.toJSONString();
			}
			ret += "]";			
		} else {
			for (models.cmu.sv.sensor.SensorReading reading : readings) {
				if (!ret.isEmpty())
					ret += '\n';
				ret += reading.toCSVString();
			}
		}
		return ok(ret);
	}	
		
}
