###*
# Dashboard routes.
###

define [
  'angular'
  './controllers'
  'common'
], (angular, controllers) ->
  'use strict'
  mod = angular.module('dashboard.routes', [ 'yourprefix.common' ])
  mod.config [
    '$routeProvider'
    'userResolve'
    ($routeProvider, userResolve) ->
      $routeProvider.when '/dashboard',
        templateUrl: '/assets/javascripts/dashboard/dashboard.html'
        controller: controllers.DashboardCtrl
        resolve: userResolve
      #.when('/admin/dashboard',  {templateUrl: '/assets/templates/dashboard/admin.html',  controller:controllers.AdminDashboardCtrl})
      return
  ]
  mod
