package bolts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import utils.Member;
import utils.Result;
import utils.ResultRecord;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;


public class SubmitBolt extends BaseRichBolt{
	private static final long serialVersionUID = 5151173513759399636L;
    private OutputCollector collector;
 
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector collector) {
        this.collector = collector;
    }

    public void execute(Tuple input) {
    	Member member = (Member)input.getValueByField("member");
        String result = null ;
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        
        try {
			ObjectMapper mapper = new ObjectMapper();
			// extract necessary fields from Member class for ML API submission
			Result resultDto = mapper.convertValue(member, Result.class);
			
			// get results from API
			String json = ow.writeValueAsString(resultDto);
			result = sendRequest(json);
			
			if (result != null) {
				// convert "result" JSON string to objects
				ResultRecord record = mapper.readValue(result, ResultRecord.class);
				Values values = new Values(Integer.parseInt(member.MemberId), Integer.parseInt(member.AnnualInc),
											Integer.parseInt(member.FundedAmnt), Float.parseFloat(record.Data),
											record.Status);
				collector.emit(values);
			}
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}      
    }
    
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    	// to use Hive bolt, each field should be declared individually
        declarer.declare(new Fields("member_id", "annual_inc", "funded_amnt", "data", "status"));
    }

    public String sendRequest(String input) {
    	try {

    		String url = "http://jupyter.datalaus.net:33334/api/v1.0/FinTech";
    		// String url = "http://0.0.0.0:5000/api/v1.0/bank/loandecision";
       	 	URL urlo = new URL(url);
    		HttpURLConnection con = (HttpURLConnection) urlo.openConnection();

    		// optional default is GET
    		con.setRequestMethod("POST");
    		con.setDoOutput(true);  
    		con.setRequestProperty("Content-Type", "application/json");
    		OutputStream os = con.getOutputStream();
            os.write(input.getBytes());
    	    
    	    BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
    	    String line;
    	    StringBuffer response = new StringBuffer();
    	    while ((line = rd.readLine()) != null) response.append(line);

    	    rd.close();
    	    return response.toString();
    	    //handle response here...
    	}catch (Exception ex) {
    	    //handle exception here
    		ex.printStackTrace();
    	}
    	return null;
    }
 
}
