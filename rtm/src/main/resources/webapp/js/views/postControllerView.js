var PostControllerView = Backbone.View.extend(
		{
			el: '.PostControllerView',
			events: {
				"click #addSelector" : "addSelector",
				"click #clearAll" : "clearAll",
				"click .at" : "addTextFilter",
				"click .an" : "addNumFilter",
				"click .ad" : "addDateFilter",
				"click .rs" : "remSelector",
				"click .rtf" : "remFilter",
				"click .rnf" : "remFilter",
				"click .rdf" : "remFilter",
				"change .sinp": "refreshController",
				"click #sendSearch" : "sendSearch",
				"click #pause" : "pauseEvent",
				"click #resume" : "resumeEvent",
				"keypress" : "hijackEnter",
				"click .defaultKey" : "setDefaultKey"
			},

//			Configurative, will be retrieved from a server configuration service in the next major update
			defaultTextKeys : ['eId', 'name', 'uId', 'profileId', 'eDesc', 'rnId', 'rnStatus'],
			defaultNumericalKeys : ['value'],
			defaultDateKeys : ['begin'],

			spinner : '',

			getGuiDomain: function(){
				return 'postControllerView';
			},

			getServiceDomain: function(){
				return 'default';
			},

			guiSelectors : [],

			hijackEnter : function(e){
				if(e.keyCode === 13){
					this.trigger('globalSearchEvent');
					e.preventDefault();
				}
			},

			initialize : function(){
			},

			hasData : function () {
				return (this.guiSelectors.length > 0);
			},

			setDefaultKey :function (e) {
				var splitArray = e.currentTarget.id.split("_");
				var selId = splitArray[0];
				var filId = splitArray[1];
				var value = splitArray[2];
				this.guiSelectors[selId].getFilter(filId).setValueGeneric('key',value);
				this.render();
				e.preventDefault();
			},

			addTextFilter: function (e) {
				this.guiSelectors[e.currentTarget.id].pushFilter(new TextFilter());
				this.render();
				e.preventDefault();
			},
			addNumFilter: function (e) {
				this.guiSelectors[e.currentTarget.id].pushFilter(new NumericalFilter());
				this.render();
				e.preventDefault();
			},
			addDateFilter: function (e) {
				this.guiSelectors[e.currentTarget.id].pushFilter(new DateFilter());
				this.render();
				e.preventDefault();
			},
			remSelector: function (e) {
				this.guiSelectors.splice(e.currentTarget.id,1);
				this.render();
				e.preventDefault();
			},

			render: function () {

				var that = this;
				jQuery.get(resolveTemplate('postController-template'), function (data) {
					template = _.template(data, {
						controller: {text : 'Add Selector'},
						selectors : that.guiSelectors,
						defaultTextKeys : that.defaultTextKeys,
						defaultNumericalKeys : that.defaultNumericalKeys,
						defaultDateKeys : that.defaultDateKeys
					});
					that.$el.html(template);  
				}, 'html')
				.success(function(){
					$('.form_datetime').datetimepicker({
						language:  'fr',
						weekStart: 1,
						todayBtn:  1,
						autoclose: 1,
						todayHighlight: 1,
						startView: 2,
						forceParse: 0,
						showMeridian: 1
					});
					
					that.resetSpinner();
				}) // success
				.fail(function(model, response, options ) {
					displayError('response=' + JSON.stringify(response));
				});
			},

			resetSpinner : function(){
				var opts = {
						top: 0, left: 0, lines: 13, length: 35, width: 15, radius: 18, scale: 0.2, corners: 1, color: 'gray',
						opacity: 0.20, rotate: 0, direction: 1, speed: 0.8, trail: 80, fps: 25, zIndex: 2e9,
						className: 'spinner', shadow: false, hwaccel: false, position: 'relative'
				};
				this.spinner = new Spinner(opts).spin();
				//this.spinner.stop();
				var target = $('#spinner');
				if(target.length > 0){
					target.append(this.spinner.el);
				}
			},
			stopSpinner : function(){
				this.spinner.stop();
				$('#spinner').empty();
			},
			addSelector : function(event){

				this.guiSelectors.push(new GuiSelector());
				this.render();
				event.preventDefault();
			},

			clearAll: function(event){
				this.guiSelectors = [];
				this.render();
				event.preventDefault();
			},

			getGuiFragment: function(event){
				//console.log('postControllerView-getSelectorFragment ---> this.guiSelectors =');
				//console.log(this.guiSelectors);
				return this.guiSelectors;
			},
			getServiceFragment: function(event){
				//console.log('postControllerView-getSelectorFragment ---> this.guiSelectors =');
				//console.log(this.guiSelectors);
				return this.guiSelectors;
			},

			remFilter : function(e){
				var id = e.currentTarget.id.split("_");
				var selId = id[0];
				var filterId = id[1];
				this.guiSelectors[selId].popFilter(filterId,1);
				this.render();
				e.preventDefault();
			},

			hasValidFilters: function(){
				var result = false;
				var l = this.guiSelectors.length;
				if(l < 1)
					return false;
				for(i = 0; i < l; i++){
					var thisSelector = this.guiSelectors[i];
					var l2 = thisSelector.getFilters().length;
					if(l2 < 1)
						continue;
					for(j=0; j < l2; j++){
						var thisFilter = thisSelector.getFilter(j);
						if(thisFilter.isEmpty())
							return false;
					}
				}
				return true;
			},

			sendSearch: function(e){
				this.spinner.spin();
				this.trigger('globalSearchEvent');
				e.preventDefault();
			},
			pauseEvent: function(e){
				this.trigger('pauseEvent');
				e.preventDefault();
			},
			resumeEvent: function(e){
				this.trigger('resumeEvent');
				e.preventDefault();
			},
			refreshController: function(e){

				var sel = this.guiSelectors;

				if(e.currentTarget.className.indexOf("treg") > -1){
					$(".treg").each(function(idx, itemZ){
						if(itemZ.id === e.currentTarget.id)
						{
							var id = e.currentTarget.id.split("_");
							var selId = id[0];
							var filterId = id[1];
							var setVal= '';
							if(itemZ.value === 'regex'){
								setVal = '';
							}else{
								setVal = 'regex';
							}
							sel[selId].getFilter(filterId).setValueGeneric('regex',setVal);
						}
					});
				}

				if(e.currentTarget.className.indexOf("tkey") > -1){
					var id = e.currentTarget.id.split("_");
					var selId = id[0];
					var filterId = id[1];
					sel[selId].getFilter(filterId).setValueGeneric('key',e.currentTarget.value);
				}

				if(e.currentTarget.className.indexOf("tval") > -1){
					var id = e.currentTarget.id.split("_");
					var selId = id[0];
					var filterId = id[1];
					sel[selId].getFilter(filterId).setValueGeneric('value',e.currentTarget.value);
				}

				if(e.currentTarget.className.indexOf("nkey") > -1){
					var id = e.currentTarget.id.split("_");
					var selId = id[0];
					var filterId = id[1];
					sel[selId].getFilter(filterId).setValueGeneric('key',e.currentTarget.value);
				}

				if(e.currentTarget.className.indexOf("nmin") > -1){
					var id = e.currentTarget.id.split("_");
					var selId = id[0];
					var filterId = id[1];
					sel[selId].getFilter(filterId).setValueGeneric('minValue',e.currentTarget.value);
				}

				if(e.currentTarget.className.indexOf("nmax") > -1){
					var id = e.currentTarget.id.split("_");
					var selId = id[0];
					var filterId = id[1];
					sel[selId].getFilter(filterId).setValueGeneric('maxValue',e.currentTarget.value);
				}

				if(e.currentTarget.className.indexOf("dkey") > -1){
					var id = e.currentTarget.id.split("_");
					var selId = id[0];
					var filterId = id[1];
					sel[selId].getFilter(filterId).setValueGeneric('key',e.currentTarget.value);
				}

				if(e.currentTarget.className.indexOf("dmin") > -1){
					var id = e.currentTarget.id.split("_");
					var selId = id[0];
					var filterId = id[1];
					sel[selId].getFilter(filterId).setValueGeneric('minDate',e.currentTarget.value);
				}

				if(e.currentTarget.className.indexOf("dmax") > -1){
					var id = e.currentTarget.id.split("_");
					var selId = id[0];
					var filterId = id[1];
					sel[selId].getFilter(filterId).setValueGeneric('maxDate',e.currentTarget.value);
				}
			},

			loadGuiState: function(input){

				this.guiSelectors = [];

				//console.log('loadInput');
				//console.log(input);

				// add functionality
				var al = input.length;
				//console.log(al);
				for(i = 0; i < al; i++){
					jQuery.extend(input[i], guiSelectorFunctions);  
					var al2 = input[i].getFilters().length;
					//console.log(al2);
					for(j = 0; j < al2; j++){
						//console.log('it ' + j);
						jQuery.extend(input[i].getFilter(j), filterFunctions);
						//console.log(input[i].getFilter(j));
						//console.log(input[i].getFilter(j).getValueGeneric('key'));
					}
				}

				this.guiSelectors = input;

			}

		});