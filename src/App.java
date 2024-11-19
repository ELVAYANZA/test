import javax.swing.SwingUtilities;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;
// Methode principale
public class App {
    public static void main(String[] args)throws Exception {
    	try {
                   UIManager.setLookAndFeel(new FlatLightLaf());

        } catch (Exception ex) {

        }

        SwingUtilities.invokeLater(() -> {
            FileExplorer explorer = new FileExplorer();
            explorer.setVisible(true);
        });
    }
}

