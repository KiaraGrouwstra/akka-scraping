###*
# Play JavaScript routing as an AngularJS module.
# Wraps Play's routes to use Angular's $http.
# Example:
# {{{
# // For `POST /login controller.Application.login()` Play generates:
# jsRoutes.controllers.Application.login()
# // With playRoutes, this can be used like this:
# playRoutes.controllers.Application.login().post({user:'username', password:'secret'}).then(function(response) {
#   ...
# )};
# }}}
# @author Marius Soutier, 2013
###

define [
  'angular'
  'require'
  'jsRoutes'
], (angular, require, jsRoutes) ->
  'use strict'
  # The service - will be used by controllers or other services, filters, etc.
  mod = angular.module('common.playRoutes', [])
  mod.service 'playRoutes', [
    '$http'
    ($http) ->

      ###*
      # Wrap a Play JS function with a new function that adds the appropriate $http method.
      # Note that the url has been already applied to the $http method so you only have to pass in
      # the data (if any).
      # Note: This is not only easier on the eyes, but must be called in a separate function with its own
      # set of arguments, because otherwise JavaScript's function scope will bite us.
      # @param playFunction The function from Play's jsRouter to be wrapped
      ###

      wrapHttp = (playFunction) ->
        ->
          routeObject = playFunction.apply(this, arguments)
          httpMethod = routeObject.method.toLowerCase()
          url = routeObject.url
          res =
            method: httpMethod
            url: url
            absoluteUrl: routeObject.absoluteURL
            webSocketUrl: routeObject.webSocketURL

          res[httpMethod] = (obj) ->
            $http[httpMethod] url, obj

          res

      # Add package object, in most cases 'controllers'

      addPackageObject = (packageName, service) ->
        if !(packageName of service)
          service[packageName] = {}
        return

      # Add controller object, e.g. Application

      addControllerObject = (controllerKey, service) ->
        addPackageObject controllerKey, service
        return

      playRoutes = {}
      # checks if the controllerKey starts with a lower case letter

      isControllerKey = (controllerKey) ->
        /^[A-Z].*/.test controllerKey

      addRoutes = (playRoutesObject, jsRoutesObject) ->
        for key of jsRoutesObject
          if isControllerKey(key)
            controller = jsRoutesObject[key]
            addControllerObject key, playRoutesObject
            for methodKey of controller
              playRoutesObject[key][methodKey] = wrapHttp(controller[methodKey])
          else
            addPackageObject key, playRoutesObject
            addRoutes playRoutesObject[key], jsRoutesObject[key]
        return

      addRoutes playRoutes, jsRoutes
      playRoutes
  ]
  mod
