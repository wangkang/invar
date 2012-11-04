package invar.model;

public class InvarType
{
    static public enum TypeID
    {
        INT8, INT16, INT32, INT64, UINT8, UINT16, UINT32, UINT64,

        FLOAT, DOUBLE, BOOL, STRING, LIST, MAP, GHOST,

        ENUM, STRUCT, PROTOCOL
    };

    private final TypeID       id;
    private final InvarPackage pack;
    private final String       name;
    private final String       comment;
    private String             generic;

    public InvarType(TypeID id, String name, InvarPackage pack, String comment)
    {
        this.id = id;
        this.name = name;
        this.comment = comment;
        this.pack = pack;
        this.generic = "";

    }

    final public InvarType setGeneric(String template)
    {
        this.generic = template;
        return this;
    }

    final public String fullName(String splitter)
    {
        return pack.getName() + splitter + name;
    }

    final public TypeID getId()
    {
        return id;
    }

    final public InvarPackage getPack()
    {
        return pack;
    }

    final public String getName()
    {
        return name;
    }

    final public String getComment()
    {
        return comment;
    }

    final public String getGeneric()
    {
        return generic;
    }

}
