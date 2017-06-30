/**
 * Thrift files can namespace, package, or prefix their output in various
 * target languages.
 */
namespace java thriftClient
namespace py FinTech

service FinTech {
	string getPrediction(1:string input_str),
}
