package invar;

import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author wkang
 */
final public class InvarContext
{
    private final HashMap<TypeID,InvarType>    redirectTypes;
    private final HashMap<String,InvarPackage> packAll;
    private final InvarPackage                 packBuildIn;

    public InvarContext() throws Exception
    {
        packBuildIn = new InvarPackage("#INVAR#", false, 16);
        packAll = new HashMap<String,InvarPackage>();
        packAll.put(packBuildIn.getName(), packBuildIn);

        redirectTypes = new HashMap<TypeID,InvarType>();
    }

    public InvarPackage addBuildInTypes(TreeMap<TypeID,String> map)
    {
        InvarPackage pack = packBuildIn;
        Set<TypeID> keys = map.keySet();
        Iterator<TypeID> i = keys.iterator();
        while (i.hasNext())
        {
            TypeID id = i.next();
            String name = map.get(id);
            InvarType type = new InvarType(id, name, pack, name
                    + " is a build in type.");
            packBuildIn.put(type);
        }
        packAll.put(pack.getName(), pack);
        return pack;
    }

    public InvarContext typeRedefine(TypeID id, String namePack, String nameType, String generic)
    {
        if (TypeID.ENUM == id || TypeID.STRUCT == id || TypeID.PROTOCOL == id)
        {
            return this;
        }
        InvarPackage pack = packAll.get(namePack);
        if (pack == null)
        {
            pack = new InvarPackage(namePack, false, 24);
            packAll.put(namePack, pack);
        }
        InvarType t = new InvarType(id, nameType, pack, "").setGeneric(generic);
        redirectTypes.put(id, t);
        pack.put(t);
        return this;
    }

    public InvarType typeRedirect(InvarType type)
    {
        InvarType tR = type;
        if (isBuildInPack(type.getPack()))
        {
            tR = findBuildInType(type.getId());
        }
        return tR;
    }

    public InvarPackage findOrCreatePack(String name)
    {
        InvarPackage info = packAll.get(name);
        if (info == null)
        {
            info = new InvarPackage(name, true, 32);
            packAll.put(name, info);
        }
        return info;
    }

    public HashMap<String,InvarPackage> getPacks()
    {
        return packAll;
    }

    public boolean isBuildInPack(InvarPackage pack)
    {
        return pack == packBuildIn;
    }

    public InvarType findBuildInType(TypeID id)
    {
        InvarType t = redirectTypes.get(id);
        if (t == null)
        {
            t = packBuildIn.getType(id);
        }
        return t;
    }

    @SuppressWarnings ("unchecked")
    public <T extends InvarType> T findType(String name, InvarPackage pack) throws Throwable
    {
        InvarType t = pack.getType(name);
        if (t == null)
        {
            t = packBuildIn.getType(name);
            if (t != null)
            {
                t = findBuildInType(t.getId());
            }
            else
            {
                //t = findType(name);
            }
        }
        return (T)t;
    }

    @SuppressWarnings ("unchecked")
    public <T extends InvarType> T findType(String typeName) throws Throwable
    {
        Set<Entry<String,InvarPackage>> infos = getPacks().entrySet();
        Iterator<Entry<String,InvarPackage>> iter = infos.iterator();

        InvarType t = null;
        while (iter.hasNext())
        {
            Entry<String,InvarPackage> en = iter.next();
            t = findType(typeName, en.getValue());
            if (t != null)
            {
                break;
            }
        }
        return (T)t;
    }

    public StringBuilder dumpTypeAll()
    {
        StringBuilder s = new StringBuilder();
        Set<Entry<String,InvarPackage>> packNames = packAll.entrySet();
        Iterator<Entry<String,InvarPackage>> i = packNames.iterator();
        while (i.hasNext())
        {
            Entry<String,InvarPackage> en = i.next();
            InvarPackage pack = en.getValue();
            s.append(pack.getName());
            s.append("\n");
            Iterator<String> iTypeName = pack.getTypeNames().iterator();
            while (iTypeName.hasNext())
            {
                String typeName = iTypeName.next();
                InvarType type = pack.getType(typeName);
                s.append(makeFixedLenString(" ", 21, type.getPack().getName()
                        + "." + type.getName()));
                if (this.isBuildInPack(pack))
                {
                    type = findBuildInType(type.getId());
                    s.append("--->  ");
                    s.append(type.getPack().getName() + "." + type.getName());
                    s.append(type.getGeneric());
                }
                s.append("\n");
            }
            s.append(makeFixedLenString("-", 80));
            s.append("\n");
        }
        return s;
    }
    private String makeFixedLenString(String blank, Integer len, String str)
    {
        int delta = len - str.length();
        if (delta > 0)
        {
            for (int i = 0; i < delta; i++)
            {
                str += blank;
            }
        }
        return str;
    }
    private String makeFixedLenString(String blank, Integer len)
    {
        return makeFixedLenString(blank, len, "");
    }

}
