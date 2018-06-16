import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.Date;

public class MyFormatter extends Formatter {
    @Override
    public String format(LogRecord rec) {
        StringBuilder sb = new StringBuilder();

        sb.append(rec.getLevel().getName())
            .append(": " + new Date(rec.getMillis()))
            .append("\n")
            .append(formatMessage(rec) + "\n\n");

        return sb.toString();
    }
}
