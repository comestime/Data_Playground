package topology;

import java.io.Serializable;
import java.util.Properties;
import java.util.UUID;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.utils.Utils;
import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.StringScheme;
import storm.kafka.ZkHosts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bolts.DecisionBolt;
import bolts.FilterBolt;
import bolts.SaveBoltWrapper;
import bolts.SubmitBolt;
import spouts.KafkaReadApiProducer;

public class Topology implements Serializable {
	private static final Logger LOGGER = LoggerFactory.getLogger(Topology.class);
	static final String TOPOLOGY_NAME = "storm-kafka-fintech";
	
	public static final void main(final String[] args) {
		try {
			String configFileLocation = "config.properties";
			Properties topologyConfig = new Properties();
		    topologyConfig.load(ClassLoader.getSystemResourceAsStream(configFileLocation));
			
		    String kafkaserver = topologyConfig.getProperty("kafkaserver");
		    String zkConnString = topologyConfig.getProperty("zookeeper");
		    String topicName = topologyConfig.getProperty("topic");
			
			//kafka file producer 
		    KafkaReadApiProducer fileProducer = new KafkaReadApiProducer(topicName, false);
			fileProducer.start();			
			
			final Config config = new Config();
			config.setMessageTimeoutSecs(20);
			TopologyBuilder topologyBuilder = new TopologyBuilder();
		   
			BrokerHosts hosts = new ZkHosts(zkConnString);
			SpoutConfig spoutConfig = new SpoutConfig(hosts, topicName, "/" + topicName, UUID.randomUUID().toString());
			spoutConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
			KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);

			topologyBuilder.setSpout("ApiSpout", kafkaSpout, 1);
			topologyBuilder.setBolt("FilterBolt", new FilterBolt()).shuffleGrouping("ApiSpout");
			// use remote ML API
			// topologyBuilder.setBolt("SubmitBolt", new SubmitBolt()).shuffleGrouping("FilterBolt");
			// topologyBuilder.setBolt("SaveBolt", SaveBoltWrapper.make(topologyConfig)).shuffleGrouping("SubmitBolt");
			
			// use local ML model instead
			topologyBuilder.setBolt("DecisionBolt", new DecisionBolt()).shuffleGrouping("FilterBolt");
			topologyBuilder.setBolt("SaveBolt", SaveBoltWrapper.make(topologyConfig)).shuffleGrouping("DecisionBolt");	
			
			//Submit it to the cluster or  locally
			if (null != args && 0 < args.length) {
				config.setNumWorkers(3);
				StormSubmitter.submitTopology(args[0], config, topologyBuilder.createTopology());
			} else {
				config.setMaxTaskParallelism(10);
				final LocalCluster localCluster = new LocalCluster();
				localCluster.submitTopology(TOPOLOGY_NAME, config, topologyBuilder.createTopology());

				Utils.sleep(360 * 10000);

				LOGGER.info("Shutting down the cluster");
				//localCluster.killTopology(TOPOLOGY_NAME);
				//localCluster.shutdown();
			}
		} catch (final InvalidTopologyException exception) {
			exception.printStackTrace();
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
	}
}