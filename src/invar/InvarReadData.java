package invar;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final public class InvarReadData
{
    static public Boolean                  verbose      = false;
    static public HashMap<String,Class<?>> aliasBasics  = null;
    static public HashMap<String,Class<?>> aliasEnums   = null;
    static public HashMap<String,Class<?>> aliasStructs = null;

    static public void start(Object root, String path, String suffix)
        throws Exception
    {
        File file = new File(path);
        if (!file.exists())
            throw new IOException("Path doesn't exist:\n" + file.getAbsolutePath());
        if (aliasBasics == null)
            throw new Exception("InvarReadData.aliasBasics is null");
        if (aliasEnums == null)
            throw new Exception("InvarReadData.aliasEnums is null");
        if (aliasStructs == null)
            throw new Exception("InvarReadData.aliasStructs is null");
        List<File> files = new ArrayList<File>();
        recursiveReadFile(files, file, suffix);
        for (File f : files)
        {
            log("Read <- " + f.getAbsolutePath());
            Document doc = DocumentBuilderFactory.newInstance()
                                                 .newDocumentBuilder().parse(f);
            if (!doc.hasChildNodes())
                return;
            Node nRoot = doc.getFirstChild();
            new InvarReadData(f.getAbsolutePath()).parse(root, nRoot);
        }
    }

    static private final String GENERIC_LEFT    = "<";
    static private final String GENERIC_RIGHT   = ">";
    static private final String GENERIC_SPLIT   = ", ";
    static private final String PREFIX_SETTER   = "set";
    static private final String PREFIX_GETTER   = "get";

    static private final String ATTR_MAP_KEY    = "key";
    static private final String ATTR_FIELD_NAME = "var";
    static private final String ATTR_VALUE      = "value";

    static private String upperHeadChar(String s)
    {
        return s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
    }

    static private String fixedLen(Integer len, String str)
    {
        String blank = " ";
        int delta = len - str.length();
        if (delta > 0)
            for (int i = 0; i < delta; i++)
                str += blank;
        return str;
    }

    static private void recursiveReadFile(List<File> all, File file, String suffix)
    {
        if (all.size() > 1024)
            return;
        if (file.isFile())
            all.add(file);
        else if (file.isDirectory())
        {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++)
                recursiveReadFile(all, files[i], suffix);
        }
        else
        {
        }
    }

    static private boolean isSimpleValue(Class<?> vType)
    {
        if (String.class == vType)
            return true;
        else if (Boolean.class == vType)
            return true;
        else if (Byte.class == vType)
            return true;
        else if (Short.class == vType)
            return true;
        else if (Integer.class == vType)
            return true;
        else if (Long.class == vType)
            return true;
        else if (Float.class == vType)
            return true;
        else if (Double.class == vType)
            return true;
        else if (aliasEnums.containsValue(vType))
            return true;
        else
            return false;
    }

    private static Object parseEnumObject(Class<?> type, String s)
        throws Exception
    {
        Integer v = Integer.parseInt(s);
        Object o = null;
        Method[] mets = type.getMethods();
        for (Method m : mets)
        {
            if (!m.getName().equals("parse"))
                continue;
            o = m.invoke(type, v);
            break;
        }
        return o;
    }

    static private String[] parseGenericTypes(String T)
    {
        int iBegin = T.indexOf(GENERIC_LEFT) + 1;
        int iEnd = T.lastIndexOf(GENERIC_RIGHT);
        if (iBegin > 0 && iEnd > iBegin)
        {
            String substr = T.substring(iBegin, iEnd);
            return substr.split(GENERIC_SPLIT);
        }
        return null;
    }

    static private String getAttrOptional(Node node, String name)
    {
        String v = "";
        NamedNodeMap attrs = node.getAttributes();
        if (attrs == null)
            return v;
        Node n = attrs.getNamedItem(name);
        if (n != null)
            v = n.getNodeValue();
        return v;
    }

    static private String formatXmlNode(Node n)
    {
        NamedNodeMap attrs = n.getAttributes();
        StringBuilder code = new StringBuilder();
        code.append("<" + n.getNodeName());
        int len = attrs != null ? attrs.getLength() : 0;
        for (int i = 0; i < len; i++)
        {
            Node a = attrs.item(i);
            code.append(" " + a.toString());
        }
        code.append(" />");
        return code.toString();
    }

    static private void log(Object txt)
    {
        System.out.println(txt);
    }

    private String path;

    public InvarReadData(String path)
    {
        this.path = path;
    }

    public void parse(Object o, Node n) throws Exception
    {
        parse(o, n, "", "");
    }

    @SuppressWarnings ("unchecked")
    private void parse(Object o, Node n, String T, String debug)
        throws Exception
    {
        if (o instanceof LinkedList)
            parseVec((LinkedList<Object>)o, n, T, debug);
        else if (o instanceof HashMap)
            parseMap((HashMap<Object,Object>)o, n, T, debug);
        else
            parseStruct(o, n, debug);
    }

    private void parseStruct(Object o, Node n, String debug) throws Exception
    {
        //log("parseStruct() -------- " + o.getClass().getName());
        Class<?> ClsO = o.getClass();
        validateNode(n, ClsO);
        HashMap<String,List<Node>> mapKeyNode = mergeAttrsAndChildren(n);
        Iterator<String> i = mapKeyNode.keySet().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            if (ATTR_FIELD_NAME.equals(key))
                continue;
            for (Node cn : mapKeyNode.get(key))
            {
                HashMap<String,Method> mapGetters = getGetters(o.getClass());
                String getterName = PREFIX_GETTER + upperHeadChar(key);
                Method getter = mapGetters.get(getterName);
                if (getter == null)
                    onError("No getter named \"" + getterName + "\" in " + o.getClass(), cn);

                Class<?> vType = getter.getReturnType();
                Object co = null;
                if (isSimpleValue(vType))
                {
                    String s = null;
                    if (Node.ATTRIBUTE_NODE == cn.getNodeType())
                        s = cn.getNodeValue();
                    else if (Node.ELEMENT_NODE == cn.getNodeType())
                    {
                        s = getAttrOptional(cn, ATTR_VALUE);
                        validateNode(cn, vType);
                    }
                    co = parseSimple(vType, s, cn, debug + "." + key);
                    invokeSetter(key, o, co, cn);
                }
                else
                {
                    co = getter.invoke(o);
                    if (co == null)
                    {
                        co = vType.newInstance();
                        invokeSetter(key, o, co, cn);
                    }
                    validateNode(cn, vType);
                    String T = getter.getGenericReturnType().toString();
                    parse(co, cn, T, debug + "." + key);
                }
            }
        }
    }

    private void invokeSetter(String key, Object o, Object value, Node n)
        throws Exception
    {
        HashMap<String,Method> mapSetters = getSetters(o.getClass());
        String setterName = PREFIX_SETTER + upperHeadChar(key);
        Method setter = mapSetters.get(setterName);
        if (setter == null)
        {
            onError("No setter named \"" + setterName + "()\" in " + o.getClass(), n);
        }
        InvarNum anno = setter.getAnnotation(InvarNum.class);
        if (anno != null)
        {
            long v = Long.parseLong(value.toString());
            if (v > anno.max() || v < anno.min())
            {
                onError("Number out of range: " + anno.min() + " - " + anno.max(), n);
            }
        }
        setter.invoke(o, value);
    }

    private void parseVec(LinkedList<Object> list, Node n, String T, String debug)
        throws Exception
    {
        //log("parseVec() -------- " + T);
        String[] typeNames = parseGenericTypes(T);
        if (typeNames == null || typeNames.length != 1)
            onError("Unexpected type: " + T.toString(), n);
        Class<?> Cls = loadGenericClass(typeNames[0]);
        NodeList children = n.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++)
        {
            Node vn = children.item(i);
            if (Node.ELEMENT_NODE != vn.getNodeType())
                continue;
            Object v = parseGenericChild(vn, Cls, typeNames[0], debug + "[" + list.size() + "]");
            list.add(v);
        }
    }

    private void parseMap(HashMap<Object,Object> map, Node n, String T, String debug)
        throws Exception
    {
        //log("InvarParseXml.parseMap() -------- " + T);
        String[] typeNames = parseGenericTypes(T);
        if (typeNames.length != 2)
            onError("Unexpected type: " + T.toString(), n);
        Class<?> ClsK = loadGenericClass(typeNames[0]);
        Class<?> ClsV = loadGenericClass(typeNames[1]);
        List<Node> children = elementNodes(n);
        int len = children.size();
        if (isSimpleValue(ClsK))
        {
            for (int i = 0; i < len; i++)
            {
                Node vn = children.get(i);
                String s = getAttr(vn, ATTR_MAP_KEY);
                Object k = parseSimple(ClsK, s, vn, debug + ".k");
                Object v = parseGenericChild(vn, ClsV, typeNames[1], debug + ".v");
                map.put(k, v);
            }
        }
        else
        {
            if ((0x01 & len) != 0)
                onError("Invaid amount of children: " + len, n);
            for (int i = 0; i < len; i += 2)
            {
                Node kn = children.get(i);
                Node vn = children.get(i + 1);
                Object k = parseGenericChild(kn, ClsK, typeNames[0], debug + ".k");
                Object v = parseGenericChild(vn, ClsV, typeNames[1], debug + ".v");
                map.put(k, v);
            }
        }
    }

    private Object parseGenericChild(Node cn, Class<?> Cls, String T, String debug)
        throws Exception
    {
        validateNode(cn, Cls);
        if (isSimpleValue(Cls))
            return parseSimple(Cls, getAttr(cn, ATTR_VALUE), cn, debug);
        else
        {
            Object co = Cls.newInstance();
            parse(co, cn, T, debug);
            return co;
        }
    }

    private Class<?> loadGenericClass(String T) throws ClassNotFoundException
    {
        String name = T;
        if (T.indexOf(GENERIC_LEFT) >= 0)
        {
            name = T.substring(0, T.indexOf(GENERIC_LEFT));
        }
        return ClassLoader.getSystemClassLoader().loadClass(name);
    }

    private Object parseSimple(Class<?> vType, String s, Node n, String debug)
        throws Exception
    {
        Object arg = null;
        if (String.class == vType)
            arg = s;
        else if (Byte.class == vType)
            arg = Byte.decode(s);
        else if (Short.class == vType)
            arg = Short.decode(s);
        else if (Integer.class == vType)
            arg = Integer.decode(s);
        else if (Long.class == vType)
            arg = Long.decode(s);
        else if (Float.class == vType)
            arg = Float.valueOf(s);
        else if (Double.class == vType)
            arg = Double.valueOf(s);
        else if (Boolean.class == vType)
            arg = Boolean.valueOf(s);
        else if (aliasEnums.containsValue(vType))
        {
            arg = parseEnumObject(vType, s);
            if (arg == null)
                onError("Enum value is invalid.", n);
        }
        else
        {
            onError("Not a simple value.", n);
        }
        if (verbose)
        {
            StringBuilder code = new StringBuilder();
            code.append(fixedLen(32, debug));
            code.append(" : ");
            code.append(fixedLen(16, vType.getSimpleName()));
            code.append(" : ");
            code.append(arg);
            log(code);
        }
        return arg;
    }

    private Class<?> validateNode(Node n, Class<?> ClsO) throws Exception
    {
        String name = n.getNodeName();
        Class<?> ClsN = aliasBasics.get(name);
        if (ClsN == null)
            ClsN = aliasEnums.get(name);
        if (ClsN == null)
            ClsN = aliasStructs.get(name);
        if (ClsN == null)
            onError("\nNode name \"" + name + "\" is not a correct alias.", n);
        if (ClsN != ClsO)
            onError("\nRequire: " + ClsN + "\nCurrent: " + ClsO, n);
        return ClsN;
    }

    private HashMap<String,List<Node>> mergeAttrsAndChildren(Node n)
        throws Exception
    {
        HashMap<String,List<Node>> mapKeyNode = new LinkedHashMap<String,List<Node>>();
        NamedNodeMap attrs = n.getAttributes();
        int attrsLen = attrs.getLength();
        for (int i = 0; i < attrsLen; i++)
        {
            Node cn = attrs.item(i);
            String key = cn.getNodeName();
            if (mapKeyNode.containsKey(key))
                mapKeyNode.get(key).add(cn);
            else
            {
                List<Node> list = new ArrayList<Node>();
                list.add(cn);
                mapKeyNode.put(key, list);
            }
        }
        NodeList children = n.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++)
        {
            Node cn = children.item(i);
            if (Node.ELEMENT_NODE != cn.getNodeType())
                continue;
            String key = getAttr(cn, ATTR_FIELD_NAME);
            if (mapKeyNode.containsKey(key))
                mapKeyNode.get(key).add(cn);
            else
            {
                List<Node> list = new ArrayList<Node>();
                list.add(cn);
                mapKeyNode.put(key, list);
            }
        }
        return mapKeyNode;
    }

    private List<Node> elementNodes(Node n)
    {
        List<Node> nodes = new ArrayList<Node>();
        NodeList children = n.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++)
        {
            Node cn = children.item(i);
            if (Node.ELEMENT_NODE != cn.getNodeType())
                continue;
            nodes.add(cn);
        }
        return nodes;
    }

    private String getAttr(Node n, String name) throws Exception
    {
        String v = getAttrOptional(n, name);
        if (v.equals(""))
            onError("Attribute '" + name + "' is required.", n);
        return v;
    }

    private void onError(String hint, Node n) throws Exception
    {
        throw new Exception("\n" + hint + "\n" + formatXmlNode(n) + "\n" + path);
    }

    static private final//
    HashMap<Class<?>,HashMap<String,Method>> mapClassSetters = new HashMap<Class<?>,HashMap<String,Method>>();

    static private final//
    HashMap<Class<?>,HashMap<String,Method>> mapClassGetters = new HashMap<Class<?>,HashMap<String,Method>>();

    static private HashMap<String,Method> getSetters(Class<?> ClsO)
    {
        HashMap<String,Method> methods = mapClassSetters.get(ClsO);
        if (methods == null)
        {
            Method[] meths = ClsO.getMethods();
            methods = new HashMap<String,Method>();
            for (Method method : meths)
            {
                if (method.getName().startsWith(PREFIX_SETTER))
                    methods.put(method.getName(), method);
            }
            mapClassSetters.put(ClsO, methods);
        }
        return methods;
    }

    static private HashMap<String,Method> getGetters(Class<?> ClsO)
    {
        HashMap<String,Method> methods = mapClassGetters.get(ClsO);
        if (methods == null)
        {
            Method[] meths = ClsO.getMethods();
            methods = new HashMap<String,Method>();
            for (Method method : meths)
            {
                if (method.getName().startsWith(PREFIX_GETTER))
                    methods.put(method.getName(), method);
            }
            mapClassGetters.put(ClsO, methods);
        }
        return methods;
    }
}