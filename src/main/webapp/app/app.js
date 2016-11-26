//Define an angular module for our app
var app = angular.module('myApp', []);

function guid() {
    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000)
            .toString(16)
            .substring(1);
    }

    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
        s4() + '-' + s4() + s4() + s4();
}

app.controller('tasksController', function ($scope, $http) {
    getTask(); // Load all available tasks
    function getTask() {
        $http.get("rest/tasks/getAll").success(function (data) {
            console.log(JSON.stringify(data));
            $scope.tasks = data;
        });
    };
    $scope.addTask = function (message) {

        var task = {
            id: guid(),
            task: message,
            createdOn: new Date(),
            status: 0
        };

        $http.post("rest/tasks/add", task).success(function (data) {
            getTask();
            $scope.taskInput = "";
        });
    };
    $scope.deleteTask = function (taskId) {
        if (confirm("Are you sure to delete this line?")) {
            $http.delete("rest/tasks/" + taskId + "/delete").success(function (data) {
                console.log(JSON.stringify(data));
                getTask();
            });
        }
    };

    $scope.toggleStatus = function (item, status) {
        if (status == '2') {
            status = '0';
        } else {
            status = '2';
        }
        $http.put("rest/tasks/" + item + "/update/" + status).success(function (data) {
            console.log(JSON.stringify(data));
            getTask();
        });
    };

});
