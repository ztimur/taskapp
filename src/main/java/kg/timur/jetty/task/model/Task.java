package kg.timur.jetty.task.model;


import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;


/**
 * Created by tzhamakeev on 11/25/16.
 */
@XmlRootElement( name = "task" )
public class Task
{
    @JsonProperty( value = "id" )
    private String id;
    @JsonProperty( value = "task" )
    private String task;
    @JsonProperty( value = "status" )
    private Integer status;
    @JsonProperty( value = "createdOn" )
    private Date createdOn;


    @JsonCreator
    public Task( @JsonProperty( value = "id" ) final String id, @JsonProperty( value = "task" ) final String task,
                 @JsonProperty( value = "status" ) final Integer status,
                 @JsonProperty( value = "createdOn" ) final Date createdOn )
    {
        this.id = id;
        this.task = task;
        this.status = status;
        this.createdOn = createdOn;
    }


    public String getId()
    {
        return id;
    }


    public void setId( final String id )
    {
        this.id = id;
    }


    public String getTask()
    {
        return task;
    }


    public void setTask( final String task )
    {
        this.task = task;
    }


    public Integer getStatus()
    {
        return status;
    }


    public void setStatus( final Integer status )
    {
        this.status = status;
    }


    public Date getCreatedOn()
    {
        return createdOn;
    }


    public void setCreatedOn( final Date createdOn )
    {
        this.createdOn = createdOn;
    }
}
