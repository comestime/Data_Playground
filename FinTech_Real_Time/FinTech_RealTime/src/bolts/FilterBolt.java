package bolts;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import utils.Member;

public class FilterBolt extends BaseRichBolt {
	private static final long serialVersionUID = 5151173513759399636L;
    private OutputCollector collector;
 
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector collector) {
        this.collector = collector;
    }
  
    public void execute(Tuple input) {
        //data cleaning 
        String applicantS = input.getString(0).toString();
        ObjectMapper mapper = new ObjectMapper();
		Member member;
		
		try {
			// extract strings to member fields
			System.out.println("[DEBUG] [Received Msg in FilterBolt] " + applicantS.toString());
			member = mapper.readValue(applicantS.toString(), Member.class);
			member.Rate = member.Rate.replace("%", "").trim();
			member.Term = member.Term.replace(" months","").trim();
			member.Dti="0";
			if(member.AnnualInc.contains("RENT"))
			{
				member.AnnualInc="0";
			}
			collector.emit(new Values(member));
		} catch (JsonParseException e) {
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
        declarer.declare(new Fields("member"));
    }
}

