package
{
	import mx.core.Application;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.events.ResultEvent;
	import mx.rpc.http.HTTPService;
	import mx.utils.URLUtil;
	
	/**
	 * This is a helper class to invoke the API calls using Flex HTTPService object.
	 */
	public class PhotoApi
	{
		private static var _baseURL:String;
		
		[Bindable]
		public static var user:String;
		
		public function PhotoApi()
		{
		}

		public static function get baseURL():String
		{
			if (_baseURL == null) {
				var u:String = Application.application.url;
				_baseURL = URLUtil.getProtocol(u) + "://" + URLUtil.getServerNameWithPort(u);
			}
			return _baseURL;
		}
		
		public static function request(url:String, resultHandler:Function=null, faultHandler:Function=null, param:Object = null, timeout:uint=10000):HTTPService
		{
			var http:HTTPService = new HTTPService();
			http.url = baseURL + url;
			http.resultFormat = HTTPService.RESULT_FORMAT_E4X;
			http.requestTimeout = timeout;
			if (resultHandler != null)
				http.addEventListener(ResultEvent.RESULT, resultHandler);
			if (faultHandler != null)
				http.addEventListener(FaultEvent.FAULT, faultHandler);
			http.send(param);
			return http;
		}
	}
}