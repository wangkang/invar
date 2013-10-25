package invar;

import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeProtocol;
import invar.model.TypeStruct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

abstract public class InvarWrite
{

    abstract protected Boolean beforeWrite (InvarContext ctx);

    abstract protected String codeEnum (TypeEnum type);

    abstract protected String codeStruct (TypeStruct type);

    abstract protected String codeRuntime (InvarType type);

    final private InvarContext                context;
    final private File                        dirRoot;
    final private HashMap<InputStream,String> exportFiles;

    public InvarWrite(InvarContext context, String dirRootPath)
    {
        File file = new File(dirRootPath);
        if (file.exists())
            deleteDirs(dirRootPath);
        this.dirRoot = file;
        this.context = context;
        this.exportFiles = new HashMap<InputStream,String>();
    }

    final public void write (String suffix) throws Throwable
    {
        if (getContext() == null)
            return;
        if (dirRoot == null)
            return;
        Boolean bool = beforeWrite(getContext());
        if (bool)
        {
            makePackageDirs();
            HashMap<File,String> files = makeFiles(suffix);
            writeFiles(files);
        }
    }

    final protected InvarContext getContext ()
    {
        return context;
    }

    final protected void log (Object txt)
    {
        System.out.println(txt);
    }

    final protected void exportFile (String resPath, String fileDir, String fileName)
    {
        InputStream res = getClass().getResourceAsStream(resPath);
        if (res != null)
        {
            makeDirs(fileDir);
            exportFiles.put(res, fileDir + "/" + fileName);
        }
        else
        {
            log("error ---------> Export resource does not exist: " + resPath);
        }
    }

    private void makeDirs (String path)
    {
        File dir = new File(dirRoot, path);
        if (!dir.exists())
            dir.mkdirs();
    }

    private HashMap<File,String> makeFiles (String suffix)
    {
        HashMap<File,String> files = new HashMap<File,String>();
        Iterator<String> i = getContext().getPackNames();
        while (i.hasNext())
        {
            InvarPackage pack = getContext().getPack(i.next());
            Iterator<String> iTypeName = pack.getTypeNames();
            while (iTypeName.hasNext())
            {
                String typeName = iTypeName.next();
                makeFile(files, pack, typeName, suffix);
            }
        }
        files.putAll(makeRuntimeFile(suffix));
        return files;
    }

    private void makeFile (HashMap<File,String> fs, InvarPackage pack, String tName, String suffix)
    {
        File codeDir = pack.getCodeDir();
        if (codeDir == null)
            return;
        InvarType type = pack.getType(tName);
        if (TypeID.ENUM == type.getId())
        {
            TypeEnum t = (TypeEnum)type;
            File codeFile = new File(codeDir, t.getName() + suffix);
            fs.put(codeFile, codeEnum(t));
        }
        else if (TypeID.STRUCT == type.getId())
        {
            TypeStruct t = (TypeStruct)type;
            File codeFile = new File(codeDir, t.getName() + suffix);
            fs.put(codeFile, codeStruct(t));
        }
        else if (TypeID.PROTOCOL == type.getId())
        {
            TypeProtocol t = (TypeProtocol)type;
            File codeFile = null;
            if (t.hasClient())
            {
                codeFile = new File(codeDir, t.getClient().getName() + suffix);
                fs.put(codeFile, codeStruct(t.getClient()));
            }
            if (t.hasServer())
            {
                codeFile = new File(codeDir, t.getServer().getName() + suffix);
                fs.put(codeFile, codeStruct(t.getServer()));
            }
        }
        else
        {
            // do nothing
        }
    }

    private HashMap<File,String> makeRuntimeFile (String suffix)
    {
        HashMap<File,String> files = new HashMap<File,String>();
        InvarType type = getContext().ghostAdd("invar", "InvarRuntime", "");
        makeDirs("invar");
        File codeFile = new File(dirRoot, "invar/" + type.getName() + suffix);
        files.put(codeFile, codeRuntime(type));
        return files;
    }

    protected HashMap<File,String> makeProtocFile (String string)
    {
        //TODO make a protocol interface code
        HashMap<File,String> files = new HashMap<File,String>();
        return files;
    }

    private void makePackageDirs () throws Exception
    {
        Iterator<String> i = getContext().getPackNames();
        while (i.hasNext())
        {
            InvarPackage pack = getContext().getPack(i.next());
            if (!pack.getNeedWrite())
                continue;
            String path = pack.getName().replace('.', '/') + '/';
            makeDirs(path);
            File packDir = new File(dirRoot, path);
            if (!packDir.exists())
            {
                throw new Exception("Dir do not exist: " + packDir.getAbsolutePath());
            }
            pack.setCodeDir(packDir);
            log("mkdir -> " + packDir.getAbsolutePath());
        }
    }

