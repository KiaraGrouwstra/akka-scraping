# `main.js` is the file that sbt-web will use as an entry point
do (requirejs) ->
  'use strict'
  # -- RequireJS config --
  requirejs.config
    # By default load any module IDs from here
    # baseUrl: '../lib'
    packages: [
      'common'
      'home'
      'user'
      'dashboard'
    ]
    # overwrite default paths
    paths:
      'requirejs': [ '../lib/requirejs/require' ]
      'jquery': [ '../lib/jquery/jquery' ]
      # 'angular': [ '../lib/angularjs/angular.min' ]
      'angular': [ '../lib/angularjs/angular' ]
      'angular-route': [ '../lib/angularjs/angular-route' ]
      'angular-cookies': [ '../lib/angularjs/angular-cookies' ]
      # 'bootstrap': [ '../lib/bootstrap/js/bootstrap.min' ]
      'bootstrap': [ '../lib/bootstrap/js/bootstrap' ]
      'jsRoutes': [ '/jsroutes' ]
      # 'angular-material': [ '../lib/angular-material/angular-material.min' ]
      'angular-material': [ '../lib/angular-material/angular-material' ]
      # 'angular-ui': [ '../lib/angular-ui/angular-ui.min' ]
      'angular-ui': [ '../lib/angular-ui/angular-ui' ]
      # 'ui-bootstrap': [ '../lib/angular-ui-bootstrap/ui-bootstrap.min' ]
      'ui-bootstrap': [ '../lib/angular-ui-bootstrap/ui-bootstrap' ]
      # 'underscore': [ '../lib/underscore/underscore.min' ]
      # 'lodash': [ '../lib/lodash/lodash.min' ]
      'lodash': [ '../lib/lodash/lodash' ]
      # 'es5-shim': [ '../lib/es5-shim/es5-shim.min' ]
      'es5-shim': [ '../lib/es5-shim/es5-shim' ]
      # 'es6-shim': [ '../lib/es5-shim/es6-shim.min' ]
      'es6-shim': [ '../lib/es6-shim/es6-shim' ]
      # 'ripples': [ '../lib/bootstrap-material-design/js/ripples.min' ]
      'ripples': [ '../lib/bootstrap-material-design/js/ripples' ]
      # 'bootstrap-material': [ '../lib/bootstrap-material-design/js/material.min' ]
      'bootstrap-material': [ '../lib/bootstrap-material-design/js/material' ]
      # '': [ '../lib/' ]
    # alias modules
    # map:
    #   'some/newmodule':
    #       'foo': 'foo1.2'
    #   'some/oldmodule':
    #       'foo': 'foo1.0'
    # get multiple module IDs from another script
    # bundles:
    #   'primary': ['main', 'util', 'text', 'text!template.html']
    #   'secondary': ['text!secondary.html']
    # deps/exports/inits for scripts not using requirejs's define()
    shim:
      'jquery':
        exports: '$'
      'jsRoutes':
        deps: []
        exports: 'jsRoutes'
      'angular':
        deps: [ 'jquery' ]  # 'es6-shim'
        exports: 'angular'
      'angular-route': [ 'angular' ]
      'angular-cookies': [ 'angular' ]
      'angular-ui': [ 'angular' ]
      'ui-bootstrap': [ 'angular-ui' ]
      'angular-material': [ 'angular' ]
      'bootstrap': [ 'jquery' ]
      'es6-shim': [ 'es5-shim' ]
      # 'underscore':
      'lodash':
        exports: '_'
      'ripples': [ 'bootstrap' ]
      'bootstrap-material':
        deps: [ 'bootstrap' ]
        # init: ($) ->
        #   $(document).ready ->
        #     $.material.init()
        #     return
      # '': [ '' ]
    # pass config variables into module.config()
    # config:
    #   'bar':
    #     size: 'large'
    #   'baz':
    #     color: 'blue'
    # kick start application
    deps: ['app']

  requirejs.onError = (err) ->
    console.log err
    return

  # Load scripts into variables and do stuff
  require [
    'angular'
    'angular-cookies'
    'angular-route'
    'jquery'
    'bootstrap'
    'angular-material'
    'angular-ui'
    'ui-bootstrap'
    'lodash'
    'es5-shim'
    'es6-shim'
    'ripples'
    'bootstrap-material'
    './app'
  ], (angular) ->
    $(document).ready ->
      $.material.init()
    angular.bootstrap document, [ 'app' ]
    return
  return
