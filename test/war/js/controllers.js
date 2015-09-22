'use strict';

// The root travelApp module.
var travelApp = travelApp || {};
travelApp.controllers = angular.module('travelControllers', ['ui.bootstrap']);

/////////////////////////////////////////////////////////////////

/**
 * @ngdoc service
 * @name fileUpload
 *
 * @description
 * The service used for file upload
 *
 */
travelApp.controllers.service('fileUpload', ['$http', function ($http) {
    this.uploadFileToUrl = function(key,file, uploadUrl){
        var fd = new FormData();
        fd.append('key', key);
	fd.append('file', file);
        return $http.post(uploadUrl, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        });
    }
}]);

/**
 * @ngdoc directive
 * @name fileModel
 *
 * @description
 * The directive used for file upload
 *
 */
travelApp.controllers.directive('fileModel', ['$parse', function ($parse) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            var model = $parse(attrs.fileModel);
            var modelSetter = model.assign;
            
            element.bind('change', function(){
                scope.$apply(function(){
                    modelSetter(scope, element[0].files[0]);
                });
            });
        }
    };
}]);
//////////////////////////////////////////////
travelApp.controllers.controller('MyProfileCtrl',
    function ($scope, $http,fileUpload, $log, oauth2Provider, HTTP_ERRORS) {
        $scope.submitted = false;
        $scope.loading = false;

        //The initial profile retrieved  
        $scope.initialProfile = {};

        $scope.genders = [
                                'Male',
                                'Female',
                                'Other', 
                         ];
        
        $scope.interests = [
                            'NOT_SPECIFIED',
                         'Toronto',
                         'London',
                         'LA',
                         'Pyongyang',
                         'Hongkong',
                         'Chennai',
                     ];

        $scope.profileImageLoading = true;
        
        $scope.init = function () {
            var retrieveProfileCallback = function () {
                $scope.profile = {};
                $scope.loading = true;
                
                $http.get('/file').success(function(uploadUrl){
					console.log(uploadUrl);
					$scope.uploadUrl = uploadUrl;
					
                gapi.client.travel.getProfile().
                    execute(function (resp) {
                        $scope.$apply(function () {
                            $scope.loading = false;
                            if (resp.error) {
                                // Failed to get a user profile.
                            } else {
                                // Succeeded 
                                $scope.profile.displayName = resp.result.displayName;
                                $scope.profile.gender = resp.result.gender;
                                $scope.profile.phone = resp.result.phone;
                                $scope.profile.age = resp.result.age;
                                $scope.profile.interest = resp.result.interest;
                                
                                $scope.profile.imagekey = resp.result.imagekey;
                                //
                                $scope.initialProfile = resp.result;
                                
                                $scope.profileImageLoading = false;
                            }
                        });
                    }
                );
            });
                
            };
            if (!oauth2Provider.signedIn) {
                var modalInstance = oauth2Provider.showLoginModal();
                modalInstance.result.then(retrieveProfileCallback);
            } else {
                retrieveProfileCallback();
            }
        };


        $scope.saveProfile = function () {
            $scope.submitted = true;
            $scope.loading = true;
            
            if($scope.profileImage){
        		$scope.profileImageLoading = true;
        		fileUpload.uploadFileToUrl($scope.profile.imagekey,$scope.profileImage, $scope.uploadUrl).success(function(imagekey){
        			$scope.profile.imagekey = imagekey;
        			$scope.profileImageLoading = false;
            
            
            
            gapi.client.travel.saveProfile($scope.profile).
                execute(function (resp) {
                    $scope.$apply(function () {
                        $scope.loading = false;
                        if (resp.error) {
                            // The request has failed.
                            var errorMessage = resp.error.message || '';
                            $scope.messages = 'Failed to update a profile : ' + errorMessage;
                            $scope.alertStatus = 'warning';
                            $log.error($scope.messages + 'Profile : ' + JSON.stringify($scope.profile));

                            if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
                                oauth2Provider.showLoginModal();
                                return;
                            }
                        } else {
                            // The request has succeeded.
                            $scope.messages = 'The profile has been updated';
                            $scope.alertStatus = 'success';
                            $scope.submitted = false;
                            $scope.initialProfile = {
                                displayName: $scope.profile.displayName,
                                gender: $scope.profile.gender,
                                interest: $scope.profile.interest,
                                phone: $scope.profile.phone,
                                age: $scope.profile.age,
                                //
                            };

                            $log.info($scope.messages + JSON.stringify(resp.result));
                        }
                    });
                });
        });
        		
            } else{

        		gapi.client.travel.saveProfile($scope.profile).execute(function (resp) {
        				    $scope.$apply(function () {
        				        $scope.loading = false;
        				        if (resp.error) {
        				            // The request has failed.
        				            var errorMessage = resp.error.message || '';
        				            $scope.messages = 'Failed to update a profile : ' + errorMessage;
        				            $scope.alertStatus = 'warning';
        				            $log.error($scope.messages + 'Profile : ' + JSON.stringify($scope.profile));

        				            if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
        				                oauth2Provider.showLoginModal();
        				                return;
        				            }
        				        } else {
        				            // The request has succeeded.
        				            $scope.messages = 'The profile has been updated';
        				            $scope.alertStatus = 'success';
        				            $scope.submitted = false;
        				            $scope.initialProfile = {
        				            		displayName: $scope.profile.displayName,
        	                                gender: $scope.profile.gender,
        	                                interest: $scope.profile.interest,
        	                                phone: $scope.profile.phone,
        	                                age: $scope.profile.age,
        	                                //  
        	                                };

        				            $log.info($scope.messages + JSON.stringify(resp.result));
        				        }
        				});
                         	});
            }  		
            };
    })
