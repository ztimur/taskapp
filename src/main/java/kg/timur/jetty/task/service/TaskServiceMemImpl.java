package kg.timur.jetty.task.service;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import kg.timur.jetty.task.model.GenericResponse;
import kg.timur.jetty.task.model.Task;


/**
 * Created by tzhamakeev on 11/25/16.
 */
@Path( "/rest/tasksMem" )
@Consumes( MediaType.APPLICATION_JSON )
@Produces( MediaType.APPLICATION_JSON )
public class TaskServiceMemImpl implements TaskService
{

    private static Map<String, Task> tasks = new HashMap<>();


    @Override
    @POST
    @Path( "/add" )
    public Response addTask( Task e )
    {
        GenericResponse response = new GenericResponse();
        if ( tasks.get( e.getId() ) != null )
        {
            response.setStatus( false );
            response.setMessage( "Task Already Exists" );
            response.setErrorCode( "EC-01" );
            return Response.status( 422 ).entity( response ).build();
        }
        tasks.put( e.getId(), e );
        response.setStatus( true );
        response.setMessage( "Task created successfully" );
        return Response.ok( response ).build();
    }


    @Override
    @DELETE
    @Path( "/{id}/delete" )
    public Response deleteTask( @PathParam( "id" ) String id )
    {
        GenericResponse response = new GenericResponse();
        if ( tasks.get( id ) == null )
        {
            response.setStatus( false );
            response.setMessage( "Task Doesn't Exists" );
            response.setErrorCode( "EC-02" );
            return Response.status( 404 ).entity( response ).build();
        }
        tasks.remove( id );
        response.setStatus( true );
        response.setMessage( "Task deleted successfully" );
        return Response.ok( response ).build();
    }


    @Override
    @PUT
    @Path( "/{id}/update/{status}" )
    public Response updateTask( @PathParam( "id" ) String id, @PathParam( "status" ) int status )
    {
        GenericResponse response = new GenericResponse();
        if ( tasks.get( id ) == null )
        {
            response.setStatus( false );
            response.setMessage( "Task Doesn't Exists" );
            response.setErrorCode( "EC-03" );
            return Response.status( 404 ).entity( response ).build();
        }
        Task task = tasks.get( id );
        task.setStatus( status );
        response.setStatus( true );
        response.setMessage( "Task updated successfully" );
        return Response.ok( response ).build();
    }


    @Override
    @GET
    @Path( "/{id}/get" )
    public Task getTask( @PathParam( "id" ) String id )
    {
        return tasks.get( id );
    }


    @GET
    @Path( "/{id}/getDummy" )
    public Task getDummyTask( @PathParam( "id" ) String id )
    {
        return new Task( id, "Dummy", 0, new Date() );
    }


    @Override
    @GET
    @Path( "/getAll" )
    public Task[] getAllTasks()
    {
        Set<String> ids = tasks.keySet();
        Task[] e = new Task[ids.size()];
        int i = 0;
        for ( String id : ids )
        {
            e[i] = tasks.get( id );
            i++;
        }
        return e;
    }
}
