###*
# User controllers.
###

define [], ->
  'use strict'

  LoginCtrl = ($scope, $location, userService) ->
    $scope.credentials = {}

    $scope.login = (credentials) ->
      userService.loginUser(credentials).then ->
        $location.path '/dashboard'
        return
      return

    return

  LoginCtrl.$inject = [
    '$scope'
    '$location'
    'userService'
  ]
  { LoginCtrl: LoginCtrl }
