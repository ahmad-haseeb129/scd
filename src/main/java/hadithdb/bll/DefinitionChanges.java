package hadithdb.bll;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class DefinitionChanges {
    private final PropertyChangeSupport listeners = new PropertyChangeSupport(this);

    public void listen(String type, PropertyChangeListener listener) {
        listeners.addPropertyChangeListener(type, listener);
    }

    public void stopListening(String type, PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(type, listener);
    }

    void saved(String type) {
        listeners.firePropertyChange(type, false, true);
    }
}
