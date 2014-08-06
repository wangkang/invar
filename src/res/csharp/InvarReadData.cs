using System;
using System.IO;
using System.Collections;
using System.Collections.Generic;
using System.Xml;
using System.Text;
using System.Reflection;

namespace Invar {

public delegate void Logger<T> (T o);

public class InvarReadData
{
    static public Int32             NumFileMax = 3096;
    static public String            FileSuffix = ".xml";
    static public Boolean           Verbose = false;
    static public Logger<Object>    Logger = null;
    //
    static internal Dictionary<String,Type>  AliasBasics = null;
    static internal Dictionary<String,Type>  AliasEnums = null;
    static internal Dictionary<String,Type>  AliasStructs = null;

    internal InvarReadData ()
    {
    }

    public void ReadFromDirectory (Object o, String dir)
    {
        if (!Directory.Exists (dir))
            throw new IOException ("Directory doesn't exist: " + dir);
        List<String> files = new List<String> (20);
        RecursiveReadFile (files, new DirectoryInfo (dir));
        //InvarReadData dataParser = new InvarReadData ();
        foreach (String file in files) {
            ReadFromFilePath (o, file, Encoding.UTF8);
        }
    }

    public void ReadFromFilePath (Object o, String path)
    {
        ReadFromFilePath (o, path, Encoding.UTF8);
    }

    public void ReadFromFilePath (Object o, String path, Encoding Encoding)
    {
        if (!File.Exists (path))
            throw new IOException ("File doesn't exist: " + path);
        FileStream stream = new FileStream (path, FileMode.Open, FileAccess.Read, FileShare.None);
        StreamReader reader = new StreamReader (stream, Encoding, true);
        XmlDocument doc = new XmlDocument ();
        doc.Load (reader);
        reader.Close ();
        stream.Close ();
        XmlNode x = doc.DocumentElement;
        Log ("Read <- " + path);
        if (x.HasChildNodes) {
            ReadFromXmlNode (o, x, o.GetType ().FullName, x.Name);
        }
    }

    public void ReadFromString (Object o, String xml)
    {
        XmlDocument doc = new XmlDocument ();
        doc.LoadXml (xml);
        XmlNode x = doc.DocumentElement;
        ReadFromXmlNode (o, x, o.GetType ().FullName, x.Name);
    }

    public void ReadFromXmlNode (Object o, XmlNode n, String rule, String debug)
    {
        if (o == null)
            OnError (debug + " is null. rule: " + rule, n);
        //Type ClsO = LoadGenericClass (rule, n);
        if (o is IList)
            ParseVec (o as IList, n, rule, debug);
        else if (o is IDictionary)
            ParseMap (o as IDictionary, n, rule, debug);
        else
            ParseStruct (o, n, rule, debug);
    }

    private void ParseStruct (Object o, XmlNode n, String rule, String debug)
    {
        Type ClsO = LoadGenericClass (rule, n);
        if (o.GetType ().FullName != ClsO.FullName)
            OnError ("Object does not matches this rule: " + rule, n);
        XmlAttributeCollection attrs = n.Attributes;
        if (attrs == null) {
            OnError ("Node unavailable: " + rule, n);
            return;
        }
        int attrsLen = attrs.Count;
        for (int i = 0; i < attrsLen; i++) {
            XmlNode x = attrs [i];
            String key = x.Name;
            if (key.IndexOf (":") >= 0)
                continue;
            String ruleX = GetRule (ClsO, key, x);
            if (ruleX == null || ruleX == String.Empty)
                continue;
            Type ClsX = LoadGenericClass (ruleX, x);
            String vStr = x.Value;
            Object v = ParseSimple (ruleX, ClsX, vStr, debug + '.' + key, n);
            InvokeSetter (v, key, o, n);
        }
        XmlNodeList children = n.ChildNodes;
        int len = children.Count;
        for (int i = 0; i < len; i++) {
            XmlNode x = children.Item (i);
            if (XmlNodeType.Element != x.NodeType)
                continue;
            String key = x.Name;
            String ruleX = GetRule (ClsO, key, n);
            if (ruleX == null || ruleX == String.Empty)
                continue;
            Type ClsX = LoadGenericClass (ruleX, x);
            String vStr = null;
            Object v = null;
            if (IsSimple (ClsX)) {
                vStr = GetAttrOptional (x, ATTR_VALUE);
                v = ParseSimple (ruleX, ClsX, vStr, debug + '.' + key, x);
                InvokeSetter (v, key, o, x);
            } else {
                v = InvokeGetter (key, o, x);
                if (v == null) {
                    v = Activator.CreateInstance (ClsX);
                    InvokeSetter (v, key, o, x);
                }
                ReadFromXmlNode (v, x, ruleX, debug + '.' + key);
            }
        }
    }

