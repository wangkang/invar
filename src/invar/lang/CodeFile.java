package invar.lang;

import java.util.ArrayList;

final class CodeFile
{
    private final CodeLine lines[];

    public CodeFile()
    {
        this.lines = new CodeLine[0];
    }

    public int numLines ()
    {
        return lines.length;
    }

    public CodeLine getLine (int i)
    {
        return lines[i];
    }

    public CodeFile(final String code)
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
                list.add(new CodeLine(from, dest + delta, code, list.size()));
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
