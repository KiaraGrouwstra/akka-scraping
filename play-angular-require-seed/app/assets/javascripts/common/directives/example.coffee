# A common directive.
# It would also be ok to put all directives into one file, or to define one RequireJS module that references them all.

define [ 'angular' ], (angular) ->
  'use strict'
  mod = angular.module('common.directives.example', [])
  mod.directive 'example', [
    '$log'
    ($log) ->
      {
        restrict: 'AE'
        link: ->
          $log.info 'Here prints the example directive from /common/directives.'
          return
      }
  ]
  mod
