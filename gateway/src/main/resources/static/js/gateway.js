angular.module('hello', [ 'ngRoute' ])
  .config(function($routeProvider, $httpProvider) {

    $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';

  })
  .controller('home', function($http) {
    var self = this;
    $http.get('/resource/').then(function(response) {
      self.greeting = response.data;
    })
  })
  .controller('navigation',
     function($rootScope, $http, $location) {

     var self = this

     var authenticate = function(credentials, callback) {

       $http.get('user')
       .then(function(response) {
         var data = response.data;
         if (data.name) {
           self.authenticated = true;
           self.user = data.name
           self.admin = data && data.roles && data.roles.indexOf("ROLE_ADMIN")>-1;
         } else {
           self.authenticated = false;
           self.admin = false;
         }
         callback && callback(true);
       }, function() {
         self.authenticated = false;
         callback && callback(false);
       });

     }

     authenticate();

     self.credentials = {};

     self.logout = function() {
       $http.post('logout', {}).finally(function() {
         self.authenticated = false;
         self.admin = false;
       });
     }
   });