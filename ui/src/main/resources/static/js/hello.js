angular.module('hello', [ 'ngRoute' ])
  .config(function($routeProvider, $httpProvider) {

    $routeProvider.when('/', {
      templateUrl : 'home.html',
      controller : 'home',
      controllerAs: 'controller'
    }).when('/login', {
      templateUrl : 'login.html',
      controller : 'navigation',
      controllerAs: 'controller'
    }).otherwise('/');

    $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';

  })
  .controller('home', function($http) {
    var self = this;
    $http.get('resource/').then(function(response) {
      self.greeting = response.data;
    })
  })
  .controller('navigation',

  function($rootScope, $http, $location, $route) {

    var self = this;

    $http.get('user').then(function(response) {
      if (response.data.name) {
        $rootScope.authenticated = true;
      } else {
        $rootScope.authenticated = false;
      }
    }, function() {
      $rootScope.authenticated = false;
    });

    self.credentials = {};

    self.logout = function() {
      $http.post('logout', {}).finally(function() {
        $rootScope.authenticated = false;
        $location.path("/");
      });
    }

  });