;

        		
        		
        		
        		
/////////////////////////////////////////////////////////////////
travelApp.controllers.controller('CreateTravelCtrl',
    function ($scope, $log, oauth2Provider, HTTP_ERRORS) {

        $scope.travel = $scope.travel || {};

        $scope.cities = [
            'Toronto',
            'London',
            'LA',
            'Pyongyang',
            'Hongkong',
            'Chennai'
        ];

        $scope.topics = [
            'Sightseeing',
            'Night Life',
            'Hangout',
            'Food',
            'Physical Activities',
            'Camping',
            'Gambling',
            'Programming'
        ];

         // Tests if the arugment is an integer and not negative.         
        $scope.isValidMaxAttendees = function () {
            if (!$scope.travel.maxAttendees || $scope.travel.maxAttendees.length == 0) {
                return true;
            }
            return /^[\d]+$/.test($scope.travel.maxAttendees) && $scope.travel.maxAttendees >= 0;
        }

        // Tests if the travel.startDate and travel.endDate are valid.
        $scope.isValidDates = function () {
            if (!$scope.travel.startDate && !$scope.travel.endDate) {
                return true;
            }
            if ($scope.travel.startDate && !$scope.travel.endDate) {
                return true;
            }
            return $scope.travel.startDate <= $scope.travel.endDate;
        }

       
         // Tests if $scope.travel is valid.
        $scope.isValidTravel = function (travelForm) {
            return !travelForm.$invalid &&
                $scope.isValidMaxAttendees() &&
                $scope.isValidDates();
        }

      
         //Invokes the travel.createTravel API.
        $scope.createTravel = function (travelForm) {
            if (!$scope.isValidTravel(travelForm)) {
                return;
            }

            $scope.loading = true;
            gapi.client.travel.createTravel($scope.travel).
                execute(function (resp) {
                    $scope.$apply(function () {
                        $scope.loading = false;
                        if (resp.error) {
                            // The request has failed.
                            var errorMessage = resp.error.message || '';
                            $scope.messages = 'Failed to create a trip : ' + errorMessage;
                            $scope.alertStatus = 'warning';
                            $log.error($scope.messages + ' Travel : ' + JSON.stringify($scope.travel));

                            if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
                                oauth2Provider.showLoginModal();
                                return;
                            }
                        } else {
                            // The request has succeeded.
                            $scope.messages = 'The trip has been created : ' + resp.result.name;
                            $scope.alertStatus = 'success';
                            $scope.submitted = false;
                            $scope.travel = {};
                            $log.info($scope.messages + ' : ' + JSON.stringify(resp.result));
                        }
                    });
                });
        };
    });


