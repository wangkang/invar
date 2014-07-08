package invar.model;

public class InvarType
{
    static public enum TypeID
    {
        INT08("int8"), INT16("int16"), INT32("int32"), INT64("int64"), //
        UINT08("uint8"), UINT16("uint16"), UINT32("uint32"), UINT64("uint64"), //
        FLOAT("float"), DOUBLE("double"), BOOL("bool"), //
        STRING("string", true), //
        VEC("vec", "<?>", true), //
        MAP("map", "<?,?>", true), //
        ENUM("enum"), PROTOCOL("protoc"), //
        STRUCT("struct", true), //
        FUNC("func", "<?...>", true), //
        DIALECT("*", true);

        private TypeID(String name)
        {
            this.name = name;
            this.useRefer = false;
            this.generic = "";
        }

        private TypeID(String name, Boolean refer)
        {
            this.name = name;
            this.useRefer = refer;
            this.generic = "";
        }

        private TypeID(String name, String generic, Boolean refer)
        {
            this.name = name;
            this.generic = generic;
            this.useRefer = refer;
        }

        public String getName ()
        {
            return name;
        }

        public String getGeneric ()
        {
            return generic;
        }

        public Boolean getUseRefer ()
        {
            return useRefer;
        }

        final private String  name;
        final private String  generic;
        final private Boolean useRefer;
    };

    private final TypeID       id;
    private final InvarPackage pack;
    private final String       name;
    private final String       comment;
    private final Boolean      isBuildin;
    private TypeID             realId;
    private InvarType          redirect;
    private String             generic;
    private Boolean            isConflict;
    private String             initValue;
    private String             initSuffix;
    private String             initPrefix;
    private String             codePath;
    private String             codeName;

    public InvarType(TypeID id, String name, InvarPackage pack, String comment, Boolean isBuildin)
    {
        this.id = id;
        this.name = name;
        this.comment = comment;
        this.pack = pack;
        this.generic = "";
        this.initPrefix = "";
        this.initSuffix = "";
        this.initValue = "";
        this.codePath = "";
        this.codeName = name;
        this.isBuildin = isBuildin;
        this.isConflict = false;
    }

    final public String fullName (String splitter)
    {
        String packName = pack.getName().replaceAll("\\.", splitter);
        return !isBuildin() && !packName.equals("") ? packName + splitter + name : name;
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

    final public InvarType getRedirect ()
    {
        return redirect == null ? this : redirect;
    }

    final public String getInitValue ()
    {
        return initValue;
    }

    final public void setGeneric (String template)
    {
        this.generic = template;
    }

    public void setRedirect (InvarType redirect)
    {
        this.redirect = redirect;
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

    public String getCodePath ()
    {
        return codePath;
    }

    public void setCodePath (String codePath)
    {
        this.codePath = codePath;
    }

    public String getCodeName ()
    {
        return codeName;
    }

    public void setCodeName (String codeName)
    {
        this.codeName = codeName;
    }

    public Boolean isBuildin ()
    {
        return isBuildin;
    }

}
