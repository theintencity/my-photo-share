package
{
	import flash.display.DisplayObject;
	import flash.events.ErrorEvent;
	import flash.events.Event;
	import flash.events.EventDispatcher;
	import flash.events.IOErrorEvent;
	import flash.events.SecurityErrorEvent;
	import flash.net.FileFilter;
	import flash.net.FileReference;
	import flash.net.FileReferenceList;
	import flash.net.URLRequest;
	
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	import mx.core.Application;
	import mx.events.FlexEvent;
	import mx.managers.PopUpManager;
	import mx.rpc.Fault;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.events.ResultEvent;

	/**
	 * This is the main data model for the application and stores a list of Photo objects.
	 * It has methods such as rotateLeft() which get propagated to the individual
	 * selected Photo objects.
	 */
	public class Photos extends EventDispatcher
	{
		private static var imageFilter:FileFilter = new FileFilter("Images (*.jpg, *.jpeg, *.gif, *.png)", "*.jpg;*.jpeg;*.gif;*.png");
		
		private var _saved:ArrayCollection = null;
		private var _items:ArrayCollection = new ArrayCollection();
		private var _selectedItems:Array = null;
		private var _selectedItem:Photo;
		private var _remaining:int = 0;
		
		private var files:FileReferenceList;
		private var file:FileReference;
		
		[Bindable]
		public var selected:Boolean = false;
		
		[Bindable]
		public var status:String = "";
		
		[Bindable]
		public var tileView:Boolean = true;
		
		public function Photos()
		{
		}
		
		public function get items():ArrayCollection
		{
			return _items;
		}
		
		[Bindable]
		public function set selectedItem(value:Photo):void
		{
			_selectedItem = value;
			if (value != null)
				selectedItems = [value];
			else
				selectedItems = [];
		}
		public function get selectedItem():Photo
		{
			return _selectedItem;
		}
		
		public function set selectedItems(value:Array):void
		{
			_selectedItems = value;
			selected = (value != null && value.length > 0);
		}
		public function get selectedItems():Array 
		{
			return _selectedItems;
		}
		
		public function load():void
		{
			if (_items.length > 0)
				_items.removeAll();
			status = "Loading your photos...";
			PhotoApi.request("/api/photos.jsp", loadResultHandler, loadFaultHandler);
		}
		
		public function upload():void
		{
			files = new FileReferenceList();
			files.addEventListener(Event.SELECT, uploadSelectHandler);
			files.addEventListener(Event.CANCEL, uploadCancelHandler);
			files.browse([imageFilter]);
		}
		
		public function share():void
		{
			var popup:SharePrompt = PopUpManager.createPopUp(Application.application as DisplayObject, SharePrompt, true) as SharePrompt;
			popup.photos = this;
			PopUpManager.centerPopUp(popup);
		}
		
		public function privacy():void
		{
			var popup:PrivacyPrompt = PopUpManager.createPopUp(Application.application as DisplayObject, PrivacyPrompt, true) as PrivacyPrompt;
			popup.photos = this;
			PopUpManager.centerPopUp(popup);
		}
		
		public function remove():void
		{
			var removeSelected:Boolean = false;
			if (_selectedItems != null) {
				for each (var item:Photo in _selectedItems) {
					var index:int = _items.getItemIndex(item);
					if (index >= 0) {
						_items.removeItemAt(index);
						item.remove();
						if (item == selectedItem) {
							removeSelected = true;
						}
					}
				}
			}
			selectedItems = [];
			if (removeSelected) {
				var nextItem:Photo = (_items.length > 0 ? _items.getItemAt(0) as Photo : null);
				selectedItem = nextItem; 
			}
		}
		
		public function resize(dimension:String):void
		{
			if (selectedItems != null) {
				for each (var item:Photo in selectedItems) {
					item.resize(dimension);
				}
			}
		}
		
		public function rotateLeft():void
		{
			if (selectedItems != null) {
				for each (var item:Photo in selectedItems) {
					item.rotateLeft();
				}
			}
		}
		
		public function rotateRight():void
		{
			if (selectedItems != null) {
				for each (var item:Photo in selectedItems) {
					item.rotateRight();
				}
			}
		}
		
		public function flipHorizontal():void
		{
			if (selectedItems != null) {
				for each (var item:Photo in selectedItems) {
					item.flipHorizontal();
				}
			}
		}
		
		public function flipVertical():void
		{
			if (selectedItems != null) {
				for each (var item:Photo in selectedItems) {
					item.flipVertical();
				}
			}
		}
		
		private function loadFaultHandler(event:FaultEvent):void
		{
			trace("loadFaultHandler " + event.message);
			status = "";
			Alert.show("Error loading your photos", "Error");
		}
		
		private function loadResultHandler(event:ResultEvent):void
		{
			trace("loadResultHandler " + event.result);
			status = "";
			for each (var child:XML in event.result.children()) {
				var found:Boolean = false;
				for each (var item:Photo in _items) {
					if (item.id == String(child.id)) {
						found = true;
					}
				}
				if (!found) {
					_items.addItem(Photo.createFromXML(child));
				}
			}
		}
		
		private function uploadCancelHandler(event:Event):void
		{
			files = null;
		}
		
		private function uploadSelectHandler(event:Event):void
		{
			trace("uploadSelectHandler");
			_remaining = files.fileList.length;
			status = "loading " + _remaining + " items";
			var largerFile:Array = [];
			for each (var file:FileReference in files.fileList) {
				if (file.size >= Photo.SIZE_LIMIT) {
					largerFile.push(file.name);
					_remaining--;
				}
				else {
					var photo:Photo = Photo.createFromFile(file);
					photo.addEventListener(FlexEvent.DATA_CHANGE, photoDataChangeHandler);
					_items.addItem(photo);
				}
			}
			files = null;
			if (largerFile.length > 0)
				Alert.show("Only files less than 1 MB are allowed. Following files not uploaded: " + largerFile.join(", "), "Error");
			if (_remaining > 0)
				status = "loading " + _remaining + " items";
			else
				status = "";
		}
		
		private function photoDataChangeHandler(event:FlexEvent):void
		{
			IEventDispatcher(event.currentTarget).removeEventListener(FlexEvent.DATA_CHANGE, photoDataChangeHandler);
			if (_remaining > 0)
				_remaining--;
			if (_remaining > 0)
				status = "loading " + _remaining + " items";
			else
				status = "";
		}
	}
}