    private void ParseVec (IList list, XmlNode n, String rule, String debug)
    {
        String R = RuleRight (rule);
        if (R == null)
            OnError ("Unexpected type: " + rule, n);
        Type Cls = LoadGenericClass (R, n);
        XmlNodeList children = n.ChildNodes;
        int len = children.Count;
        for (int i = 0; i < len; i++) {
            XmlNode vn = children.Item (i);
            if (XmlNodeType.Element != vn.NodeType)
                continue;
            Object v = ParseGenericChild (vn, Cls, R, debug + "[" + list.Count + "]");
            list.Add (v);
        }
    }

    private void ParseMap (IDictionary map, XmlNode n, String rule, String debug)
    {
        String R = RuleRight (rule);
        if (R == null)
            OnError ("Unexpected type: " + rule, n);
        String[] typeNames = R.Split (GENERIC_SPLIT.ToCharArray ());
        if (typeNames.Length != 2)
            OnError ("Unexpected type: " + rule, n);
        Type ClsK = LoadGenericClass (typeNames [0], n);
        Type ClsV = LoadGenericClass (typeNames [1], n);

        List<XmlNode> nodes = new List<XmlNode> ();
        XmlNodeList children = n.ChildNodes;
        int len = children.Count;
        for (int i = 0; i < len; i++) {
            XmlNode cn = children.Item (i);
            if (XmlNodeType.Element != cn.NodeType)
                continue;
            nodes.Add (cn);
        }

        len = nodes.Count;
        if (IsSimple (ClsK)) {
            for (int i = 0; i < len; i++) {
                XmlNode vn = nodes [i];
                String s = GetAttr (vn, ATTR_MAP_KEY);
                Object k = ParseSimple (typeNames [0], ClsK, s, debug + ".k", vn);
                Object v = ParseGenericChild (vn, ClsV, typeNames [1], debug + ".v");
                if (!map.Contains (k))
                    map.Add (k, v);
                else
                    map [k] = v;
            }
        } else {
            if ((0x01 & len) != 0)
                OnError ("Invaid amount of children: " + len, n);
            for (int i = 0; i < len; i += 2) {
                XmlNode kn = nodes [i];
                XmlNode vn = nodes [i + 1];
                Object k = ParseGenericChild (kn, ClsK, typeNames [0], debug + ".k");
                Object v = ParseGenericChild (vn, ClsV, typeNames [1], debug + ".v");
                if (!map.Contains (k))
                    map.Add (k, v);
                else
                    map [k] = v;
            }
        }
    }

    private Object ParseGenericChild (XmlNode cn, Type Cls, String rule, String debug)
    {
        if (IsSimple (Cls))
            return ParseSimple (rule, Cls, GetAttr (cn, ATTR_VALUE), debug, cn);
        else {
            try {
                //Cls.IsGenericType
                Object co = Activator.CreateInstance (Cls);
                ReadFromXmlNode (co, cn, rule, debug);
                return co;
            } catch (Exception e) {
                OnError (e.Message + "\n" + rule, cn);
            } finally {
            }
            return null;
        }
    }

#region static private

    static private String GENERIC_LEFT = "<";
    static private String GENERIC_RIGHT = ">";
    static private String GENERIC_SPLIT = ",";
    static private String PREFIX_SETTER = "Set";
    static private String PREFIX_GETTER = "Get";
    static private String ATTR_MAP_KEY = "key";
    static private String ATTR_VALUE = "value";
    //
    static private Dictionary<Type,Dictionary<String,MethodInfo>>
            mapClassSetters = new  Dictionary<Type,Dictionary<String,MethodInfo>> ();
    static private Dictionary<Type,Dictionary<String,MethodInfo>>
            mapClassGetters = new  Dictionary<Type,Dictionary<String,MethodInfo>> ();

