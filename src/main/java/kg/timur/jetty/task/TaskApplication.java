package kg.timur.jetty.task;


import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import kg.timur.jetty.task.service.TaskServiceImpl;


/**
 * Created by tzhamakeev on 11/25/16.
 */
public class TaskApplication extends Application
{
    private Set<Object> singletons = new HashSet<Object>();


    public TaskApplication()
    {
        singletons.add( new TaskServiceImpl() );
    }


    @Override
    public Set<Object> getSingletons()
    {
        return singletons;
    }
}
