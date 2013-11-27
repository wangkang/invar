package invar.model;

public class InvarType
{
    static public enum TypeID
    {
        STRUCT("struct"), ENUM("enum"), PROTOCOL("protoc"), //
        INT8("int8"), INT16("int16"), INT32("int32"), INT64("int64"), //
        UINT8("uint8"), UINT16("uint16"), UINT32("uint32"), UINT64("uint64"), //
        FLOAT("float"), DOUBLE("double"), BOOL("bool"), //
        STRING("string"), LIST("vec"), MAP("map"), GHOST("*");//

        public String getName ()
        {
            return name;
        }

        private TypeID(String name)
        {
            this.name = name;
        }

        private String name;
    };

    private final TypeID       id;
    private final InvarPackage pack;
    private final String       name;
    private final String       comment;
    private Boolean            isConflict;
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
        this.isConflict = false;
    }

    final public String fullName ()
    {
        return (pack.getName() != "") ? pack.getName() + "." + name : name;
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

    public Boolean getIsConflict ()
    {
        return isConflict;
    }

    public void setIsConflict (Boolean isConflict)
    {
        this.isConflict = isConflict;
    }
}