    static private void RecursiveReadFile (List<String> all, DirectoryInfo parent)
    {
        if (all.Count > NumFileMax)
            return;
        FileInfo[] files = parent.GetFiles ();
        foreach (FileInfo file in files) {
            if (file.Name.StartsWith ("."))
                continue;
            if (file.Name.StartsWith ("_"))
                continue;
            if (!file.Name.EndsWith (FileSuffix))
                continue;
            all.Add (file.FullName);
        }
        DirectoryInfo[] dirs = parent.GetDirectories ();
        foreach (DirectoryInfo dir in dirs) {
            if (dir.Name.StartsWith ("."))
                continue;
            if (dir.Name.StartsWith ("_"))
                continue;
            RecursiveReadFile (all, dir);
        }
    }

    static private Type LoadGenericClass (String rule, XmlNode n)
    {
        Type Cls = RecursiveGeneric (rule);
        if (Cls == null)
            OnError ("No Class matches this rule: " + rule, n);
        return Cls;
    }

    static private Type RecursiveGeneric (String rule)
    {
        Type Cls = GetClassByAlias (rule);
        if (typeof(List<>) == Cls) {
            String R = RuleRight (rule);
            if (R == null)
                return Cls;
            Type ClsV = RecursiveGeneric (R);
            return Cls.MakeGenericType (new Type[]{ ClsV });
        } else if (typeof(Dictionary<,>) == Cls) {
            String R = RuleRight (rule);
            if (R == null)
                return Cls;
            String[] Rs = R.Split (GENERIC_SPLIT.ToCharArray ());
            if (Rs.Length != 2)
                return Cls;
            Type ClsK = RecursiveGeneric (Rs [0]);
            Type ClsV = RecursiveGeneric (Rs [1]);
            return Cls.MakeGenericType (new Type[]{ ClsK,ClsV});
        } else {
            return Cls;
        }
    }

    static private Type GetClassByAlias (String rule)
    {
        String name = RuleLeft (rule);
        Type Cls = null;
        if (AliasBasics.ContainsKey (name))
            Cls = AliasBasics [name];
        else if (AliasEnums.ContainsKey (name))
            Cls = AliasEnums [name];
        else if (AliasStructs.ContainsKey (name))
            Cls = AliasStructs [name];
        else
            Cls = Type.GetType (name, false, false);
        return Cls;
    }

    static private String RuleLeft (String rule)
    {
        String name = rule;
        if (rule.IndexOf (GENERIC_LEFT) >= 0) {
            name = rule.Substring (0, rule.IndexOf (GENERIC_LEFT));
        }
        return name;
    }

    static private String RuleRight (String rule)
    {
        int iBegin = rule.IndexOf (GENERIC_LEFT) + 1;
        int iEnd = rule.LastIndexOf (GENERIC_RIGHT);
        int length = iEnd - iBegin;
        if (iBegin > 0 && length > 0) {
            return rule.Substring (iBegin, length);
        }
        return null;
    }

    static private String GetAttr (XmlNode n, String name)
    {
        String v = GetAttrOptional (n, name);
        if (String.Empty == v)
            OnError ("Attribute '" + name + "' is required.", n);
        return v;
    }

    static private String GetAttrOptional (XmlNode n, String name)
    {
        String v = String.Empty;
        XmlAttributeCollection attrs = n.Attributes;
        if (attrs == null)
            return v;
        XmlNode node = attrs.GetNamedItem (name);
        if (node != null)
            v = node.Value;
        return v;
    }

    static private String UpperHeadChar (String s)
    {
        return s.Substring (0, 1).ToUpper () + s.Substring (1, s.Length - 1);
    }

    static private T GetMethodAnnotation<T> (MemberInfo method)
    {
        Object[] annos = method.GetCustomAttributes (typeof(T), false);
        if (annos.Length > 0)
            return (T)annos [0];
        return default(T);
    }

