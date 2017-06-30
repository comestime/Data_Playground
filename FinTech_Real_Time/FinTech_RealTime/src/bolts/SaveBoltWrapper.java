package bolts;

import java.util.Properties;

import org.apache.storm.hive.bolt.HiveBolt;
import org.apache.storm.hive.bolt.mapper.DelimitedRecordHiveMapper;
import org.apache.storm.hive.common.HiveOptions;

import backtype.storm.tuple.Fields;


public class SaveBoltWrapper {
	
	public static HiveBolt make(Properties topologyConfig) {
		String hive_db = topologyConfig.getProperty("hive_db");
		String hive_table = topologyConfig.getProperty("hive_table");
		String hive_metastore_URI = topologyConfig.getProperty("hive_metastore_URI");
		// hive table does not distinguish lower case/upper case
		String[] hive_table_col_name = {"member_id", "annual_inc", "funded_amnt", "data", "status"};
		
		// Record Writer configuration
        DelimitedRecordHiveMapper mapper = new DelimitedRecordHiveMapper()
                .withColumnFields(new Fields(hive_table_col_name));
                // .withPartitionFields(new Fields(partNames));
        
        HiveOptions hiveOptions;
        hiveOptions = new HiveOptions(hive_metastore_URI, hive_db, hive_table, mapper)
                .withTxnsPerBatch(2)
                .withBatchSize(10)
                .withIdleTimeout(10)
                .withCallTimeout(10000000);
                // .withKerberosKeytab(path_to_keytab)
                // .withKerberosPrincipal(krb_principal);
        
		return new HiveBolt(hiveOptions);
	}
}
