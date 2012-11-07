package invar;

import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeStruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

final public class InvarContext
{
    private final HashMap<String,InvarPackage> packAll;
    private final InvarPackage                 packBuildIn;
    private final TreeMap<String,InvarType>    structsWithAlias;

    public InvarContext() throws Exception
    {
        structsWithAlias = new TreeMap<String,InvarType>();
        packBuildIn = new InvarPackage("#INVAR#", false);
        packAll = new HashMap<String,InvarPackage>();
        packAll.put(packBuildIn.getName(), packBuildIn);
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
            pack = new InvarPackage(namePack, false);
            packAll.put(namePack, pack);
        }
        InvarType type = packBuildIn.getType(id);
        InvarType typeRedi = new InvarType(id, nameType, pack, "")
                .setGeneric(generic);
        type.setRedirect(typeRedi);
        structsWithAlias.put(type.getName(), typeRedi);
        pack.put(typeRedi);
        return this;
    }
    public InvarType ghostAdd(String namePack, String nameType, String generic)
    {
        InvarPackage pack = packAll.get(namePack);
        if (pack == null)
        {
            pack = new InvarPackage(namePack, false);
            packAll.put(namePack, pack);
        }
        InvarType t = new InvarType(TypeID.GHOST, nameType, pack, "")
                .setGeneric(generic);
        pack.put(t);
        return t;
    }

    public void ghostClear()
    {
        Iterator<String> i = packAll.keySet().iterator();
        while (i.hasNext())
        {
            InvarPackage pack = packAll.get(i.next());
            pack.clearGhostTypes();
        }
    }

    public InvarPackage findOrCreatePack(String name)
    {
        InvarPackage info = packAll.get(name);
        if (info == null)
        {
            info = new InvarPackage(name, true);
            packAll.put(name, info);
        }
        return info;
    }

    public InvarPackage getPack(String name)
    {
        return packAll.get(name);
    }

    public Iterator<String> getPackNames()
    {
        return packAll.keySet().iterator();
    }

    public List<InvarType> findTypes(String typeName)
    {
        Iterator<String> i = packAll.keySet().iterator();
        InvarType type = null;
        List<InvarType> types = new ArrayList<InvarType>();
        while (i.hasNext())
        {
            InvarPackage pack = packAll.get(i.next());
            type = pack.getType(typeName);
            if (type != null)
                types.add(type);
        }
        return types;
    }

    public InvarType findBuildInType(String typeName)
    {
        return packBuildIn.getType(typeName.toLowerCase());
    }

    public boolean isBuildInPack(InvarPackage pack)
    {
        return pack == packBuildIn;
    }

    @SuppressWarnings ("unchecked")
    public <T extends InvarType> T findType(String name, InvarPackage pack) throws Throwable
    {
        InvarType t = pack.getType(name);
        if (t == null)
        {
            t = packBuildIn.getType(name);
        }
        return (T)t;
    }

    public void aliasAdd(TypeStruct type)
    {
        structsWithAlias.put(type.getAlias(), type);
    }
    public InvarType aliasGet(String alias)
    {
        return structsWithAlias.get(alias);
    }
    public Iterator<String> aliasNames()
    {
        return structsWithAlias.keySet().iterator();
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
            Iterator<String> iTypeName = pack.getTypeNames();
            while (iTypeName.hasNext())
            {
                String typeName = iTypeName.next();
                InvarType type = pack.getType(typeName);
                s.append(makeFixedLenString(" ", 21, type.getPack().getName()
                        + "." + type.getName()));
                if (type.getRedirect() != null)
                {
                    InvarType typeR = type.getRedirect();
                    s.append("--->  ");
                    String namePack = typeR.getPack().getName();
                    if (!namePack.equals(""))
                        s.append(namePack + ".");
                    s.append(typeR.getName());
                    s.append(typeR.getGeneric());
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