    static private Dictionary<String,MethodInfo> GetSetters (Type ClsO)
    {
        if (!mapClassSetters.ContainsKey (ClsO)) {
            MethodInfo[] meths = ClsO.GetMethods ();
            Dictionary<String,MethodInfo> methods = new Dictionary<String,MethodInfo> ();
            foreach (MethodInfo method in meths) {
                if (method.Name.StartsWith (PREFIX_SETTER)) {
                    InvarRule anno = GetMethodAnnotation<InvarRule> (method);
                    if (anno != null) {
                        methods.Add (method.Name, method);
                        //Log (ClsO + " -> " + method.Name);
                        String shortName = anno.S;
                        if (shortName != null && shortName != String.Empty)
                            methods.Add (shortName, method);
                    }
                }
            }
            mapClassSetters.Add (ClsO, methods);
        }
        return mapClassSetters [ClsO];
    }

    static private Dictionary<String,MethodInfo> GetGetters (Type ClsO)
    {
        if (!mapClassGetters.ContainsKey (ClsO)) {
            MethodInfo[] meths = ClsO.GetMethods ();
            Dictionary<String,MethodInfo> methods = new Dictionary<String,MethodInfo> ();
            foreach (MethodInfo method in meths) {
                if (method.Name.StartsWith (PREFIX_GETTER)) {
                    InvarRule anno = GetMethodAnnotation<InvarRule> (method);
                    if (anno != null) {
                        methods.Add (method.Name, method);
                        //log (ClsO + " -> " + method.Name);
                        String shortName = anno.S;
                        if (shortName != null && shortName != String.Empty)
                            methods.Add (shortName, method);
                    }
                }
            }
            mapClassGetters.Add (ClsO, methods);
        }
        return mapClassGetters [ClsO];
    }

    static private String GetRule (Type ClsO, String key, XmlNode n)
    {
        MethodInfo method = GetMethod (key, PREFIX_GETTER, GetGetters (ClsO), ClsO, n);
        if (method == null)
            return null;
        String rule = null;
        InvarRule anno = GetMethodAnnotation<InvarRule> (method);
        if (anno != null && anno.T != String.Empty)
            rule = anno.T;
        return rule;
    }

    static private Object InvokeGetter (String key, Object o, XmlNode n)
    {
        Type ClsO = o.GetType ();
        MethodInfo method = GetMethod (key, PREFIX_GETTER, GetGetters (ClsO), ClsO, n);
        if (method != null) {
            return method.Invoke (o, null);
        }
        return null;
    }

    static private void InvokeSetter (Object val, String key, Object o, XmlNode n)
    {
        Type ClsO = o.GetType ();
        MethodInfo method = GetMethod (key, PREFIX_SETTER, GetSetters (ClsO), ClsO, n);
        if (method != null) {
            method.Invoke (o, new Object[]{val});
        }
    }

    static private MethodInfo GetMethod (String key, String prefix,
                                         Dictionary<String,MethodInfo> map, Type ClsO, XmlNode n)
    {
        MethodInfo method = null;
        if (map.ContainsKey (key))
            method = map [key];
        if (method == null) {
            String nameGetter = prefix + UpperHeadChar (key);
            if (map.ContainsKey (nameGetter))
                method = map [nameGetter];
        }
        if (method == null) {
            if (key != ATTR_MAP_KEY)
                OnError ("No method named '" + key + "' in " + ClsO, n);
            return null;
        }
        return method;

    }

#endregion

#region Parse Simple Type

    static private Boolean IsSimple (Type t)
    {
        if (t.IsEnum)
            return true;
        if (t == typeof(String))
            return true;
        if (t == typeof(Boolean))
            return true;
        if (t == typeof(Single))
            return true;
        if (t == typeof(Double))
            return true;
        if (t == typeof(SByte))
            return true;
        if (t == typeof(Int16))
            return true;
        if (t == typeof(Int32))
            return true;
        if (t == typeof(Int64))
            return true;
        if (t == typeof(Byte))
            return true;
        if (t == typeof(UInt16))
            return true;
        if (t == typeof(UInt32))
            return true;
        if (t == typeof(UInt64))
            return true;
        return false;
    }

