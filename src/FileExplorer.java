import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Stack;
import java.util.prefs.Preferences;

public class FileExplorer extends JFrame {
    private JTextField pathField, searchField;
    private JButton backButton, forwardButton;
    private Stack<File> backStack, forwardStack;
    private DirectoryTreePanel directoryTreePanel;
    private FileViewPanel fileViewPanel;
    private static final String PREF_VIEW_MODE = "viewMode";
    private static final String PREF_LAST_PATH = "lastPath";
    private static final String PREF_WINDOW_X = "windowX";
    private static final String PREF_WINDOW_Y = "windowY";
    private static final String PREF_WINDOW_WIDTH = "windowWidth";
    private static final String PREF_WINDOW_HEIGHT = "windowHeight";
    private Preferences prefs;

    public FileExplorer() {
        setTitle("Explorateur de Fichiers");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width, screenSize.height);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        prefs = Preferences.userNodeForPackage(FileExplorer.class);
        
        int x = prefs.getInt(PREF_WINDOW_X, 100);
        int y = prefs.getInt(PREF_WINDOW_Y, 100);
        int width = prefs.getInt(PREF_WINDOW_WIDTH, 800);
        int height = prefs.getInt(PREF_WINDOW_HEIGHT, 600);
        setBounds(x, y, width, height);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveWindowPreferences();
            }
        });

        setLayout(new BorderLayout());

        backStack = new Stack<>();
        forwardStack = new Stack<>();

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backButton = new JButton();
        backButton.setIcon(new ImageIcon("icon/arrow-left.png"));
        forwardButton = new JButton();
        forwardButton.setIcon(new ImageIcon("icon/arrow-right.png"));

        backButton.addActionListener(e -> navigateBack());
        forwardButton.addActionListener(e -> navigateForward());

        backButton.setEnabled(false);
        forwardButton.setEnabled(false);

        navPanel.add(backButton);
        navPanel.add(forwardButton);

        setPathField(new JTextField("Ordinateur", 30));
        getPathField().setEditable(false);
        navPanel.add(getPathField());

        // Ajouter le champ de recherche
        searchField = new JTextField(15);
        searchField.setToolTipText("Rechercher...");
        searchField.addActionListener(e -> fileViewPanel.filterContent(searchField.getText()));
        navPanel.add(searchField);

        add(navPanel, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        JButton viewToggleButton = new JButton("Changer Affichage");
        viewToggleButton.addActionListener(e -> {
            fileViewPanel.toggleView();
            showDirectoryContent(new File(getPathField().getText()));
            saveViewModePreference(fileViewPanel.isGridView());
        });
        panel.add(viewToggleButton);
        add(panel, BorderLayout.SOUTH);

        fileViewPanel = new FileViewPanel(this);
        boolean isGridView = prefs.getBoolean(PREF_VIEW_MODE, false);
        fileViewPanel.setGridView(isGridView);

        directoryTreePanel = new DirectoryTreePanel(this);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                directoryTreePanel.getScrollPane(), fileViewPanel.getScrollPane());
        add(splitPane, BorderLayout.CENTER);

        String lastPath = prefs.get(PREF_LAST_PATH, "Ordinateur");
        File lastDir = new File(lastPath);

        if (lastDir.exists() && lastDir.isDirectory()) {
            setPath(lastPath);
            showDirectoryContent(lastDir);
        } else {
            setPath("Ordinateur");
            showDirectoryContent(new File("Ordinateur"));
        }
    }

    // Méthodes de navigation
    private void navigateBack() {
        if (!backStack.isEmpty()) {
            forwardStack.push(new File(getPathField().getText()));
            showDirectoryContent(backStack.pop());
            updateNavButtons();
        }
    }

    private void navigateForward() {
        if (!forwardStack.isEmpty()) {
            backStack.push(new File(getPathField().getText()));
            showDirectoryContent(forwardStack.pop());
            updateNavButtons();
        }
    }

    public void setPath(String path) {
        getPathField().setText(path);
    }


    public void showDirectoryContent(File directory) {
        fileViewPanel.showDirectoryContent(directory);
        getPathField().setText(directory.getAbsolutePath());
        prefs.put(PREF_LAST_PATH, directory.getAbsolutePath());
    }


    private void updateNavButtons() {
        backButton.setEnabled(!backStack.isEmpty());
        forwardButton.setEnabled(!forwardStack.isEmpty());
    }

    private void saveViewModePreference(boolean isGridView) {
        prefs.putBoolean(PREF_VIEW_MODE, isGridView);
    }

    private void saveWindowPreferences() {
        prefs.putInt(PREF_WINDOW_X, getX());
        prefs.putInt(PREF_WINDOW_Y, getY());
        prefs.putInt(PREF_WINDOW_WIDTH, getWidth());
        prefs.putInt(PREF_WINDOW_HEIGHT, getHeight());
    }
    
    public void navigateTo(File directory) {
        // Ne pas ajouter dans l'historique si l'utilisateur utilise les boutons de navigation
        if (!directory.getAbsolutePath().equals(getPathField().getText())) {
            backStack.push(new File(getPathField().getText()));
            forwardStack.clear(); // Vider la pile d'avance lorsque l'utilisateur va dans un nouveau dossier
        }
        showDirectoryContent(directory);
        updateNavButtons();
    }

	public JTextField getPathField() {
		return pathField;
	}

	public void setPathField(JTextField pathField) {
		this.pathField = pathField;
	}

}
