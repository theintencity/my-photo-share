package
{
	import flash.events.DataEvent;
	import flash.events.ErrorEvent;
	import flash.events.Event;
	import flash.events.EventDispatcher;
	import flash.events.HTTPStatusEvent;
	import flash.events.IOErrorEvent;
	import flash.events.SecurityErrorEvent;
	import flash.events.TimerEvent;
	import flash.geom.Matrix;
	import flash.net.FileReference;
	import flash.net.URLLoader;
	import flash.net.URLLoaderDataFormat;
	import flash.net.URLRequest;
	import flash.net.URLVariables;
	import flash.utils.ByteArray;
	import flash.utils.Timer;
	
	import mx.controls.Alert;
	import mx.core.Application;
	import mx.events.FlexEvent;
	import mx.events.PropertyChangeEvent;
	import mx.events.PropertyChangeEventKind;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.events.ResultEvent;
	
	/**
	 * Dispatched when the data changes for this image or photo.
	 */
	[Event(name="dataChange", type="mx.events.FlexEvent")]
	
	/**
	 * This represents a single photo. It provides various photo properties like
	 * caption, author, id, etc. It also has methods to perform API calls on the photo
	 * e.g., rotateLeft().
	 */
	public class Photo extends EventDispatcher
	{
		public static const THUMB_WIDTH:uint = 200;
		public static const THUMB_HEIGHT:uint =200;
		public static const SIZE_LIMIT:uint = 1000000; // limit imposed by google apps engine.
		
		private var _data:ByteArray;
		private var _thumb:ByteArray;
		private var _file:FileReference;
		private var _caption:String;
		private var _matrix:Matrix;
		private var _id:String;
		private var _size:int;
		
		[Bindable]
		public var valid:Boolean = false;
		
		public var zoomCount:int = 0;
		
		[Bindable]
		public var dimension:String = null;
		
		[Bindable]
		public var author:String = "";
		
		public static function createFromXML(xml:XML):Photo 
		{
			trace("createFromXML " + xml);
			var photo:Photo = new Photo();
			photo._caption = String(xml.caption);
			photo._id = String(xml.id);
			try {
				photo._size = parseInt(String(xml.size));
			} catch (e:Error) { }
			if (xml.dimension != undefined) 
				photo.dimension = String(xml.dimension);
			if (xml.author != undefined)
				photo.author = String(xml.author);
			photo.loadThumb();
			return photo;
		}
		
		public static function createFromFile(file:FileReference):Photo
		{
			trace("createFromFile " + file.name + " " + file.size + " " + file.data);
			var photo:Photo = new Photo();
			photo._caption = file.name;
			photo._file = file;
			photo.author = PhotoApi.user;
			photo.uploadFile();
			return photo;
		}
		
		public function Photo()
		{
		}
		
		public function get id():String
		{
			return _id;
		}
		
		public function set data(value:ByteArray):void
		{
			_data = value;
		}
		public function get data():ByteArray
		{
			return _data;
		}
		public function set thumb(value:ByteArray):void
		{
			_thumb = value;
		}
		public function get thumb():ByteArray
		{
			return _thumb;
		}
		
		public function set file(value:FileReference):void
		{
			_file = value;
		}
		public function get file():FileReference
		{
			return _file;
		}
		
		[Bindable]
		public function set caption(value:String):void
		{
			var oldValue:String = _caption;
			_caption = value;
			if (value != oldValue) {
				var faultHandler:Function = function(event:FaultEvent):void
				{
					Alert.show("Error changing caption and tags", "Error");
					_caption = oldValue;
					dispatchEvent(new PropertyChangeEvent(PropertyChangeEvent.PROPERTY_CHANGE, false, false, PropertyChangeEventKind.UPDATE, "caption", value, oldValue));
				};
				PhotoApi.request("/api/update/" + _id + "?caption=" + value.split(" ").join("+"), null, faultHandler);
			}
		}
		public function get caption():String
		{
			return _caption;
		}
		
		public function set matrix(value:Matrix):void
		{
			_matrix = value;
		}
		public function get matrix():Matrix
		{
			return _matrix;
		}
		
		public function clone():Photo
		{
			var obj:Photo = new Photo();
			obj._data = _data;
			obj._caption = _caption;
			obj._thumb = _thumb;
			obj._file = _file;
			obj._matrix = _matrix;
			obj.valid = valid;
			obj.zoomCount = zoomCount;
			return obj;
		}
		
		public function download(force:Boolean = false):void
		{
			if (force || _data == null || valid == false)
				downloadData();
		}
		
		public function remove():void
		{
			if (author != PhotoApi.user) {
				Alert.show("You cannot permanently delete other people's photos: " + _caption, "Error");
				return;
			}
			
			var faultHandler:Function = function(event:FaultEvent):void
			{
				Alert.show("Error deleting file on server: " + caption, "Error");
			};
			
			PhotoApi.request("/api/delete/" + _id, null, faultHandler);
		}
		
		public function rotateLeft():void
		{
			if (_size >= SIZE_LIMIT) {
				Alert.show("Cannot modify image of size more than 1 MB", "Error");
				return;
			}
			
			var resultHandler:Function = function(event:ResultEvent):void {
				loadThumb();
				dispatchEvent(new Event("rotateLeft"));
			};
			
			PhotoApi.request("/api/update/" + _id + "?rotate=270", resultHandler);
		}
		
		public function rotateRight():void
		{
			if (_size >= SIZE_LIMIT) {
				Alert.show("Cannot modify image of size more than 1 MB", "Error");
				return;
			}
			
			var resultHandler:Function = function(event:ResultEvent):void {
				loadThumb();
				dispatchEvent(new Event("rotateRight"));
			};
			
			PhotoApi.request("/api/update/" + _id + "?rotate=90", resultHandler);
		}
		
		public function flipHorizontal():void
		{
			if (_size >= SIZE_LIMIT) {
				Alert.show("Cannot modify image of size more than 1 MB", "Error");
				return;
			}
			
			var resultHandler:Function = function(event:ResultEvent):void {
				loadThumb();
				dispatchEvent(new Event("flipHorizontal"));
			};
			PhotoApi.request("/api/update/" + _id + "?flip=horizontal", resultHandler);
		}
		
		public function flipVertical():void
		{
			if (_size >= SIZE_LIMIT) {
				Alert.show("Cannot modify image of size more than 1 MB", "Error");
				return;
			}
			
			var resultHandler:Function = function(event:ResultEvent):void {
				loadThumb();
				dispatchEvent(new Event("flipVertical"));
			};
			PhotoApi.request("/api/update/" + _id + "?flip=vertical", resultHandler);
		}
		
		public function resize(dimension:String):void
		{
			var oldValue:String = this.dimension;
			this.dimension = dimension;
			
			var faultHandler:Function = function(event:FaultEvent):void {
				Alert.show("Cannot resize the image", "Error");
				this.dimension = oldValue;
			};
			
			PhotoApi.request("/api/update/" + _id + "?resize=" + dimension, null, faultHandler);
		}
		
		private var _downloadFile:FileReference;
		
		public function downloadSave():void 
		{
			if (_data != null && _data.length > 0) {
				var fileName:String = id;
				if (fileName.indexOf("_") > 0)
					fileName = fileName.substr(fileName.indexOf("_")+1);
				_downloadFile = new FileReference();
				_downloadFile.save(_data, fileName);
			}
		}
		
		//////
		
		
		private function uploadFile():void
		{
			if (_id == null) {
				_id = String(int(Math.random()*10000000).toString() + "_" + file.name.replace(/[^a-zA-Z0-9\\._]+/g, "_")).toLowerCase();
			}
			
			_size = file.size;
			
			// need blobservice to store larger files.
			PhotoApi.request("/api/upload.jsp?photo=" + _id + "&size=" + _size, uploadResultHandler, uploadFaultHandler);
		}
		
		private var _uploadTimer:Timer;
		
		private function uploadResultHandler(event:ResultEvent):void
		{
			file.addEventListener(Event.COMPLETE, uploadCompleteHandler);
			file.addEventListener(IOErrorEvent.IO_ERROR, uploadCompleteHandler);
			file.addEventListener(SecurityErrorEvent.SECURITY_ERROR, uploadCompleteHandler);
			
			/**
			 * For some reason the complete event is not fired on production but works on
			 * local host, so I am setting a 5 second time to assume completion.
			 */
			_uploadTimer = new Timer(5000, 1);
			_uploadTimer.addEventListener(TimerEvent.TIMER, uploadTimerHandler, false, 0, true);
			_uploadTimer.start();
			
			trace("found " + event.result.url + " creating timer");
			var url:URLRequest = new URLRequest(PhotoApi.baseURL + String(event.result.url));
			file.upload(url);
		}
		
		private function uploadFaultHandler(event:FaultEvent):void
		{
			trace("/api/upload.jsp fault: " + event.fault + " " + event.message);
			Alert.show("fault handler");
		}
		
		private function uploadTimerHandler(event:TimerEvent):void
		{
			trace("/api/uploadfile timesout. Implicitly calling completion");
			uploadCompleteHandler(new Event(Event.COMPLETE));
		}
		
		private function uploadCompleteHandler(event:Event):void
		{
			trace("uploadCompleteHandler " + event.type);
			if (_uploadTimer != null) {
				_uploadTimer.stop();
				_uploadTimer = null;
			}
			
			file.removeEventListener(Event.COMPLETE, uploadCompleteHandler);
			file.removeEventListener(IOErrorEvent.IO_ERROR, uploadCompleteHandler);
			file.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, uploadCompleteHandler);
			
			var success:Boolean = (event.type == Event.COMPLETE);
			if (event.type == IOErrorEvent.IO_ERROR && String(ErrorEvent(event).text).indexOf("#2038") >= 0) {
				/**
				 * For some unknown reason Flash Player sends #2038 with ioError even if the data is
				 * successfully uploaded. Treat this as success.
				 */
				trace("treating #2038 as success");
				success = true;
			}
			
			if (success) {
				
				if (_caption != null)
					PhotoApi.request("/api/update/" + _id + "?caption=" + caption); 
				
				var timeout:Function = function(event:Event):void {
					loadThumb();
				}
				var timer:Timer = new Timer(1000, 1);
				timer.addEventListener("timer", timeout);
				timer.start();
			}
			else {
				Alert.show("Error uploading data " + event.type + " " + ErrorEvent(event).text, "Error");
			}
		}
		
		private function loadThumb():void 
		{
			var loader:URLLoader = new URLLoader();
			loader.addEventListener(Event.COMPLETE, thumbCompleteHandler);
			loader.addEventListener(IOErrorEvent.IO_ERROR, thumbCompleteHandler);
			loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, thumbCompleteHandler);
			loader.dataFormat = URLLoaderDataFormat.BINARY;
			loader.load(new URLRequest(PhotoApi.baseURL + "/api/thumb/" + _id));
		}
		
		private function thumbCompleteHandler(event:Event):void
		{
			trace("thumbCompleteHandler " + event.type);
			var loader:URLLoader = URLLoader(event.currentTarget);
			loader.removeEventListener(Event.COMPLETE, thumbCompleteHandler);
			loader.removeEventListener(IOErrorEvent.IO_ERROR, thumbCompleteHandler);
			loader.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, thumbCompleteHandler);
			
			var success:Boolean = (event.type == Event.COMPLETE);
			
			if (success) {
				_thumb = loader.data as ByteArray;
				trace("thumb length is " + (_thumb != null ? _thumb.length : -1));
				valid = true;
				dispatchEvent(new FlexEvent(FlexEvent.DATA_CHANGE));
			}
		}
		
		private function downloadData():void 
		{
			var loader:URLLoader = new URLLoader();
			loader.addEventListener(Event.COMPLETE, downloadCompleteHandler);
			loader.addEventListener(IOErrorEvent.IO_ERROR, downloadCompleteHandler);
			loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, downloadCompleteHandler);
			loader.dataFormat = URLLoaderDataFormat.BINARY;
			loader.load(new URLRequest(PhotoApi.baseURL + "/api/download/" + _id));
		}
		
		private function downloadCompleteHandler(event:Event):void
		{
			trace("downloadCompleteHandler " + event.type);
			var loader:URLLoader = URLLoader(event.currentTarget);
			loader.removeEventListener(Event.COMPLETE, downloadCompleteHandler);
			loader.removeEventListener(IOErrorEvent.IO_ERROR, downloadCompleteHandler);
			loader.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, downloadCompleteHandler);
			
			var success:Boolean = (event.type == Event.COMPLETE);
			
			if (success) {
				_data = loader.data as ByteArray;
				trace("data length is " + (_data != null ? _data.length : -1));
				valid = true;
				dispatchEvent(new FlexEvent(FlexEvent.DATA_CHANGE));
			}
		}

	}
}