    static private Object ParseSimple (String rule, Type t, String s, String debug, XmlNode x)
    {
        Object arg = null;
        if (t == typeof(String)) {
            arg = s;
        } else if (t == typeof(Boolean)) {
            arg = Boolean.Parse (s);
        } else if (t.IsEnum) {
             if (Enum.IsDefined (t, s))
                arg = Enum.Parse (t, s, false);
            else
                OnError ("'" + s + "' is a bad enum value. ", x);
        } else {
            switch (rule) {
            case "int8":
                arg = (SByte)CheckNumber (s, -0x80L, 0x7FL, debug, x);
                break;
            case "int16":
                arg = (Int16)CheckNumber (s, -0x8000L, 0x7FFFL, debug, x);
                break;
            case "int32":
                arg = (Int32)CheckNumber (s, -0x80000000L, 0x7FFFFFFFL, debug, x);
                break;
            case "int64":
                arg = CheckNumber (s, Int64.MinValue, Int64.MaxValue, debug, x);
                break;
            case "uint8":
                arg = (Byte)CheckNumber (s, 0UL, 0xFFUL, debug, x);
                break;
            case "uint16":
                arg = (UInt16)CheckNumber (s, 0UL, 0xFFFFUL, debug, x);
                break;
            case "uint32":
                arg = (UInt32)CheckNumber (s, 0UL, 0xFFFFFFFFUL, debug, x);
                break;
            case "uint64":
                arg = CheckNumber (s, UInt64.MinValue, UInt64.MaxValue, debug, x);
                break;
            case "float":
                arg = CheckNumber (s, Single.MinValue, Single.MaxValue, debug, x);
                break;
            case "double":
                arg = CheckNumber (s, Double.MinValue, Double.MaxValue, debug, x);
                break;
            default:
                break;
            }
        }
        if (arg == null)
            OnError ("'" + s + "' is not a simple value.", x);
        if (Verbose) {
            StringWriter code = new StringWriter ();
            code.Write (FixedLen (40, debug));
            code.Write (" : ");
            code.Write (FixedLen (32, rule));
            code.Write (" : ");
            code.Write (arg);
            Log (code);
        }
        return arg;
    }

    static private String FixedLen (Int32 len, String str)
    {
        String blank = " ";
        int delta = len - str.Length;
        if (delta > 0)
            for (int i = 0; i < delta; i++)
                str += blank;
        return str;
    }

    static private Int64 CheckNumber (String s, Int64 min, Int64 max, String debug, XmlNode x)
    {
        Int64 v = Int64.Parse (s);
        if (v < min || v > max) {
            OnError (debug + " = " + v + ". Number is out of range [" + min + ", " + max + "]", x);
        }
        return v;
    }

    static private UInt64 CheckNumber (String s, UInt64 min, UInt64 max, String debug, XmlNode x)
    {
        UInt64 v = UInt64.Parse (s);
        if (v > max) {
            OnError (debug + " = " + v + ". Number is out of range [" + min + ", " + max + "]", x);
        }
        return v;
    }

    static private Single CheckNumber (String s, Single min, Single max, String debug, XmlNode x)
    {
        Single v = Single.Parse (s);
        if (v < min || v > max) {
            OnError (debug + " = " + v + ". Number is out of range [" + min + ", " + max + "]", x);
        }
        return v;
    }

    static private Double CheckNumber (String s, Double min, Double max, String debug, XmlNode x)
    {
        Double v = Double.Parse (s);
        if (v < min || v > max) {
            OnError (debug + " = " + v + ". Number is out of range [" + min + ", " + max + "]", x);
        }
        return v;
    }

    static private String FormatXmlNode (XmlNode n)
    {
        XmlAttributeCollection attrs = n.Attributes;
        StringBuilder code = new StringBuilder ();
        code.Append ("<" + n.Name);
        int len = attrs != null ? attrs.Count : 0;
        for (int i = 0; i < len; i++) {
            XmlAttribute a = attrs [i];
            code.Append (" " + a.Name + "=\"" + a.Value + "\"");
        }
        code.Append (" />");
        return code.ToString ();
    }

    static private void OnError (String hint, XmlNode n)
    {
        throw new Exception (" " + hint + "\n  " + FormatXmlNode (n) + "\n  " + n.BaseURI);
    }

    static private void Log (Object txt)
    {
        if (Logger != null) {
            Logger (txt);
        }
    }

#endregion

}
}