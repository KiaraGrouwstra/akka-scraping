###*
# Home routes.
###

define [
  'angular'
  './controllers'
  'common'
], (angular, controllers) ->
  'use strict'
  mod = angular.module('home.routes', [ 'yourprefix.common' ])
  mod.config [
    '$routeProvider'
    ($routeProvider) ->
      $routeProvider.when('/',
        templateUrl: '/assets/javascripts/home/home.html'
        controller: controllers.HomeCtrl).otherwise templateUrl: '/assets/javascripts/home/notFound.html'
      return
  ]
  mod
