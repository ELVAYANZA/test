import javax.swing.*;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;

public class FileViewPanel {
    private JPanel panel;
    private JScrollPane scrollPane;
    private JTable table;
    private boolean isGridView = false; // Mode d'affichage : liste ou grille
    private FileExplorer fileExplorer;

    public FileViewPanel(FileExplorer explorer) {
        this.fileExplorer = explorer;
        panel = new JPanel(new BorderLayout());
        scrollPane = new JScrollPane(panel);
    }

    // Méthode pour définir le mode de vue en fonction des préférences sauvegardées
    public void setGridView(boolean isGridView) {
        this.isGridView = isGridView;
    }

    // Méthode pour vérifier le mode de vue actuel
    public boolean isGridView() {
        return isGridView;
    }

    public void toggleView() {
        isGridView = !isGridView;
    }

    // Afficher le contenu d'un répertoire spécifique
    public void showDirectoryContent(File directory) {
        panel.removeAll();
        File[] files;

        if (directory.getAbsolutePath().equals("PC")) { 
            // Si le dossier est "PC", affichez les disques et les périphériques
            files = File.listRoots();
        } else {
            // Sinon, affichez le contenu du répertoire
            files = directory.listFiles();
        }

        if (files == null) {
            JOptionPane.showMessageDialog(panel, "Impossible de lire le contenu du répertoire : " + directory.getAbsolutePath(), "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        displayFiles(files);
        panel.revalidate();
        panel.repaint();
    }




    // Filtrer et afficher le contenu selon la requête de recherche
    public void searchAllDirectories(File directory, String query, ArrayList<File> results) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchAllDirectories(file, query, results); // Recherche dans le sous-répertoire
                } else if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                    results.add(file); // Ajoute le fichier s'il correspond à la requête
                }
            }
        }
    }


    public void filterContent(String query) {
        ArrayList<File> results = new ArrayList<>();
        searchAllDirectories(new File("/"), query, results); // "/"" pour Linux/Mac, "C:\\" pour Windows
        
        File[] files = results.toArray(new File[0]);
        panel.removeAll();
        displayFiles(files);
        panel.revalidate();
        panel.repaint();
    }



    // Méthode pour afficher les fichiers soit en grille, soit en liste
    private void displayFiles(File[] files) {
        FileSystemView fileSystemView = FileSystemView.getFileSystemView();

        if (isGridView) {
            JPanel gridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            gridPanel.setPreferredSize(new Dimension(180, (files.length / 4 + 1) * 40));

            for (File file : files) {
                JPanel filePanel = new JPanel(new BorderLayout());
                filePanel.setPreferredSize(new Dimension(50, 50));
                filePanel.setBackground(Color.WHITE);

                JLabel iconLabel = new JLabel(fileSystemView.getSystemIcon(file), JLabel.CENTER);
                JLabel nameLabel = new JLabel(fileSystemView.getSystemDisplayName(file), JLabel.CENTER);

                filePanel.add(iconLabel, BorderLayout.CENTER);
                filePanel.add(nameLabel, BorderLayout.SOUTH);

                // Ajout du double-clic pour ouvrir le fichier ou le répertoire
                filePanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        filePanel.setBackground(Color.LIGHT_GRAY);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        filePanel.setBackground(Color.WHITE);
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            if (file.isDirectory()) {
                                fileExplorer.setPath(file.getAbsolutePath());
                                fileExplorer.showDirectoryContent(file);
                            } else {
                                try {
                                    Desktop.getDesktop().open(file);
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(panel, "Impossible d'ouvrir le fichier : " + file.getName(), "Erreur", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                    }
                });

                gridPanel.add(filePanel);
            }

            JScrollPane gridScrollPane = new JScrollPane(gridPanel);
            gridScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            panel.add(gridScrollPane, BorderLayout.CENTER);

        } else {
            String[] columnNames = { "Nom", "Type", "Taille" };
            Object[][] data = new Object[files.length][3];

            for (int i = 0; i < data.length; i++) {
                File file = files[i];
                data[i][0] = file;
                data[i][1] = file.isDirectory() ? "Dossier" : (isRemovableDrive(file) ? "Appareil Mobile" : "Fichier");
                data[i][2] = file.isFile() ? file.length() : "";
            }

            DefaultTableModel model = new DefaultTableModel(data, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            table = new JTable(model);

            // Ajoutez un renderer personnalisé si nécessaire
            table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    if (value instanceof File) {
                        File file = (File) value;
                        setText(fileSystemView.getSystemDisplayName(file));
                        setIcon(fileSystemView.getSystemIcon(file));
                    }
                    return label;
                }
            });



            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int row = table.getSelectedRow();
                        if (row != -1) {
                            File file = (File) table.getValueAt(row, 0);
                            if (file.isDirectory()) {
                                fileExplorer.navigateTo(file);
                            } else {
                                try {
                                    Desktop.getDesktop().open(file);
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(panel, "Impossible d'ouvrir le fichier : " + file.getName(), "Erreur", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                    }
                }
            });

            table.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int row = table.rowAtPoint(e.getPoint());
                    table.clearSelection();
                    if (row != -1) {
                        table.setRowSelectionInterval(row, row);
                    }
                }
            });
            
            table.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    char keyChar = e.getKeyChar();
                    
                    // Vérifie si la touche est une lettre
                    if (Character.isLetter(keyChar)) {
                        keyChar = Character.toLowerCase(keyChar);
                        for (int i = 0; i < table.getRowCount(); i++) {
                            File file = (File) table.getValueAt(i, 0);
                            if (file.getName().toLowerCase().charAt(0) == keyChar) {
                                table.setRowSelectionInterval(i, i);
                                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                                break;
                            }
                        }
                    }
                }
            });


            panel.add(new JScrollPane(table), BorderLayout.CENTER);
        }
    }

    private boolean isRemovableDrive(File file) {
    return file.exists() && file.isDirectory() && file.canRead() && !file.getAbsolutePath().equals("C:\\"); // Exemple pour Windows
}




	public JScrollPane getScrollPane() {
        return scrollPane;
    }
}
