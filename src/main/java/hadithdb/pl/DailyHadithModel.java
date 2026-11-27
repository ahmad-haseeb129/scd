package hadithdb.pl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import hadithdb.bll.DailyHadithBO;
import hadithdb.dto.DailyHadithViewDTO;
import hadithdb.dto.HadithDTO;
import hadithdb.dto.TranslationDTO;

public class DailyHadithModel
{
    public static final String LOAD_ERROR = "Could not load the Hadith of the Day. "
        + "Please reopen the daily view to try again. / آج کی حدیث لوڈ نہیں ہو سکی۔ دوبارہ کھول کر کوشش کریں۔";

    public enum Status
    {
        IDLE, LOADING, READY, EMPTY, ERROR
    }

    private final DailyHadithBO bo;
    private final ReadOnlyObjectWrapper<State> state;

    public DailyHadithModel(DailyHadithBO bo)
    {
        this.bo = Objects.requireNonNull(bo);
        state = new ReadOnlyObjectWrapper<>(new State(
            bo.currentDate(), null, null, null, Status.IDLE));
    }

    public State getState()
    {
        return state.get();
    }

    public ReadOnlyObjectProperty<State> stateProperty()
    {
        return state.getReadOnlyProperty();
    }

    public void beginLoading(LocalDate date)
    {
        state.set(new State(Objects.requireNonNull(date), null, null,
            getState().getPreferredLanguage(), Status.LOADING));
    }

    public void showResult(LocalDate requestedDate, DailyHadithViewDTO view)
    {
        Objects.requireNonNull(requestedDate);
        String preferred = getState().getPreferredLanguage();
        if (view == null)
        {
            state.set(new State(requestedDate, null, null, preferred,
                Status.EMPTY));
            return;
        }

        TranslationDTO translation = bo.selectTranslation(view, preferred);
        // The BO normally guarantees complete display data. Do not publish an incomplete view.
        if (view.getHadith() == null || translation == null)
        {
            showError(requestedDate);
            return;
        }
        state.set(new State(view.getSelectionDate(), view, translation, preferred, Status.READY));
    }

    public void showError(LocalDate date)
    {
        state.set(new State(Objects.requireNonNull(date), null, null,
            getState().getPreferredLanguage(), Status.ERROR));
    }

    
    public void selectLanguage(String language)
    {
        if (language == null || language.isBlank())
        {
            return;
        }

        State current = getState();
        String preferred = language.strip();
        TranslationDTO translation = current.view == null ? null
            : bo.selectTranslation(current.view, preferred);

        if (Objects.equals(current.preferredLanguage, preferred)
                && current.selectedTranslation == translation)
        {
            return;
        }
        state.set(new State(current.selectionDate, current.view, translation,
            preferred, current.status));
    }

   
    public static final class State
    {
        private final LocalDate selectionDate;
        private final DailyHadithViewDTO view;
        private final TranslationDTO selectedTranslation;
        private final String preferredLanguage;
        private final Status status;

        private State(LocalDate selectionDate, DailyHadithViewDTO view,
                TranslationDTO selectedTranslation, String preferredLanguage,
                Status status)
        {
            this.selectionDate = selectionDate;
            this.view = view;
            this.selectedTranslation = selectedTranslation;
            this.preferredLanguage = preferredLanguage;
            this.status = Objects.requireNonNull(status);
        }

        public LocalDate getSelectionDate()
        {
            return selectionDate;
        }

        public HadithDTO getSelectedHadith()
        {
            return view == null ? null : view.getHadith();
        }

        public List<TranslationDTO> getAvailableTranslations()
        {
            return view == null ? List.of() : view.getTranslations();
        }

        public TranslationDTO getSelectedTranslation()
        {
            return selectedTranslation;
        }

        public String getPreferredLanguage()
        {
            return preferredLanguage;
        }

        public Status getStatus()
        {
            return status;
        }

        public String getMessage()
        {
            switch (status)
            {
                case EMPTY:
                    return DailyHadithBO.NO_ELIGIBLE_HADITH;
                case ERROR:
                    return LOAD_ERROR;
                default:
                    return "";
            }
        }

        public boolean isLoading()
        {
            return status == Status.LOADING;
        }

        public boolean hasHadith()
        {
            return status == Status.READY;
        }

        public boolean isFallbackTranslation()
        {
            return preferredLanguage != null && selectedTranslation != null
                && !selectedTranslation.getLanguage().strip().equalsIgnoreCase(preferredLanguage);
        }
    }
}
