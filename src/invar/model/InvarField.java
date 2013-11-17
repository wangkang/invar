package invar.model;

import invar.InvarContext;
import invar.model.InvarType.TypeID;
import java.util.LinkedList;

public class InvarField
{
    private final InvarType             type;
    private final LinkedList<InvarType> generics;
    private final String                key;
    private final String                comment;
    private String                      shortName;
    private String                      defaultVal;
    private Boolean                     encode;
    private Boolean                     decode;

    private String                      typeFormatted = "";
    private String                      deftFormatted = "";
    private int                         widthType     = 1;
    private int                         widthKey      = 1;
    private int                         widthDefault  = 1;

    public InvarField(InvarType type, String key, String comment)
    {
        this.type = type;
        this.generics = new LinkedList<InvarType>();
        this.key = key;
        this.comment = comment;
        this.setEncode(true);
        this.setDecode(true);
        this.setDefault("");
    }

    public InvarType getType ()
    {
        return type;
    }

    public LinkedList<InvarType> getGenerics ()
    {
        return generics;
    }

    public String getTypeFormatted ()
    {
        return typeFormatted;
    }

    public String getDeftFormatted ()
    {
        return deftFormatted;
    }

    public String getKey ()
    {
        return key;
    }

    public String makeTypeFormatted (InvarContext ctx)
    {
        InvarType t = type.getRedirect();
        String tName = t.getName();
        if (ctx.findTypes(t.getName()).size() > 1)
            tName = t.fullName(".");
        typeFormatted = tName + evalGenerics(ctx, t);
        return typeFormatted;
    }

    private String evalGenerics (InvarContext ctx, InvarType typeBasic)
    {
        if (getGenerics().size() == 0)
            return "";
        String s = typeBasic.getGeneric();
        for (InvarType t : getGenerics())
        {
            t = t.getRedirect();
            String tName = t.getName();
            if (ctx.findTypes(t.getName()).size() > 1)
                tName = t.fullName(".");
            s = s.replaceFirst("\\?", tName + t.getGeneric());
        }
        return s;
    }

    public String createAliasRule (InvarContext ctx, String split)
    {
        InvarType typeBasic = type;
        String s = typeBasic.getGeneric();
        for (InvarType t : getGenerics())
        {
            s = s.replaceFirst("\\?", t.fullName(split) + t.getGeneric());
        }
        return typeBasic.fullName(split) + s;
    }

    public String createFullNameRule (InvarContext ctx, String split)
    {
        InvarType typeBasic = type.getRedirect();
        if (getGenerics().size() == 0)
            return typeBasic.fullName(split);
        String s = typeBasic.getGeneric();
        for (InvarType t : getGenerics())
        {
            t = t.getRedirect();
            s = s.replaceFirst("\\?", t.fullName(split) + t.getGeneric());
        }
        return typeBasic.fullName(split) + s;
    }

    public String createShortRule (InvarContext ctx)
    {
        String split = ".";
        InvarType typeBasic = type.getRedirect();
        if (getGenerics().size() == 0)
        {
            if (ctx.findTypes(typeBasic.getName()).size() > 1)
                return typeBasic.fullName(split);
            else
                return typeBasic.getName();
        }
        String s = typeBasic.getGeneric();
        for (InvarType t : getGenerics())
        {
            t = t.getRedirect();

            String forShort = null;
            if (t.getRealId() == TypeID.LIST || t.getRealId() == TypeID.MAP)
                forShort = t.getName();
            else
            {
                if (ctx.findTypes(t.getName()).size() > 1)
                    forShort = t.fullName(split);
                else
                    forShort = t.getName();
            }
            s = s.replaceFirst("\\?", forShort + t.getGeneric());
        }
        return typeBasic.getName() + s;

    }

    public void setEncode (Boolean encode)
    {
        this.encode = encode;
    }

    public void setDecode (Boolean decode)
    {
        this.decode = decode;
    }

    public void setDefault (String defaultValue)
    {
        this.defaultVal = defaultValue;
    }

    public Boolean getEncode ()
    {
        return encode;
    }

    public Boolean getDecode ()
    {
        return decode;
    }

    public String getDefault ()
    {
        return defaultVal;
    }

    public String getComment ()
    {
        return comment;
    }

    public int getWidthKey ()
    {
        return Math.min(widthKey, 24);
    }

    public int getWidthType ()
    {
        return Math.min(widthType, 48);
    }

    public int getWidthDefault ()
    {
        return Math.min(widthDefault, 48);
    }

    public void setWidthType (int widthType)
    {
        this.widthType = widthType;
    }

    public void setWidthKey (int widthKey)
    {
        this.widthKey = widthKey;
    }

    public void setWidthDefault (int widthDefault)
    {
        this.widthDefault = widthDefault;
    }

    public String getShortName ()
    {
        return shortName == null ? "" : shortName;
    }

    public void setShortName (String shortName)
    {
        this.shortName = shortName;

    }

    public void setDeftFormatted (String deftFormatted)
    {
        this.deftFormatted = deftFormatted;
    }

}
