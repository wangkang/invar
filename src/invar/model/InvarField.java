package invar.model;

import invar.InvarContext;
import java.util.LinkedList;
import java.util.List;

public class InvarField
{
    private final InvarType             type;
    private final LinkedList<InvarType> generics;
    private final String                key;
    private final String                comment;
    private final Boolean               isStructSelf;
    private final Boolean               disableSetter;

    private Boolean                     useReference  = false;
    private Boolean                     usePointer    = false;
    private String                      shortName     = "";
    private String                      defaultVal    = "";
    private String                      typeFormatted = "";
    private String                      deftFormatted = "";
    private int                         widthType     = 1;
    private int                         widthKey      = 1;
    private int                         widthDefault  = 1;

    public InvarField(InvarType type, String key, String comment, Boolean isStructSelf, Boolean disableSetter)
    {
        this.type = type;
        this.generics = new LinkedList<InvarType>();
        this.key = key;
        this.comment = comment;
        this.isStructSelf = isStructSelf;
        this.disableSetter = disableSetter;
        this.setDefault("");
    }

    public InvarType getType ()
    {
        return type;
    }

    public String getKey ()
    {
        return key;
    }

    public List<InvarType> getGenerics ()
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

    public String makeTypeFormatted (InvarContext ctx, String split, Boolean fullName)
    {
        InvarType t = type.getRedirect();
        String tName = t.getName();
        if (fullName || ctx.findTypes(t.getName(), true).size() > 1)
            tName = t.fullName(split);

        typeFormatted = tName + evalGenerics(ctx, t, split, fullName);
        return typeFormatted;
    }

    String evalGenerics (InvarContext ctx, InvarType typeBasic, String split, Boolean fullName)
    {
        if (getGenerics().size() == 0)
            return "";
        String s = typeBasic.getGeneric();
        for (InvarType t : getGenerics())
        {
            t = t.getRedirect();
            String tName = t.getName();
            if (fullName || ctx.findTypes(t.getName(), true).size() > 1)
                tName = t.fullName(split);
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
            String name = t.fullName(split);
            s = s.replaceFirst("\\?", name + t.getGeneric());
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

    public void setDefault (String defaultValue)
    {
        this.defaultVal = defaultValue;
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

    public Boolean getDisableSetter ()
    {
        return disableSetter;
    }

    public Boolean getUseReference ()
    {
        return !isStructSelf && useReference;
    }

    public void setUseReference (Boolean useReference)
    {
        this.useReference = (useReference && !isStructSelf);
        if (this.useReference)
            this.usePointer = false;
    }

    public Boolean getUsePointer ()
    {
        return isStructSelf || usePointer;
    }

    public void setUsePointer (Boolean usePointer)
    {
        this.usePointer = (isStructSelf || usePointer);
        if (this.usePointer)
            this.useReference = false;
    }

}
