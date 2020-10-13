package es.uma.main;

import com.google.gson.Gson;
import es.uma.algorithms.*;
import es.uma.auxiliar.CSVBuilder;
import es.uma.models.*;
import org.bson.Document;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;

public class mainLoop {

	public static void main(String [] argv){

		/// TEST: Values for testing
		/*        queueCommunication queue = new queueCommunication(
                "invitado", // System.getenv("UM_QUEUE_USER"),
                "invitado", //System.getenv("UM_QUEUE_PASS"),
                "127.0.0.1", //System.getenv("UM_QUEUE_HOST"),
                "/", //System.getenv("UM_QUEUE_VHOST"),
                5672 //Integer.parseInt(System.getenv("UM_QUEUE_PORT"))
        );*/

		queueCommunication queue = new queueCommunication(
				System.getenv("UM_QUEUE_USER"),
				System.getenv("UM_QUEUE_PASS"),
				System.getenv("UM_QUEUE_HOST"),
				System.getenv("UM_QUEUE_VHOST"),
				Integer.parseInt(System.getenv("UM_QUEUE_PORT")));

		if (queue == null) {
			
			System.err.println("Cannot connect to RabbitMQ queue");
			return;
		}

		while(true){
			
			IncomingMessage message = queue.getMessage();
			
			if(message != null) {
				
				String result = manageMessage(message.message);
				queue.sendMessage(result, message.deliveryTag);
				
			} else {
				
				System.err.println("Receiving null message from queue.");
			}
		}
	}

	private static String manageMessage(String message) {
		// Convert message to object
		Gson gson = new Gson();
		PredictionRequestEvent requestEvent = gson.fromJson(message, PredictionRequestEvent.class);
		if(requestEvent == null){
			System.err.println("request event format is not valid");
			return null;
		}
		// Connect to database with event and get data
		ArrayList<ArrayList<Document>> data = connectDB(requestEvent);
		if(data == null || data.isEmpty()){
			System.out.println("Invalid datasources or no datasources in request event");
			return null;
		}
		// Calculate result
		ArrayList<Document> result;
		/// Real Call
		// generate CSV
		//CSVBuilder csv = new CSVBuilder("temporal", "test.csv", data);
		//csv.writeFile();
		// Call algorithm
		//ArrayList<ArrayList<Integer>> resAlg = executeAlgorithm(requestEvent, "temporal", "test.csv", data.size());
		// Generate Results
		//result = analyzeResultsAlgorithm(resAlg, data, requestEvent);

		/// fake call
		result = analyzeResults(data, requestEvent);
		if(result == null || result.isEmpty()){
			System.out.println("Invalid data in database");
			return null;
		}

		// Write Database
		writeDatabase(requestEvent.output, result);
		// Create event
		String res = createExecutedEvent(requestEvent);
		return res;
	}

	private static ArrayList<ArrayList<Integer>> executeAlgorithm(PredictionRequestEvent requestEvent, String dp,
			String fn, Integer inputs) {
		AlgorithmConfiguration ac = new AlgorithmConfiguration();
		ac.setDatapath(dp);
		ac.setDatafile(fn);
		ac.setLayers(new int[]{inputs, inputs*8, inputs * 4, inputs * 2, inputs});
		if(requestEvent.model.horizon != null){
			if(requestEvent.model.horizon == "MEDIO") ac.setPrediction(AlgorithmConfiguration.Prediction.MEDIUM);
			else if(requestEvent.model.horizon == "LARGO") ac.setPrediction(AlgorithmConfiguration.Prediction.LARGE);
		}

		AbstractAlgorithm algorithm = null;

		if(requestEvent.model.type == null){
			algorithm = new PSO_Multi_Par(ac);
		} else {
			if (requestEvent.model.type.equals("modelA")) {
				algorithm = new PSO_Multi_Par(ac);
			} else if (requestEvent.model.type.equals("modelB")) {
				algorithm = new PSO_Multi_Seq(ac);
			} else if (requestEvent.model.type.equals("modelC")) {
				algorithm = new PSO_Mono_Par(ac);
			} else if (requestEvent.model.type.equals("modelD")) {
				algorithm = new PSO_Mono_Seq(ac);
			} else if (requestEvent.model.type.equals("modelE")) {
				algorithm = new cGA_Multi_Par(ac);
			} else if (requestEvent.model.type.equals("modelF")) {
				algorithm = new cGA_Multi_Par(ac);
			} else if (requestEvent.model.type.equals("modelG")) {
				algorithm = new cGA_Mono_Par(ac);
			} else if (requestEvent.model.type.equals("modelH")) {
				algorithm = new cGA_Mono_Seq(ac);
			} else if (requestEvent.model.type.equals("modelI")) {
				algorithm = new ACO_Multi_Par(ac);
			} else if (requestEvent.model.type.equals("modelJ")) {
				algorithm = new ACO_Multi_Par(ac);
			} else if (requestEvent.model.type.equals("modelK")) {
				algorithm = new ACO_Mono_Par(ac);
			} else if (requestEvent.model.type.equals("modelL")) {
				algorithm = new ACO_Mono_Seq(ac);
			}
		}

		algorithm.run();

		return algorithm.getPrediction();
	}

