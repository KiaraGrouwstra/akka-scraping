###* Common helpers ###

define [ 'angular' ], (angular) ->
  'use strict'
  mod = angular.module('common.helper', [])
  mod.service 'helper', ->
    { sayHi: ->
      'hi'
 }
  mod
