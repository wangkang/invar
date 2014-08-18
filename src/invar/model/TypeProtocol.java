package invar.model;

public class TypeProtocol extends InvarType
{
    private TypeStruct server;
    private TypeStruct client;
    private Boolean    noClient;
    private Boolean    noServer;

    public TypeProtocol(String name, InvarPackage pack, String comment)
    {
        super(TypeID.PROTOCOL, name, pack, comment, false);
        client = new TypeStruct(name + "2S", pack, comment);
        server = new TypeStruct(name + "2C", pack, comment);
        noClient = true;
        noServer = true;
    }

    public TypeStruct getClient ()
    {
        return client;
    }

    public TypeStruct getServer ()
    {
        return server;
    }

    public TypeProtocol setNoClient (Boolean bool)
    {
        noClient = bool;
        return this;
    }

    public TypeProtocol setNoServer (Boolean bool)
    {
        noServer = bool;
        return this;
    }

    public Boolean hasClient ()
    {
        return !noClient;
    }

    public Boolean hasServer ()
    {
        return !noServer;
    }
}
