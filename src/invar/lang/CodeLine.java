package invar.lang;

public class CodeLine
{
    private final String code;
    private final int    from;
    private final int    dest;
    private final int    number;

    public CodeLine(int from, int dest, String code, int number)
    {
        // [from, dest)
        this.from = from;
        this.dest = dest;
        this.code = code;
        this.number = number + 1;
    }

    public int getFrom ()
    {
        return from;
    }

    public int getDest ()
    {
        return dest;
    }

    public String toString ()
    {
        StringBuilder s = new StringBuilder();
        s.append(number + " : ");
        s.append(code.substring(from, dest));
        s.append('\n');
        return s.toString();
    }
}
