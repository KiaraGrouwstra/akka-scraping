# `main.js` is the file that sbt-web will use as an entry point
do (requirejs) ->
  'use strict'
  # -- RequireJS config --
  requirejs.config
    packages: [
      'common'
      'home'
      'user'
      'dashboard'
    ]
    shim:
      'jsRoutes':
        deps: []
        exports: 'jsRoutes'
      'angular':
        deps: [ 'jquery' ]
        exports: 'angular'
      'angular-route': [ 'angular' ]
      'angular-cookies': [ 'angular' ]
      'bootstrap': [ 'jquery' ]
    paths:
      'requirejs': [ '../lib/requirejs/require' ]
      'jquery': [ '../lib/jquery/jquery' ]
      'angular': [ '../lib/angularjs/angular' ]
      'angular-route': [ '../lib/angularjs/angular-route' ]
      'angular-cookies': [ '../lib/angularjs/angular-cookies' ]
      'bootstrap': [ '../lib/bootstrap/js/bootstrap' ]
      'jsRoutes': [ '/jsroutes' ]

  requirejs.onError = (err) ->
    console.log err
    return

  # Load the app. This is kept minimal so it doesn't need much updating.
  require [
    'angular'
    'angular-cookies'
    'angular-route'
    'jquery'
    'bootstrap'
    './app'
  ], (angular) ->
    angular.bootstrap document, [ 'app' ]
    return
  return