travelApp.controllers.controller('ShowTravelCtrl', function ($scope, $log, oauth2Provider, HTTP_ERRORS) {

    $scope.submitted = false;
    $scope.selectedTab = 'ALL';

    $scope.filters = [
    ];

    $scope.filtereableFields = [
        {enumValue: 'CITY', displayName: 'City'},
        {enumValue: 'TOPIC', displayName: 'Topic'},
        {enumValue: 'MONTH', displayName: 'Start month'},
        {enumValue: 'MAX_ATTENDEES', displayName: 'Max Attendees'}
    ]

    $scope.operators = [
        {displayName: '=', enumValue: 'EQ'},
        {displayName: '>', enumValue: 'GT'},
        {displayName: '>=', enumValue: 'GTEQ'},
        {displayName: '<', enumValue: 'LT'},
        {displayName: '<=', enumValue: 'LTEQ'},
        {displayName: '!=', enumValue: 'NE'}
    ];

    $scope.travels = [];

    $scope.isOffcanvasEnabled = false;

    $scope.tabAllSelected = function () {
        $scope.selectedTab = 'ALL';
        $scope.queryTravels();
    };


    $scope.tabYouHaveCreatedSelected = function () {
        $scope.selectedTab = 'YOU_HAVE_CREATED';
        if (!oauth2Provider.signedIn) {
            oauth2Provider.showLoginModal();
            return;
        }
        $scope.queryTravels();
    };

    $scope.tabYouWillAttendSelected = function () {
        $scope.selectedTab = 'YOU_WILL_ATTEND';
        if (!oauth2Provider.signedIn) {
            oauth2Provider.showLoginModal();
            return;
        }
        $scope.queryTravels();
    };


    $scope.toggleOffcanvas = function () {
        $scope.isOffcanvasEnabled = !$scope.isOffcanvasEnabled;
    };

    $scope.pagination = $scope.pagination || {};
    $scope.pagination.currentPage = 0;
    $scope.pagination.pageSize = 20;

    $scope.pagination.numberOfPages = function () {
        return Math.ceil($scope.travels.length / $scope.pagination.pageSize);
    };


    $scope.pagination.pageArray = function () {
        var pages = [];
        var numberOfPages = $scope.pagination.numberOfPages();
        for (var i = 0; i < numberOfPages; i++) {
            pages.push(i);
        }
        return pages;
    };


    $scope.pagination.isDisabled = function (event) {
        return angular.element(event.target).hasClass('disabled');
    }

    $scope.addFilter = function () {
        $scope.filters.push({
            field: $scope.filtereableFields[0],
            operator: $scope.operators[0],
            value: ''
        })
    };

    $scope.clearFilters = function () {
        $scope.filters = [];
    };

    $scope.removeFilter = function (index) {
        if ($scope.filters[index]) {
            $scope.filters.splice(index, 1);
        }
    };
    
    ///////////query
    $scope.queryTravels = function () {
        $scope.submitted = false;
        if ($scope.selectedTab == 'ALL') {
            $scope.queryTravelsAll();
        } else if ($scope.selectedTab == 'YOU_HAVE_CREATED') {
            $scope.getTravelsCreated();
        } else if ($scope.selectedTab == 'YOU_WILL_ATTEND') {
            $scope.getTravelsAttend();
        }
    };

    /**
     * Invokes the travel.queryTravels API.
     */
    $scope.queryTravelsAll = function () {
        var sendFilters = {
            filters: []
        }
        for (var i = 0; i < $scope.filters.length; i++) {
            var filter = $scope.filters[i];
            if (filter.field && filter.operator && filter.value) {
                sendFilters.filters.push({
                    field: filter.field.enumValue,
                    operator: filter.operator.enumValue,
                    value: filter.value
                });
            }
        }
        $scope.loading = true;
        gapi.client.travel.queryTravels(sendFilters).
            execute(function (resp) {
                $scope.$apply(function () {
                    $scope.loading = false;
                    if (resp.error) {
                        // The request has failed.
                        var errorMessage = resp.error.message || '';
                        $scope.messages = 'Failed to query trips : ' + errorMessage;
                        $scope.alertStatus = 'warning';
                        $log.error($scope.messages + ' filters : ' + JSON.stringify(sendFilters));
                    } else {
                        // The request has succeeded.
                        $scope.submitted = false;
                        $scope.messages = 'Query succeeded : ' + JSON.stringify(sendFilters);
                        $scope.alertStatus = 'success';
                        $log.info($scope.messages);

                        $scope.travels = [];
                        angular.forEach(resp.items, function (travel) {
                            $scope.travels.push(travel);
                        });
                    }
                    $scope.submitted = true;
                });
            });
    }

    /**
     * Invokes the travel.getTravelsCreated method.
     */
    $scope.getTravelsCreated = function () {
        $scope.loading = true;
        gapi.client.travel.getTravelsCreated().
            execute(function (resp) {
                $scope.$apply(function () {
                    $scope.loading = false;
                    if (resp.error) {
                        // The request has failed.
                        var errorMessage = resp.error.message || '';
                        $scope.messages = 'Failed to query the trips created : ' + errorMessage;
                        $scope.alertStatus = 'warning';
                        $log.error($scope.messages);

                        if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
                            oauth2Provider.showLoginModal();
                            return;
                        }
                    } else {
                        // The request has succeeded.
                        $scope.submitted = false;
                        $scope.messages = 'Query succeeded : Trips you have created';
                        $scope.alertStatus = 'success';
                        $log.info($scope.messages);

                        $scope.travels = [];
                        angular.forEach(resp.items, function (travel) {
                            $scope.travels.push(travel);
                        });
                    }
                    $scope.submitted = true;
                });
            });
    };

  
    // Retrieves the travels to attend by calling the travel.getProfile method and
    //invokes the travel.getTravel method n times where n == the number of the travels to attend.
    $scope.getTravelsAttend = function () {
        $scope.loading = true;
        gapi.client.travel.getTravelsToAttend().
            execute(function (resp) {
                $scope.$apply(function () {
                    if (resp.error) {
                        // The request has failed.
                        var errorMessage = resp.error.message || '';
                        $scope.messages = 'Failed to query the trips to attend : ' + errorMessage;
                        $scope.alertStatus = 'warning';
                        $log.error($scope.messages);

                        if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
                            oauth2Provider.showLoginModal();
                            return;
                        }
                    } else {
                        // The request has succeeded.
                        $scope.travels = resp.result.items;
                        $scope.loading = false;
                        $scope.messages = 'Query succeeded : Trips you will attend (or you have attended)';
                        $scope.alertStatus = 'success';
                        $log.info($scope.messages);
                    }
                    $scope.submitted = true;
                });
            });
    };
});



