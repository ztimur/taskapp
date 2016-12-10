package kg.timur.jetty.task.service;


import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import kg.timur.jetty.task.model.Task;


/**
 * Created by tzhamakeev on 11/25/16.
 */
public interface TaskService
{
    @POST
    @Path( "/add" )
    Response addTask( Task e );

    @DELETE
    @Path( "/{id}/delete" )
    Response deleteTask( @PathParam( "id" ) String id );

    @PUT
    @Path( "/{id}/update/{status}" )
    Response updateTask( @PathParam( "id" ) String id, @PathParam( "status" ) int status );

    @GET
    @Path( "/{id}/get" )
    Task getTask( @PathParam( "id" ) String id );

    @GET
    @Path( "/getAll" )
    Task[] getAllTasks();

    @GET
    @Path( "/getClusterStatus" )
    String getClusterStatus();
}
