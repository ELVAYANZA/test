import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class App {
    public static void main(String[] args)throws Exception {
    	
        UIManager.setLookAndFeel(new FlatLightLaf());

        SwingUtilities.invokeLater(() -> {
            FileExplorer explorer = new FileExplorer();
            explorer.setVisible(true);
        });
    }
}

