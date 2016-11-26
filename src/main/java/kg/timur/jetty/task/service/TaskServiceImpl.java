package kg.timur.jetty.task.service;


import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kg.timur.jetty.task.CassandraClient;
import kg.timur.jetty.task.model.GenericResponse;
import kg.timur.jetty.task.model.Task;


/**
 * Created by tzhamakeev on 11/25/16.
 */
@Path( "/rest/tasks" )
@Consumes( MediaType.APPLICATION_JSON )
@Produces( MediaType.APPLICATION_JSON )
public class TaskServiceImpl implements TaskService
{
    private static Logger LOG = LoggerFactory.getLogger( TaskServiceImpl.class );

    private static int CASS_PORT = 9042;

    private static String[] CASS_CONFIG_POINTS = { "127.0.0.1" };

    private CassandraClient cassandraClient;


    public TaskServiceImpl()
    {
        LOG.info( "Initializing TaskServiceImpl......" );
        this.cassandraClient = new CassandraClient();
        try
        {
            initCassandra();
        }
        catch ( InterruptedException | IOException e )
        {
            e.printStackTrace();
        }
    }


    private void initCassandra() throws InterruptedException, IOException
    {
        cassandraClient = new CassandraClient();

        boolean notConnected = true;

        while ( !Thread.interrupted() && notConnected )
        {
            //            updateConfigs();
            try
            {
                cassandraClient.connect( CASS_CONFIG_POINTS, CASS_PORT );

                notConnected = false;
            }
            catch ( Exception e )
            {
                LOG.error( e.getMessage() );
                TimeUnit.SECONDS.sleep( 1 );
            }
        }

        if ( cassandraClient.getTableMetadata( "tasks" ) == null )
        {
            List<String> script = cassandraClient.getCql();

            LOG.info( "tasks.cql: {}", script );

            cassandraClient.createSchema( script );
        }
    }


    @Override
    @POST
    @Path( "/add" )
    public Response addTask( Task e )
    {
        GenericResponse response = new GenericResponse();
        if ( cassandraClient.getTask( e.getId() ) != null )
        {
            response.setStatus( false );
            response.setMessage( "Task Already Exists" );
            response.setErrorCode( "EC-01" );
            return Response.status( 422 ).entity( response ).build();
        }
        cassandraClient.addTask( e );
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
        if ( cassandraClient.getTask( id ) == null )
        {
            response.setStatus( false );
            response.setMessage( "Task Doesn't Exists" );
            response.setErrorCode( "EC-02" );
            return Response.status( 404 ).entity( response ).build();
        }
        cassandraClient.removeTask( id );
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
        if ( cassandraClient.getTask( id ) == null )
        {
            response.setStatus( false );
            response.setMessage( "Task Doesn't Exists" );
            response.setErrorCode( "EC-03" );
            return Response.status( 404 ).entity( response ).build();
        }
        Task task = cassandraClient.getTask( id );
        task.setStatus( status );

        cassandraClient.addTask( task );
        response.setStatus( true );
        response.setMessage( "Task updated successfully" );
        return Response.ok( response ).build();
    }


    @Override
    @GET
    @Path( "/{id}/get" )
    public Task getTask( @PathParam( "id" ) String id )
    {
        return cassandraClient.getTask( id );
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
        return cassandraClient.getAllTasks().toArray( new Task[0] );
    }
}
