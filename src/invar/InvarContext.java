package invar;

import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeStruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

final public class InvarContext
{
    private final InvarPackage                 packBuildIn;
    private final HashMap<String,InvarPackage> packAll;
    private final HashMap<String,InvarType>    typeWithAlias;
    private String                             structRootAlias = "root";
    private TypeStruct                         structRoot;
    private String                             ruleDir;

    public InvarContext() throws Exception
    {
        typeWithAlias = new LinkedHashMap<String,InvarType>();
        packBuildIn = new InvarPackage("", false);
        packAll = new HashMap<String,InvarPackage>();
        packAll.put(packBuildIn.getName(), packBuildIn);

    }

    public InvarPackage addBuildInTypes (TreeMap<TypeID,String> map)
    {
        InvarPackage pack = packBuildIn;
        Set<TypeID> keys = map.keySet();
        Iterator<TypeID> i = keys.iterator();
        while (i.hasNext())
        {
            TypeID id = i.next();
            String name = map.get(id);
            InvarType type = new InvarType(id, name, pack, name + "[buildin]");
            packBuildIn.put(type);
            if (TypeID.LIST == id)
                type.setGeneric("<?>");
            else if (TypeID.MAP == id)
                type.setGeneric("<?,?>");
        }
        packAll.put(pack.getName(), pack);
        return pack;
    }

    public InvarContext typeRedefine (TypeID id, String namePack, String nameType, String generic)
    {
        return typeRedefine(id, namePack, nameType, generic, "", "", "");
    }

    public InvarContext typeRedefine (TypeID id,
                                      String namePack,
                                      String nameType,
                                      String generic,
                                      String initValue,
                                      String initPrefix,
                                      String initSuffix)
    {
        if (TypeID.ENUM == id || TypeID.STRUCT == id || TypeID.PROTOCOL == id)
        {
            return this;
        }
        InvarType type = packBuildIn.getType(id);
        InvarType typeGhost = ghostAdd(namePack, nameType, generic, id);
        type.setRedirect(typeGhost);
        type.setInitValue(initValue);
        type.setInitSuffix(initSuffix);
        type.setInitPrefix(initPrefix);
        typeWithAlias.put(type.getName(), typeGhost);
        return this;
    }

    public InvarType ghostAdd (String namePack, String nameType, String generic)
    {
        return ghostAdd(namePack, nameType, generic, TypeID.STRUCT);
    }

    public InvarType ghostAdd (String namePack, String nameType, String generic, TypeID realId)
    {
        InvarPackage pack = packAll.get(namePack);
        if (pack == null)
        {
            pack = new InvarPackage(namePack, false);
            packAll.put(namePack, pack);
        }
        InvarType t = new InvarType(TypeID.GHOST, nameType, pack, "");
        t.setGeneric(generic);
        t.setRealId(realId);
        pack.put(t);
        return t;
    }

    public void ghostClear ()
    {
        Iterator<String> i = packAll.keySet().iterator();
        while (i.hasNext())
        {
            InvarPackage pack = packAll.get(i.next());
            pack.clearGhostTypes();
            if (pack.size() == 0)
                i.remove();
        }
    }

    public InvarPackage findOrCreatePack (String name)
    {
        InvarPackage info = packAll.get(name);
        if (info == null)
        {
            info = new InvarPackage(name, true);
            packAll.put(name, info);
        }
        return info;
    }

    public InvarPackage getPack (String name)
    {
        return packAll.get(name);
    }

    public Iterator<String> getPackNames ()
    {
        return packAll.keySet().iterator();
    }

    public List<InvarType> findTypes (String typeName)
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

    public InvarType findBuildInType (String typeName)
    {
        return packBuildIn.getType(typeName.toLowerCase());
    }

    public InvarType findBuildInType (TypeID id)
    {
        return packBuildIn.getType(id);
    }

    public boolean isBuildInPack (InvarPackage pack)
    {
        return pack == packBuildIn;
    }

    public void aliasAdd (TypeEnum type)
    {
        typeWithAlias.put(type.getAlias(), type);
    }

    public void aliasAdd (TypeStruct type)
    {
        typeWithAlias.put(type.getAlias(), type);
        if (type.getAlias().equals(structRootAlias))
            structRoot = type;
    }

    public InvarType aliasGet (String alias)
    {
        return typeWithAlias.get(alias);
    }

    public Iterator<String> aliasNames ()
    {
        return typeWithAlias.keySet().iterator();
    }

    public TypeStruct getStructRoot ()
    {
        return structRoot;
    }

    public String getStructRootAlias ()
    {
        return structRootAlias;
    }

    public void setRuleDir (String path)
    {
        ruleDir = path;
    }

    public String getRuleDir ()
    {
        return ruleDir;
    }

}