    private void deleteDirs (String dir)
    {
        File delfolder = new File(dir);
        File oldFile[] = delfolder.listFiles();
        if (oldFile == null)
            return;
        for (int i = 0; i < oldFile.length; i++)
        {
            if (oldFile[i].isDirectory())
                deleteDirs(dir + oldFile[i].getName() + "//");
            oldFile[i].delete();
        }
        delfolder.delete();
    }

    private void writeFiles (HashMap<File,String> files) throws IOException
    {
        parseExportFiles(files);
        Iterator<File> i = files.keySet().iterator();
        while (i.hasNext())
        {
            File file = i.next();
            String content = files.get(file);
            FileWriter writer = new FileWriter(file, false);
            writer.write(content == null ? "" : content);
            writer.close();
            log("write -> " + file.getAbsolutePath());
        }
    }

    private void parseExportFiles (HashMap<File,String> files) throws IOException
    {
        Iterator<InputStream> i = exportFiles.keySet().iterator();
        while (i.hasNext())
        {
            InputStream res = i.next();
            File file = new File(dirRoot, exportFiles.get(res));
            byte[] bs = new byte[res.available()];
            res.read(bs);
            char[] chars = getChars(bs);
            files.put(file, String.copyValueOf(chars));
        }
    }

    final protected StringBuilder dumpTypeAll ()
    {
        StringBuilder s = new StringBuilder();
        Iterator<String> i = getContext().getPackNames();
        while (i.hasNext())
        {
            InvarPackage pack = getContext().getPack(i.next());
            Iterator<String> iTypeName = pack.getTypeNames();
            while (iTypeName.hasNext())
            {
                String typeName = iTypeName.next();
                InvarType type = pack.getType(typeName);
                s.append(TypeID.GHOST == type.getId() ? " # " : "   ");
                s.append(fixedLen(" ", 32, pack.getName() + "." + typeName));
                if (type.getRedirect() != null)
                {
                    InvarType typeR = type.getRedirect();
                    s.append(" --->  ");
                    String namePack = typeR.getPack().getName();
                    if (!namePack.equals(""))
                        s.append(namePack + ".");
                    s.append(typeR.getName());
                    s.append(typeR.getGeneric());
                }
                s.append("\n");
            }
            s.append(fixedLen("-", 80));
            s.append("\n");
        }
        return s;
    }

    static protected String upperHeadChar (String s)
    {
        return s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
    }

    static protected String fixedLen (String blank, Integer len, String str)
    {
        int delta = len - str.length();
        if (delta > 0)
            for (int i = 0; i < delta; i++)
                str += blank;
        return str;
    }

    static protected String fixedLen (Integer len, String str)
    {
        return fixedLen(" ", len, str);
    }

    static protected String fixedLen (String blank, Integer len)
    {
        return fixedLen(blank, len, "");
    }

    static protected char[] getChars (byte[] bytes)
    {
        Charset cs = Charset.forName("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        bb.flip();
        CharBuffer cb = cs.decode(bb);
        return cb.array();
    }

    static protected byte[] getBytes (char[] chars)
    {
        Charset cs = Charset.forName("UTF-8");
        CharBuffer cb = CharBuffer.allocate(chars.length);
        cb.put(chars);
        cb.flip();
        ByteBuffer bb = cs.encode(cb);
        return bb.array();
    }

    static protected void checkKeywords (String s, String[] ks) throws Exception
    {
        if (Arrays.binarySearch(ks, s) >= 0)
        {
            throw new Exception(s + " is a reserved word.");
        }
    }

    final static private String GENERIC_LEFT  = "<";
    final static private String GENERIC_RIGHT = ">";

    final protected String ruleLeft (String rule)
    {
        String name = rule;
        if (rule.indexOf(GENERIC_LEFT) >= 0)
        {
            name = rule.substring(0, rule.indexOf(GENERIC_LEFT));
        }
        return name;
    }

    final protected String ruleRight (String rule)
    {
        int iBegin = rule.indexOf(GENERIC_LEFT) + 1;
        int iEnd = rule.lastIndexOf(GENERIC_RIGHT);
        if (iBegin > 0 && iEnd > iBegin)
        {
            return rule.substring(iBegin, iEnd);
        }
        return null;
    }
}
