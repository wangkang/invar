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
    private int                   widthTypeMax = 30;
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
        InvarType t = type.getRedirect() == null ? type : type.getRedirect();
        typeFormatted = t.getName() + evalGenerics(ctx, t);
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
            t = t.getRedirect() == null ? t : t.getRedirect();
            s = s.replaceFirst("\\?", t.getName() + t.getGeneric());
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

    public int getWidthKey()
    {
        return Math.min(widthKey, 20);
    }

    public int getWidthType()
    {
        return Math.min(widthType, widthTypeMax);
    }

    public int getWidthDefault()
    {
        return Math.min(widthDefault, 30);
    }

    public void setWidthType(int widthType)
    {
        this.widthType = widthType;
    }

    public void setWidthKey(int widthKey)
    {
        this.widthKey = widthKey;
    }

    public void setWidthDefault(int widthDefault)
    {
        this.widthDefault = widthDefault;
    }

    public int getWidthTypeMax()
    {
        return widthTypeMax;
    }

    public void setWidthTypeMax(int widthTypeMax)
    {
        this.widthTypeMax = widthTypeMax;
    }

}
