package invar.model;

import invar.InvarContext;
import java.util.LinkedList;

public class InvarField<T extends InvarType>
{
    private final T               type;
    private final String          key;
    private final String          comment;
    private LinkedList<InvarType> generics;
    private String                typeFormatted;
    private String                defaultVal;
    private Boolean               encode;
    private Boolean               decode;
    private int                   widthType    = 1;
    private int                   widthKey     = 1;
    private int                   widthDefault = 1;

    public InvarField(T type, String key, String comment)
    {
        this.type = type;
        this.typeFormatted = "";
        this.generics = new LinkedList<InvarType>();
        this.key = key;
        this.comment = comment;
        this.setEncode(true);
        this.setDecode(true);
        this.setDefault("");
    }

    public String makeTypeFormatted(InvarContext ctx)
    {
        InvarType tR = ctx.typeRedirect(type);
        typeFormatted = tR.getName() + evalGenerics(ctx, tR);
        return getTypeFormatted();
    }

    public String getTypeFormatted()
    {
        return typeFormatted;
    }

    private String evalGenerics(InvarContext ctx, InvarType typeBasic)
    {
        if (getGenerics().size() == 0)
            return "";
        String s = typeBasic.getGeneric();
        for (InvarType t : getGenerics())
        {
            t = ctx.typeRedirect(t);
            s = s.replaceFirst("\\?", t.getName());
        }
        return s;
    }

    public T getType()
    {
        return (T)type;
    }

    public LinkedList<InvarType> getGenerics()
    {
        return generics;
    }

    public String getKey()
    {
        return key;
    }

    public void setEncode(Boolean encode)
    {
        this.encode = encode;
    }

    public void setDecode(Boolean decode)
    {
        this.decode = decode;
    }

    public void setDefault(String defaultValue)
    {
        this.defaultVal = defaultValue;
    }

    public Boolean getEncode()
    {
        return encode;
    }

    public Boolean getDecode()
    {
        return decode;
    }

    public String getDefault()
    {
        return defaultVal;
    }

    public String getComment()
    {
        return comment;
    }

    public int getWidthType()
    {
        return widthType;
    }

    public void setWidthType(int widthType)
    {
        this.widthType = widthType;
    }

    public int getWidthKey()
    {
        return widthKey;
    }

    public void setWidthKey(int widthKey)
    {
        this.widthKey = widthKey;
    }

    public int getWidthDefault()
    {
        return widthDefault;
    }

    public void setWidthDefault(int widthDefault)
    {
        this.widthDefault = widthDefault;
    }

}
