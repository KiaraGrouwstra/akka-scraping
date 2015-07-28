###*
# User service, exposes user model to the rest of the app.
###

define [
  'angular'
  'common'
], (angular) ->
  'use strict'
  mod = angular.module('user.services', [
    'yourprefix.common'
    'ngCookies'
  ])
  mod.factory 'userService', [
    '$http'
    '$q'
    'playRoutes'
    '$cookies'
    '$log'
    ($http, $q, playRoutes, $cookies, $log) ->
      user = undefined
      token = $cookies.get('XSRF-TOKEN')

      ### If the token is assigned, check that the token is still valid on the server ###

      if token
        $log.info 'Restoring user from cookie...'
        playRoutes.controllers.Users.authUser().get().success((data) ->
          $log.info 'Welcome back, ' + data.name
          user = data
          return
        ).error ->
          $log.info 'Token no longer valid, please log in.'
          token = undefined
          delete $cookies['XSRF-TOKEN']
          $q.reject 'Token invalid'
      {
        loginUser: (credentials) ->
          playRoutes.controllers.Application.login().post(credentials).then((response) ->
            # return promise so we can chain easily
            token = response.data.token
            playRoutes.controllers.Users.authUser().get()
          ).then (response) ->
            user = response.data
            user
        logout: ->
          # Logout on server in a real app
          delete $cookies['XSRF-TOKEN']
          token = undefined
          user = undefined
          playRoutes.controllers.Application.logout().post().then ->
            $log.info 'Good bye '
            return
        getUser: ->
          user

      }
  ]

  ###*
  # Add this object to a route definition to only allow resolving the route if the user is
  # logged in. This also adds the contents of the objects as a dependency of the controller.
  ###

  mod.constant 'userResolve', user: [
    '$q'
    'userService'
    ($q, userService) ->
      deferred = $q.defer()
      user = userService.getUser()
      if user
        deferred.resolve user
      else
        deferred.reject()
      deferred.promise
  ]

  ###*
  # If the current route does not resolve, go back to the start page.
  ###

  handleRouteError = ($rootScope, $location) ->
    $rootScope.$on '$routeChangeError', ->
      $location.path '/'
      return
    return

  handleRouteError.$inject = [
    '$rootScope'
    '$location'
  ]
  mod.run handleRouteError
  mod
