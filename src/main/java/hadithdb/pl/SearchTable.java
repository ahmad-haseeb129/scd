package hadithdb.pl;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

final class SearchTable<T>
{
    private final TableView<T> table;
    private final ObservableList<T> rows = FXCollections.observableArrayList();
    private final FilteredList<T> filtered = new FilteredList<>(rows, row -> true);
    private final TextField search = new TextField();
    private final Label placeholder = new Label();
    private final String emptyText;

    SearchTable(TableView<T> table, String emptyText, Function<T, String> searchText)
    {
        this.table = table;
        this.emptyText = emptyText;
        table.setItems(filtered);
        placeholder.setWrapText(true);
        table.setPlaceholder(placeholder);
        search.setPromptText("Search");
        search.setPrefWidth(260);
        search.textProperty().addListener((source, before, now) ->
        {
            String query = now == null ? "" : now.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(row -> query.isEmpty()
                    || String.valueOf(searchText.apply(row)).toLowerCase(Locale.ROOT).contains(query));
            updatePlaceholder();
        });
        rows.addListener((ListChangeListener<T>) change -> updatePlaceholder());
        updatePlaceholder();
    }

    void setAll(List<T> items)
    {
        rows.setAll(items);
    }

    TextField field()
    {
        return search;
    }

    void reveal(Predicate<T> match)
    {
        for (T row : filtered)
        {
            if (match.test(row))
            {
                table.scrollTo(row);
                return;
            }
        }
    }

    static String join(Object... parts)
    {
        StringBuilder text = new StringBuilder();
        for (Object part : parts)
        {
            if (part != null)
            {
                text.append(part).append(' ');
            }
        }
        return text.toString();
    }

    private void updatePlaceholder()
    {
        placeholder.setText(rows.isEmpty() ? emptyText : "Nothing matches your search.");
    }
}