	private static String createExecutedEvent(PredictionRequestEvent r) {
		PredictionExecutionEvent p = new PredictionExecutionEvent();
		p.id = r.id;
		p.timestamp = "" + Instant.now().getEpochSecond();
		p.name = "PredictionExecutedEvent";
		p.version = "1";
		p.ack = true;
		p.etlId = r.executionId;
		p.executionId = r.executionId;
		p.userId = r.executionId;
		Gson gson = new Gson();
		String json = gson.toJson(p);
		return json;
	}

	private static void writeDatabase(Output output, ArrayList<Document> result) {
		if(output == null || output.source == null ){
			System.out.println("No output database");
			return;
		}
		if(output.source.collection != null && output.source.host != null && output.source.name != null && output.source.port != null) {
			//  mirar si hay user y pass
			//  Llamar al constructor apropiado
			databaseAccess db = null;
			if(output.source.username == null || output.source.password == null) {
				db = new databaseAccess(output.source.host, output.source.port, output.source.name);
			} else {
				db = new databaseAccess(output.source.host, output.source.port, output.source.name, output.source.username, output.source.password);
			}
			if(db == null){
				System.err.println("Incorrent Datasource");
			} else {
				db.setData(output.source.collection, result);
			}
		}
	}

	private static ArrayList<Document> analyzeResultsAlgorithm(ArrayList<ArrayList<Integer>> res,
			ArrayList<ArrayList<Document>> data,
			PredictionRequestEvent requestEvent) {
		Instant date = null, max_date = null;
		ArrayList<Document> result = new ArrayList<>();
		for(ArrayList<Document> ad: data){
			for(Document d: ad){
				String s = d.get("TimeInstant", String.class);
				TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(s);
				date = Instant.from(ta);
				if(max_date == null){
					max_date = date;
				} else if(date.compareTo(max_date) > 0){
					max_date = date;
				}
			}
		}
		for(ArrayList<Integer> ai: res){
			Integer d = 0;
			Integer o = 0;
			max_date = max_date.plusSeconds(240);
			for(Integer i: ai){
				Document doc = new Document();
				doc.put("Datasource", requestEvent.origins.get(o).datasources.get(d).source.collection);
				doc.put("TimeInstant",DateTimeFormatter.ISO_INSTANT.format(max_date));
				doc.put("per", i);
				result.add(doc);
				d++;
				if(d >= requestEvent.origins.get(o).datasources.size()){
					d = 0;
					o++;
				}
			}
		}

		return result;
	}

	private static ArrayList<Document> analyzeResults(ArrayList<ArrayList<Document>> data,
			PredictionRequestEvent requestEvent) {
		Instant date = null, max_date = null;
		ArrayList<Document> result = new ArrayList<>();
		for(ArrayList<Document> ad: data){
			for(Document d: ad){
				String s = d.get("TimeInstant", String.class);
				TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(s);
				date = Instant.from(ta);
				if(max_date == null){
					max_date = date;
				} else if(date.compareTo(max_date) > 0){
					max_date = date;
				}
			}
		}
		Integer d = 0;
		Integer o = 0;
		for(ArrayList<Document> ad: data){
			Instant max_date1 = max_date;
			for(Document doc: ad) {
				Document doc1 = new Document();
				doc.put("Datasource", requestEvent.origins.get(o).datasources.get(d).source.collection);
				max_date1 = max_date.plusSeconds(240);
				doc.put("TimeInstant",DateTimeFormatter.ISO_INSTANT.format(max_date1));
				try {
					Integer i = d.getInteger("per");
					i += 2;
					doc1.put("per", i);
				} catch(Exception e){
					doc1.put("per", 0);
				}
				result.add(doc1);
			}
			d++;
			if(d >= requestEvent.origins.get(o).datasources.size()){
				d = 0;
				o++;
			}

		}

		return result;
	}

	private static ArrayList<ArrayList<Document>> connectDB(PredictionRequestEvent requestEvent) {
		ArrayList<ArrayList<Document>> data = new ArrayList<>();
		// Comprobar atributos
		if(requestEvent.origins == null){
			return data;
		}
		for(Origins o: requestEvent.origins){
			if(o.datasources != null){
				for(DataSource s: o.datasources){
					if(s.source != null){
						if(s.source.collection != null && s.source.host != null && s.source.name != null && s.source.port != null && s.source.collection !=  null) {
							//  mirar si hay user y pass
							//  Llamar al constructor apropiado
							databaseAccess db = null;
							if(s.source.username == null || s.source.password == null) {
								db = new databaseAccess(s.source.host, s.source.port, s.source.name);
							} else {
								db = new databaseAccess(s.source.host, s.source.port, s.source.name, s.source.username, s.source.password);
							}
							if(db == null){
								System.err.println("Incorrent Datasource");
							} else {
								data.add(db.getData(s.source.collection, null, null));
							}
						}
					}
				}
			}
		}
		return data;
	}
}
