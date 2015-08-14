###* Common filters. ###

define [ 'angular' ], (angular) ->
  'use strict'
  mod = angular.module('common.filters', [])

  ###*
  # Extracts a given property from the value it is applied to.
  # {{{
  # (user | property:'name')
  # }}}
  ###

  mod.filter 'property', (value, property) ->
    if angular.isObject(value)
      if value.hasOwnProperty(property)
        return value[property]
    return
  mod
