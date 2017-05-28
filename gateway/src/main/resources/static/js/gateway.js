angular.module('hello', [ 'ngRoute' ])
  .config(function($routeProvider, $httpProvider) {

    $routeProvider.when('/register', {
          templateUrl : 'register.html',
          controller : 'register',
          controllerAs: 'controller'
        }).otherwise('/');

    $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';

  })
  .controller('register', function($http, $location) {
    var self = this;

    var userName;

    self.register = function() {
        $http.post('/user').then(function(response) {
          $location.path("/");
        });
    }
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