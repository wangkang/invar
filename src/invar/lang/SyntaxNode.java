package invar.lang;

final class SyntaxNode
{
    static final String FILE_ROOT           = "";
    static final String LINE_DOC            = "//";
    static final String DOC_START           = "/*";
    static final String DOC_END             = "*/";

    static final String SYMBOL_PACK         = "package";
    static final String SYMBOL_PACK_END     = ";";
    static final String SYMBOL_ENUM         = "enum";
    static final String SYMBOL_ENUM_START   = "{";
    static final String SYMBOL_ENUM_END     = "}";
    static final String SYMBOL_STRUCT       = "struct";
    static final String SYMBOL_STRUCT_START = "{";
    static final String SYMBOL_STRUCT_END   = "}";
    static final String SYMBOL_PROTOC       = "ptotoc";
    static final String SYMBOL_PROTOC_START = "{";
    static final String SYMBOL_PROTOC_END   = "}";

    static public SyntaxNode build ()
    {
        SyntaxNode n = new SyntaxNode(FILE_ROOT, 5);
        n.child(new SyntaxNode(DOC_START, 1).child(new SyntaxNode(DOC_END, 0)));

        n.child(new SyntaxNode(SYMBOL_PACK, 1).child(new SyntaxNode(SYMBOL_PACK_END, 0)));

        n.child(new SyntaxNode(SYMBOL_ENUM, 2).child(new SyntaxNode(SYMBOL_ENUM_START, 0))
                                              .child(new SyntaxNode(SYMBOL_ENUM_END, 0)));
        n.child(new SyntaxNode(SYMBOL_STRUCT, 2).child(new SyntaxNode(SYMBOL_STRUCT, 0))
                                                .child(new SyntaxNode(SYMBOL_STRUCT_END, 0)));
        n.child(new SyntaxNode(SYMBOL_PROTOC, 2).child(new SyntaxNode(SYMBOL_PROTOC_START, 0))
                                                .child(new SyntaxNode(SYMBOL_PROTOC_END, 0)));
        return n;
    }

    private final String       symbol;
    private final SyntaxNode[] childen;
    private int                count         = 0;

    private boolean            symbolMatched = false;

    private SyntaxNode child (SyntaxNode n)
    {
        childen[count] = n;
        ++count;
        return this;
    }

    public SyntaxNode(String symbol, int numChildren)
    {
        this.symbol = symbol;
        childen = new SyntaxNode[numChildren];
        this.symbolMatched = (symbol.equals(FILE_ROOT));
    }

    public boolean isLeaf ()
    {
        return childen.length == 0;
    }

    public String getSymbol ()
    {
        return symbol;
    }

    public void reset ()
    {
    }

    public SyntaxNode parse (CodeLine line, int offset)
    {
        if (childen.length == 0)
        {
            return this;
        }
        System.out.println(symbol + "  SyntaxNode.parse() --->>> " + line);
        SyntaxNode node = this;
        StringBuilder buffer = new StringBuilder(8);
        int len = line.numChars();
        for (int i = offset; i < len; i++)
        {
            char c = line.charAt(i);
            if (' ' == c || '\t' == c)
                continue;
            buffer.append(c);
            node = matchChildren(buffer);
            if (node != null)
            {
                node = node.parse(line, i);
                break;
            }
        }
        return node == null ? this : node;
    }

    private SyntaxNode matchChildren (StringBuilder buffer)
    {
        int len = buffer.length();
        for (SyntaxNode child : childen)
        {
            if (child.symbol.length() != len)
            {
                continue;
            }
            for (int i = 0; i < len; i++)
            {
                if (buffer.charAt(i) != child.symbol.charAt(i))
                    return null;
            }
            return child;
        }
        return null;
    }
}
