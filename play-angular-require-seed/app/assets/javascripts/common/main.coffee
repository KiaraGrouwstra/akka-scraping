###*
# Common functionality.
###

define [
  'angular'
  './services/helper'
  './services/playRoutes'
  './filters'
  './directives/example'
], (angular) ->
  'use strict'
  angular.module 'yourprefix.common', [
    'common.helper'
    'common.playRoutes'
    'common.filters'
    'common.directives.example'
  ]
