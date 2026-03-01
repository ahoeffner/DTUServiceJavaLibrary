package dtu.services.library.utils.database;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;

/**
 * This class will rewrites a simple filter to jdbc syntax.
 * Given a filter like "first_name like 'john' or last_name like 'doe'"
 * getJDBCFilter will return "(first_name like ? or last_name like ?)"
 * and bindings = "john" and "smith"
 */
public class Filter
{
    private static final String regex = "'([^']*)'|(-?\\d+\\.?\\d*)|(\\S+)";
    private static final Pattern pattern = Pattern.compile(regex);

    public static void main(String[] args)
    {
        String filter =
        """
            first_name like 'john'
            or last_name like 'doe'
            and sal between 2000 and 3000
            and hire_date = '29-08-1960'
        """;

        JDBCFilter jdbc = getJDBCFilter(filter);
        System.out.println(jdbc.sql);
        System.out.println(jdbc.bindings);
    }


    public static JDBCFilter getJDBCFilter(String filter)
    {
        if (filter == null)
            return (null);

        JDBCFilter jdbc = new JDBCFilter();
        StringBuilder sql = new StringBuilder();

        // Group 1: Quoted strings '...'
        // Group 2: Numbers (integers or decimals)
        // Group 3: Everything else (operators, column names)
        Matcher matcher = pattern.matcher(filter);

        while (matcher.find())
        {
            if (matcher.group(1) != null)
            {
                // Found quoted string
                sql.append("? ");
                jdbc.bindings.add(getObject(matcher.group(1)));
            }
            else if (matcher.group(2) != null)
            {
                // Found number
                sql.append("? ");
                jdbc.bindings.add(getObject(matcher.group(2)));
            }
            else
            {
                // Found operator or column name
                String word = matcher.group(3);
                sql.append(word).append(" ");
            }
        }

        jdbc.sql = "(" + sql.toString().trim() + ")";
        return (jdbc);
    }


    public static class JDBCFilter
    {
        String sql = null;
        ArrayList<Object> bindings = new ArrayList<>();
    }


    private static Object getObject(String val)
    {
        if (val == null)
            return (null);

        try
        {
            // Try Long
            if (val.matches("-?\\d+"))
                return (Long.parseLong(val));
        }
        catch (Exception e) {}

        try
        {
            if (val.matches("-?\\d*\\.\\d+"))
                return(Float.parseFloat(val));
        }
        catch (Exception e) {}

        try
        {
            // Try Date (yyyy-MM-dd)
            if (val.matches("\\d{4}-\\d{2}-\\d{2}"))
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                return (sdf.parse(val));
            }
        }
        catch (Exception e) {}

        try
        {
            // Try Date (dd-MM-yyyy)
            if (val.matches("\\d{2}-\\d{2}-\\d{4}"))
            {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                sdf.setLenient(false);
                return (sdf.parse(val));
            }
        }
        catch (Exception e) {}

        return(val);
    }
}