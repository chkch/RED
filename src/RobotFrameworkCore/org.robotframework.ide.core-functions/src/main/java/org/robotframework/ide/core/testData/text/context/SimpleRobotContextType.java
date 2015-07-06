package org.robotframework.ide.core.testData.text.context;

/**
 * Gives types, which are not multiple lines - just one line.
 * 
 * @author wypych
 * @since JDK 1.7 update 74
 * @version Robot Framework 2.9 alpha 2
 */
public enum SimpleRobotContextType implements IContextElementType {
    /**
     * means that this line can't be match to any context
     */
    UNDECLARED_COMMENT,
    /**
     * line contains hash sign or comment word
     */
    DECLARED_COMMENT,
    /**
     * <pre>
     * *** Settings ***
     * </pre>
     * 
     * setting table declaration
     */
    SETTING_TABLE_HEADER,
    /**
     * <pre>
     * *** Variables ***
     * </pre>
     * 
     * variable table declaration
     */
    VARIABLE_TABLE_HEADER,
    /**
     * <pre>
     * *** Test Case ***
     * </pre>
     * 
     * variable table declaration
     */
    TEST_CASE_TABLE_HEADER,
    /**
     * <pre>
     * *** Keywords ***
     * </pre>
     * 
     * keyword table declaration
     */
    KEYWORD_TABLE_HEADER,
    /**
     * is i.e. "water is liquid"
     */
    QUOTES_SENTENCE,
    /**
     * separator without pipe
     */
    DOUBLE_SPACE_OR_TABULATOR_SEPARATED,
    /**
     * separator following by pipe and whitespace inside
     */
    PIPE_SEPARATED,
    /**
     * this contexts belongs to additional spaces or tabulators made by user for
     * readability increase
     */
    PRETTY_ALIGN,
    /**
     * in textual format new line \n
     */
    LINE_FEED_TEXT,
    /**
     * \t
     */
    TABULATOR_TEXT,
    /**
     * \xhh char with hex value
     */
    CHAR_WITH_BYTE_HEX_VALUE,
    /**
     * \\uhhhh
     */
    CHAR_WITH_SHORT_HEX_VALUE,
    /**
     * \\Uhhhhhhhh
     */
    UNICODE_CHAR_WITH_HEX_VALUE;
}
