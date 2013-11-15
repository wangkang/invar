package invar.model;

public class InvarType
{
    static public enum TypeID
    {
        STRUCT, ENUM, PROTOCOL, //
        INT8, INT16, INT32, INT64, UINT8, UINT16, UINT32, UINT64, //
        FLOAT, DOUBLE, BOOL, STRING, LIST, MAP, GHOST//
    };

    private final TypeID       id;
    private final InvarPackage pack;
    private final String       name;
    private final String       comment;
    private TypeID             realId;
    private InvarType          redirect;
    private String             generic;
    private String             initValue;
    private String             initSuffix;
    private String             initPrefix;

    public InvarType(TypeID id, String name, InvarPackage pack, String comment)
    {
        this.id = id;
        this.name = name;
        this.comment = comment;
        this.pack = pack;
        this.generic = "";
        this.initPrefix = "";
        this.initSuffix = "";
        this.initValue = "";
    }

    final public String fullName (String splitter)
    {
        return (pack.getName() != "") ? pack.getName() + splitter + name : name;
    }

    final public TypeID getId ()
    {
        return id;
    }

    final public InvarPackage getPack ()
    {
        return pack;
    }

    final public String getName ()
    {
        return name;
    }

    final public String getComment ()
    {
        return comment;
    }

    final public String getGeneric ()
    {
        return generic;
    }

    public InvarType getRedirect ()
    {
        return redirect == null ? this : redirect;
    }

    final public void setGeneric (String template)
    {
        this.generic = template;
    }

    public void setRedirect (InvarType redirect)
    {
        this.redirect = redirect;
    }

    public String getInitValue ()
    {
        return initValue;
    }

    public void setInitValue (String construct)
    {
        this.initValue = construct;
    }

    public TypeID getRealId ()
    {
        return realId != null ? realId : id;
    }

    public void setRealId (TypeID realId)
    {
        this.realId = realId;
    }

    public String getInitSuffix ()
    {
        return initSuffix;
    }

    public void setInitSuffix (String initSuffix)
    {
        this.initSuffix = initSuffix;
    }

    public String getInitPrefix ()
    {
        return initPrefix;
    }

    public void setInitPrefix (String initPrefix)
    {
        this.initPrefix = initPrefix;
    }
}
