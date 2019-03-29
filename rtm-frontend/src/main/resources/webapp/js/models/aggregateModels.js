var Aggregate = Backbone.Model.extend({});

var Aggregates = Backbone.Collection.extend({
  url: '/rtm/rest/aggregate/get',
  model: Aggregate,

  refreshData: function(rawInput){

    var that = this;

    input = JSON.stringify(rawInput);
    //console.log('Fetching Aggregates with following service input : ');
    //console.log(input);
    this.fetch({
      type : 'POST',
      dataType:'json',
      contentType:'application/json; charset=utf-8',
      data: input,
      success: function (response) {
        // console.log('loaded ' + response.length + ' objects.');
        //console.log(response.models[0].get('warning'));
        //console.log(JSON.stringify(response.models[0]));
        //console.log(JSON.stringify(response.models[0].get('payload')));
        if(response.models[0].get('status') !== 'SUCCESS')
          displayError('[SERVER_CALL] (1) Technical Error=' + JSON.stringify(response.models[0].get('metaMessage')));
        else{
          if(response.models[0].get('payload') && Object.keys(response.models[0].get('payload')).length > 0){
            that.trigger('AggregatesRefreshed');
          }
        }
      },
      error: function( model, response, options ){
        //console.log('model=' + JSON.stringify(model) + ', response=' + JSON.stringify(response) + ', options=' + JSON.stringify(options));
        that.trigger('pauseChartTimer');
        displayError('[SERVER_CALL] (2) Technical Error=' + JSON.stringify(response)+ ';       input=' + input);
        that.reset();
      }
    });
    return status;
  }
});

var AggregateDatapoints = Backbone.Collection.extend({
  url: '/rtm/rest/aggregate/refresh',
  model: Aggregate,

  refreshData: function(rawInput){

    var that = this;

    input = JSON.stringify(rawInput);
    this.fetch({
      type : 'POST',
      dataType:'json',
      contentType:'application/json; charset=utf-8',
      //Attempted to set a standard Cookie but refused
      //headers: {'Cookie' : Config.getProperty('curCookie')},
      data: input,
      success: function (response) {

        var responseMsg = response.models[0];
        if(responseMsg.get('status') !== 'SUCCESS'){

          console.log('debug1');
          displayError('[SERVER_CALL] (1) Technical Error=' + JSON.stringify(responseMsg.get('metaMessage')));
          // do we want a specific event to handle stream status? 
          that.trigger('dispatchStreamConsumed');
        }else{

          var payload = responseMsg.get('payload'); 

          if(payload && Object.keys(payload).length > 0){

            if(payload.stream.complete === true){
              console.log('stream complete! ' + JSON.stringify(payload.stream.complete));
              that.trigger('streamConsumed');
            }
            that.trigger('AggregateDatapointsRefreshed');
          }else{
            displayError('[SERVER_CALL] (2) Invalid paylaod=' + JSON.stringify(response)+ ';       input=' + input);
          }
        }
      },
      error: function( model, response, options ){
        that.trigger('pauseChartTimer');
        //console.log('model=' + JSON.stringify(model) + ', response=' + JSON.stringify(response) + ', options=' + JSON.stringify(options));
        displayError('[SERVER_CALL] (3) Technical Error=' + JSON.stringify(response)+ ';       input=' + input);
        that.reset();
      }
    });
    return status;
  }
});