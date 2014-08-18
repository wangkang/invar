package invar.lang.lex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LexPattern
{
    public static final LexPattern       HIDDEN_0_N;
    public static final LexPattern       HIDDEN_1_N;
    public static final LexPattern       DOC;
    public static final LexPattern       DOC_LINE;

    public static final LexPattern       LITERAL_FLOAT;
    public static final LexPattern       LITERAL_INT;
    public static final LexPattern       LITERAL_BOOL;

    public static final LexPattern       FILE_BLOCK;
    public static final LexPattern       BLOCK_L;
    public static final LexPattern       BLOCK_R;
    public static final LexPattern       RULE_L;
    public static final LexPattern       RULE_R;

    public static final List<LexPattern> STRUCT;
    public static final List<LexPattern> FILE;

    static
    {

        HIDDEN_1_N = new LexPattern(LexType.HIDDEN_CHARS, "[\t \r\n]+", 0);
        HIDDEN_0_N = new LexPattern(LexType.HIDDEN_CHARS, "[\t \r\n]+", 0).setOptional(true);
        DOC = new LexPattern(LexType.DOC, "/\\*.*?\\*/", Pattern.DOTALL).setOptional(true);
        DOC_LINE = new LexPattern(LexType.DOC_LINE, "//.*", 0).setOptional(true);

        LITERAL_FLOAT = new LexPattern(LexType.STRUCT_FIELD_DEFT, "[0-9]+\\.[0-9]+", 0);
        LITERAL_INT = new LexPattern(LexType.STRUCT_FIELD_DEFT, "([1-9][0-9]*)|(0)", 0);
        LITERAL_BOOL = new LexPattern(LexType.STRUCT_FIELD_DEFT, "true|false", 0);

        RULE_L = new LexPattern(LexType.BLOCK_L, "<", 0);
        RULE_R = new LexPattern(LexType.BLOCK_L, ">", 0);

        BLOCK_L = new LexPattern(LexType.BLOCK_L, "\\{", 0);
        BLOCK_R = new LexPattern(LexType.BLOCK_L, "\\}", 0);

        //SECTION_END = new LexPattern(LexType.BLOCK_L, "[package|shared|struct|enum|protoc]+", 0);

        FILE_BLOCK = new LexPattern(LexType.HIDDEN_CHARS, "\\s", 0);

    }

    static
    {
        List<LexPattern> list = new ArrayList<LexPattern>();

        list.add(HIDDEN_0_N);
        list.add(new LexPattern(LexType.STRUCT_SHARED, "shared", 0).setOptional(true));
        list.add(HIDDEN_1_N);
        list.add(new LexPattern(LexType.STRUCT_ID, "struct", 0));
        list.add(HIDDEN_1_N);
        list.add(new LexPattern(LexType.STRUCT_NAME, "[A-Z]+", 0));
        list.add(HIDDEN_0_N);
        list.add(BLOCK_L);
        list.add(HIDDEN_0_N);
        list.add(BLOCK_R);
        list.add(HIDDEN_0_N);

        STRUCT = Collections.unmodifiableList(list);
    }

    static
    {
        List<LexPattern> list = new ArrayList<LexPattern>();

        list.add(HIDDEN_0_N);
        list.add(DOC);
        list.add(HIDDEN_0_N);
        list.add(DOC_LINE);

        //list.addAll(STRUCT);

        FILE = Collections.unmodifiableList(list);
    }

    private final LexType                tokenType;
    private final String                 staticText;
    private final int                    sliceDelta = -1;

    private final Pattern                pattern;
    private List<LexPattern>             children;     // if err, stop scanning
    private List<LexPattern>             branches;     // if err, try another branch

    private boolean                      optional;
    private boolean                      sliceHere;

    public LexPattern(LexType type, String regex)
    {
        this(type, regex, 0);
    }

    public LexPattern(LexType type, String regex, int flags)
    {
        this.tokenType = type;
        this.pattern = Pattern.compile(regex, flags);
        if ((flags & Pattern.LITERAL) == Pattern.LITERAL)
        {
            this.staticText = regex;
        }
        else
        {
            this.staticText = null;
        }
    }

    public int match (CharSequence input, int from, int dest)
    {
        Matcher m = this.pattern.matcher(input);
        m.region(from, dest);
        if (m.lookingAt())
        {
            return m.end();
        }
        return -1;
    }

    public boolean isOptional ()
    {
        return optional;
    }

    public LexPattern setOptional (boolean optional)
    {
        this.optional = optional;
        return this;
    }

    public List<LexPattern> getChildren ()
    {
        return children;
    }

    public void setChildren (List<LexPattern> children)
    {
        this.children = children;
    }

    public LexType getTokenType ()
    {
        return tokenType;
    }

    public int getSliceDelta ()
    {
        return sliceDelta;
    }

}