travelApp.controllers.controller('TravelDetailCtrl', function ($scope, $log, $routeParams, HTTP_ERRORS) {
    $scope.travel = {};

    $scope.isUserAttending = false;

    $scope.init = function () {
        $scope.loading = true;
        gapi.client.travel.getTravel({
            websafeTravelKey: $routeParams.websafeTravelKey
        }).execute(function (resp) {
            $scope.$apply(function () {
                $scope.loading = false;
                if (resp.error) {
                    // The request has failed.
                    var errorMessage = resp.error.message || '';
                    $scope.messages = 'Failed to get the trip : ' + $routeParams.websafeKey
                        + ' ' + errorMessage;
                    $scope.alertStatus = 'warning';
                    $log.error($scope.messages);
                } else {
                    // The request has succeeded.
                    $scope.alertStatus = 'success';
                    $scope.travel = resp.result;
                }
            });
        });

        $scope.loading = true;
        // If the user is attending the travel, updates the status message and available function.
        gapi.client.travel.getProfile().execute(function (resp) {
            $scope.$apply(function () {
                $scope.loading = false;
                if (resp.error) {
                    // Failed to get a user profile.
                } else {
                    var profile = resp.result;
                    for (var i = 0; i < profile.travelKeysToAttend.length; i++) {
                        if ($routeParams.websafeTravelKey == profile.travelKeysToAttend[i]) {
                            // The user is attending the travel.
                            $scope.alertStatus = 'info';
                            $scope.messages = 'You have signed up for this trip';
                            $scope.isUserAttending = true;
                        }
                    }
                }
            });
        });
    };


    $scope.registerForTravel = function () {
        $scope.loading = true;
        gapi.client.travel.registerForTravel({
            websafeTravelKey: $routeParams.websafeTravelKey
        }).execute(function (resp) {
            $scope.$apply(function () {
                $scope.loading = false;
                if (resp.error) {
                    // The request has failed.
                    var errorMessage = resp.error.message || '';
                    $scope.messages = 'Failed to sign up the trip : ' + errorMessage;
                    $scope.alertStatus = 'warning';
                    $log.error($scope.messages);

                    if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
                        oauth2Provider.showLoginModal();
                        return;
                    }
                } else {
                    if (resp.result) {
                        // Register succeeded.
                        $scope.messages = 'Signed up for the trip';
                        $scope.alertStatus = 'success';
                        $scope.isUserAttending = true;
                        $scope.travel.seatsAvailable = $scope.travel.seatsAvailable - 1;
                    } else {
                        $scope.messages = 'Failed to sign up for the trip';
                        $scope.alertStatus = 'warning';
                    }
                }
            });
        });
    };


    $scope.unregisterFromTravel = function () {
        $scope.loading = true;
        gapi.client.travel.unregisterFromTravel({
            websafeTravelKey: $routeParams.websafeTravelKey
        }).execute(function (resp) {
            $scope.$apply(function () {
                $scope.loading = false;
                if (resp.error) {
                    // The request has failed.
                    var errorMessage = resp.error.message || '';
                    $scope.messages = 'Failed to cancel the trip : ' + errorMessage;
                    $scope.alertStatus = 'warning';
                    $log.error($scope.messages);
                    if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
                        oauth2Provider.showLoginModal();
                        return;
                    }
                } else {
                    if (resp.result) {
                        // Unregister succeeded.
                        $scope.messages = 'Trip cancelled';
                        $scope.alertStatus = 'success';
                        $scope.travel.seatsAvailable = $scope.travel.seatsAvailable + 1;
                        $scope.isUserAttending = false;
                        $log.info($scope.messages);
                    } else {
                        var errorMessage = resp.error.message || '';
                        $scope.messages = 'Failed to cancel the trip : ' + $routeParams.websafeKey +
                            ' : ' + errorMessage;
                        $scope.messages = 'Failed to cancel the trip';
                        $scope.alertStatus = 'warning';
                        $log.error($scope.messages);
                    }
                }
            });
        });
    };
});



