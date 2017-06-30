package bolts;

import java.io.IOException;
import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.apache.log4j.Logger;

import thriftClient.FinTech;
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


public class DecisionBolt extends BaseRichBolt{
	private static final long serialVersionUID = 5151173513759399636L;
	private static final Logger LOG = Logger.getLogger(DecisionBolt.class);
    private OutputCollector collector;
    
    // Thrift Client
    private TTransport transport;
    private TProtocol protocol;
    private FinTech.Client client;
 
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector collector) {
        this.collector = collector;
        try {
        	this.transport = new TSocket("127.0.0.1", 8989);
        	this.protocol = new TBinaryProtocol(transport);
        	this.client = new FinTech.Client(protocol);
        	transport.open();
        } catch (TTransportException e) {
            e.printStackTrace();
        } catch (TException e) {
          e.printStackTrace();
        }
    }
    
    public void execute(Tuple input) {
    	Member member = (Member)input.getValueByField("member");
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();

        try {
        	ObjectMapper mapper = new ObjectMapper();
        	// extract necessary fields from Member class for ML API submission
        	Result resultDto = mapper.convertValue(member, Result.class);
		
        	String json = ow.writeValueAsString(resultDto);
        	// get results from local ML model by using Thrift services
			try {
				String result = client.getPrediction(json);
				// System.out.println("[DEBUG] [ML result back]: " + result);
			
				ResultRecord resultRecord = mapper.readValue(result.toString(), ResultRecord.class);
			
				Values values = new Values(Integer.parseInt(member.MemberId), Integer.parseInt(member.AnnualInc),
										Integer.parseInt(member.FundedAmnt), Float.parseFloat(resultRecord.Data), 
										resultRecord.Status);
				collector.emit(values);		
			} catch (TTransportException e) {
				e.printStackTrace();
			} catch (TException e) {
				e.printStackTrace();
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
        declarer.declare(new Fields("member_id", "annual_inc", "funded_amnt", "data", "status"));
    }
    
    public void cleanUp() {
    	try {
    		transport.close();
		} catch (Exception e) {
			LOG.error("Error closing connections", e);
		}
    }
}

