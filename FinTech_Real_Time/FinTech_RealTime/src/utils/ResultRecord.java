package utils;

import org.codehaus.jackson.annotate.JsonProperty;

// ResultRecord fields are field by results returned by ML API
public class ResultRecord {
	@JsonProperty("data")
	public String Data;
	@JsonProperty("status")
	public String Status;
}
