import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

public class FileTransferHandler extends TransferHandler {
    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE; // Spécifier si vous autorisez la copie ou le déplacement
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JPanel && c.getClientProperty("file") instanceof File) {
            File file = (File) c.getClientProperty("file");
            return new FileTransferable(file);
        } else {
            System.err.println("Le composant n'est pas un JPanel contenant un fichier.");
            return null;
        }
    }



    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    public boolean importData(TransferSupport support) {
        try {
            if (canImport(support)) {
                // Traitez le dépôt du fichier
                List<File> droppedFiles = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                for (File droppedFile : droppedFiles) {
                    // Effectuez l'action nécessaire avec le fichier déposé
                    System.out.println("Fichier déposé : " + droppedFile.getName());
                }
                return true;
            }
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
