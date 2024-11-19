import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.swing.JOptionPane;

public class FileDropTargetListener extends DropTargetAdapter {
//    private final FileViewPanel fileViewPanel;
    private FileViewPanel viewPanel;

    public FileDropTargetListener(FileViewPanel viewPanel) {
        this.viewPanel = viewPanel;
    }

     @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        // Peut être utilisé pour modifier le curseur ou vérifier le type de données
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        // Peut être utilisé pour ajuster l'apparence pendant le survol
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // Peut être utilisé pour ajuster l'action de dépôt
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        // Gère le départ du drag
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        Transferable transferable = dtde.getTransferable();
        try {
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                for (File file : droppedFiles) {
                    // Traitez chaque fichier déposé ici
                    JOptionPane.showMessageDialog(viewPanel.getPanel(), "Fichier déposé: " + file.getAbsolutePath());
                }
                dtde.dropComplete(true);
            } else {
                dtde.rejectDrop();
            }
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
            dtde.rejectDrop();
        }
    }



    private File getTargetDirectory(Point dropPoint) {
        int row = viewPanel.getTable().rowAtPoint(dropPoint);
        if (row != -1) {
            Object value = viewPanel.getTable().getValueAt(row, 0);
            return (value instanceof File) ? (File) value : null;
        }
        return null;
    }
}
