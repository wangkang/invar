package invar.lang;

import java.util.ArrayList;

public class CodeFile
{
    private final String   code;
    private final CodeLine lines[];

    public CodeFile()
    {
        this.code = "";
        this.lines = new CodeLine[0];
    }

    public CodeFile(String code)
    {
        ArrayList<CodeLine> list = new ArrayList<CodeLine>();
        int from = 0;
        int dest = 0;
        int delta = 0;
        int len = code.length();
        while (dest < len)
        {
            char c = code.charAt(dest);
            ++dest;
            if ('\r' == c)
            {
                --delta;
            }
            else if ('\n' == c)
            {
                --delta;
                if (from < dest + delta)
                {
                    list.add(new CodeLine(from, dest + delta, code, list.size()));
                }
                from = dest;
            }
            else
            {
                delta = 0;
            }
        }
        if (from < dest)
        {
            list.add(new CodeLine(from, dest, code, list.size()));
        }
        this.code = code;
        this.lines = list.toArray(new CodeLine[list.size()]);
    }

    public String toString ()
    {
        StringBuilder s = new StringBuilder();
        for (CodeLine line : lines)
        {
            s.append(line.toString());
        }
        return s.toString();
    }
}
