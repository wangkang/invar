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

public abstract class InvarWrite
{
    abstract protected Boolean beforeWrite(InvarContext ctx);
    abstract protected String codeEnum(TypeEnum type);
    abstract protected String codeStruct(TypeStruct type);
    abstract protected String codeStructAlias(InvarType type);

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

    final public void write(String suffix) throws Throwable
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

    final protected InvarContext getContext()
    {
        return context;
    }

    final protected void log(Object txt)
    {
        System.out.println(txt);
    }

    final protected String fixedLen(String blank, Integer len, String str)
    {
        int delta = len - str.length();
        if (delta > 0)
            for (int i = 0; i < delta; i++)
                str += blank;
        return str;
    }
    final protected String upperHeadChar(String s)
    {
        return s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
    }
    final protected String fixedLen(Integer len, String str)
    {
        return fixedLen(" ", len, str);
    }
    final protected String fixedLen(String blank, Integer len)
    {
        return fixedLen(blank, len, "");
    }

    final protected void exportFile(String resPath, String fileDir, String fileName)
    {
        InputStream res = getClass().getResourceAsStream(resPath);
        if (res != null)
        {
            makeDirs(fileDir);
            exportFiles.put(res, fileDir + "/" + fileName);
        }
        else
        {
            //log("[WARN] Resource does not exist: " + resPath);
        }
    }

    private void makeDirs(String path)
    {
        File dir = new File(dirRoot, path);
        if (!dir.exists())
            dir.mkdirs();
    }

    private HashMap<File,String> makeFiles(String suffix)
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
                File codeDir = pack.getCodeDir();
                if (codeDir == null)
                {
                    //System.out.println("code dir null. " + typeName);
                    continue;
                }
                InvarType type = pack.getType(typeName);
                if (TypeID.ENUM == type.getId())
                {
                    TypeEnum t = (TypeEnum)type;
                    File codeFile = new File(codeDir, t.getName() + suffix);
                    files.put(codeFile, codeEnum(t));
                }
                else if (TypeID.STRUCT == type.getId())
                {
                    TypeStruct t = (TypeStruct)type;
                    File codeFile = new File(codeDir, t.getName() + suffix);
                    files.put(codeFile, codeStruct(t));
                }
                else if (TypeID.PROTOCOL == type.getId())
                {
                    TypeProtocol t = (TypeProtocol)type;
                    File codeFile = null;
                    if (t.hasClient())
                    {
                        codeFile = new File(codeDir, t.getClient().getName()
                                + suffix);
                        files.put(codeFile, codeStruct(t.getClient()));
                    }
                    if (t.hasServer())
                    {
                        codeFile = new File(codeDir, t.getServer().getName()
                                + suffix);
                        files.put(codeFile, codeStruct(t.getServer()));
                    }
                }
                else
                {
                    // do nothing
                }
            }
        }
        files.putAll(makeAliasFile(suffix));
        return files;
    }
    private HashMap<File,String> makeAliasFile(String suffix)
    {
        HashMap<File,String> files = new HashMap<File,String>();
        InvarType type = getContext().ghostAdd("invar", "InvarAlias", "");
        makeDirs("invar");
        File codeFile = new File(dirRoot, "invar/" + type.getName() + suffix);
        files.put(codeFile, codeStructAlias(type));
        return files;
    }
    protected HashMap<File,String> makeProtocFiles(String string)
    {
        HashMap<File,String> files = new HashMap<File,String>();
        return files;
    }

    private void makePackageDirs() throws Exception
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
                throw new Exception("Dir do not exist: "
                        + packDir.getAbsolutePath());
            }
            pack.setCodeDir(packDir);
            log("mkdir -> " + packDir.getAbsolutePath());
        }
    }
    private void deleteDirs(String dir)
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

    private void writeFiles(HashMap<File,String> files) throws IOException
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

    private void parseExportFiles(HashMap<File,String> files) throws IOException
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

    static protected char[] getChars(byte[] bytes)
    {
        Charset cs = Charset.forName("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        bb.flip();
        CharBuffer cb = cs.decode(bb);
        return cb.array();
    }

    static protected byte[] getBytes(char[] chars)
    {
        Charset cs = Charset.forName("UTF-8");
        CharBuffer cb = CharBuffer.allocate(chars.length);
        cb.put(chars);
        cb.flip();
        ByteBuffer bb = cs.encode(cb);
        return bb.array();
    }

    static protected void checkKeywords(String name, String[] keywords) throws Exception
    {
        if (Arrays.binarySearch(keywords, name) >= 0)
        {
            throw new Exception(name + " is a reserved word.");
        }
    }
}