// The root controller having a scope of the body element and methods used in the application wide such as user authentications.
travelApp.controllers.controller('RootCtrl', function ($scope, $location, oauth2Provider) {

	// Returns if the viewLocation is the currently viewed page.
    $scope.isActive = function (viewLocation) {
        return viewLocation === $location.path();
    };

    // Returns the OAuth2 signedIn state.
    $scope.getSignedInState = function () {
        return oauth2Provider.signedIn;
    };

    // Calls the OAuth2 authentication method.
    $scope.signIn = function () {
        oauth2Provider.signIn(function () {
            gapi.client.oauth2.userinfo.get().execute(function (resp) {
                $scope.$apply(function () {
                    if (resp.email) {
                        oauth2Provider.signedIn = true;
                        $scope.alertStatus = 'success';
                        $scope.rootMessages = 'Logged in with ' + resp.email;
                    }
                });
            });
        });
    };

    /**
     * Render the signInButton and restore the credential if it's stored in the cookie.
     * (Just calling this to restore the credential from the stored cookie. So hiding the signInButton immediately
     *  after the rendering)
     */
    $scope.initSignInButton = function () {
        gapi.signin.render('signInButton', {
            'callback': function () {
                jQuery('#signInButton button').attr('disabled', 'true').css('cursor', 'default');
                if (gapi.auth.getToken() && gapi.auth.getToken().access_token) {
                    $scope.$apply(function () {
                        oauth2Provider.signedIn = true;
                    });
                }
            },
            'clientid': oauth2Provider.CLIENT_ID,
            'cookiepolicy': 'single_host_origin',
            'scope': oauth2Provider.SCOPES
        });
    };

    //Logs out the user.
    $scope.signOut = function () {
        oauth2Provider.signOut();
        $scope.alertStatus = 'success';
        $scope.rootMessages = 'Logged out';
    };

    /**
     * Collapses the navbar on mobile devices.
     */
    $scope.collapseNavbar = function () {
        angular.element(document.querySelector('.navbar-collapse')).removeClass('in');
    };

});



 // The controller for the modal dialog that is shown when an user needs to login to achive some functions.
travelApp.controllers.controller('OAuth2LoginModalCtrl',
    function ($scope, $modalInstance, $rootScope, oauth2Provider) {
        $scope.singInViaModal = function () {
            oauth2Provider.signIn(function () {
                gapi.client.oauth2.userinfo.get().execute(function (resp) {
                    $scope.$root.$apply(function () {
                        oauth2Provider.signedIn = true;
                        $scope.$root.alertStatus = 'success';
                        $scope.$root.rootMessages = 'Logged in with ' + resp.email;
                    });

                    $modalInstance.close();
                });
            });
        };
    });


// A controller that holds properties for a datepicker.
travelApp.controllers.controller('DatepickerCtrl', function ($scope) {
    $scope.today = function () {
        $scope.dt = new Date();
    };
    $scope.today();

    $scope.clear = function () {
        $scope.dt = null;
    };

    // Disable weekend selection
    $scope.disabled = function (date, mode) {
        return ( mode === 'day' && ( date.getDay() === 0 || date.getDay() === 6 ) );
    };

    $scope.toggleMin = function () {
        $scope.minDate = ( $scope.minDate ) ? null : new Date();
    };
    $scope.toggleMin();

    $scope.open = function ($event) {
        $event.preventDefault();
        $event.stopPropagation();
        $scope.opened = true;
    };

    $scope.dateOptions = {
        'year-format': "'yy'",
        'starting-day': 1
    };

    $scope.formats = ['dd-MMMM-yyyy', 'yyyy/MM/dd', 'shortDate'];
    $scope.format = $scope.formats[0];
});
