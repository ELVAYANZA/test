import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class FileViewPanel {
	private JPanel panel;
	private JScrollPane scrollPane;
	private JTable table;
	private boolean isGridView = false; // Mode d'affichage : liste ou grille
	private FileExplorer fileExplorer;

	// Variables statiques pour stocker le fichier copié/coupé
	private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	private boolean isCutOperation = false;

	public FileViewPanel(FileExplorer explorer) {

		table = new JTable();
		
		// Dans le constructeur ou la méthode d'initialisation
		table.addKeyListener(new KeyAdapter() {
		    @Override
		    public void keyPressed(KeyEvent e) {
		        if (e.getKeyCode() == KeyEvent.VK_A && e.isControlDown()) {
		            // Sélectionner toutes les lignes
		            table.selectAll();
		        }
		    }
		});

		this.fileExplorer = explorer;
		panel = new JPanel(new BorderLayout());
		scrollPane = new JScrollPane(panel);
		panel.setDropTarget(
				new DropTarget(panel, DnDConstants.ACTION_COPY_OR_MOVE, new FileDropTargetListener(this), true));

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
			JOptionPane.showMessageDialog(panel,
					"Impossible de lire le contenu du répertoire : " + directory.getAbsolutePath(), "Erreur",
					JOptionPane.ERROR_MESSAGE);
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

	// Methode pour la recherche sur toute l'ordinateur
	public void filterContent(String query) {
		ArrayList<File> results = new ArrayList<>();
		searchAllDirectories(new File("/"), query, results); // "/"" pour Linux/Mac, "C:\\" pour Windows

		File[] files = results.toArray(new File[0]);
		panel.removeAll();
		displayFiles(files);
		panel.revalidate();
		panel.repaint();

	}
	private List<File> selectedFiles = new ArrayList<>();

	// Méthode pour afficher les fichiers soit en grille, soit en liste
	private void displayFiles(File[] files) {
		FileSystemView fileSystemView = FileSystemView.getFileSystemView();

		// mode grille
		if (isGridView) {
			table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // Sélection multiple
			JPanel gridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
			gridPanel.setPreferredSize(new Dimension(180, (files.length / 4 + 1) * 40));
			gridPanel.setTransferHandler(new FileTransferHandler());
			gridPanel.setDropTarget(new DropTarget(gridPanel, DnDConstants.ACTION_COPY_OR_MOVE, new FileDropTargetListener(this), true));


			gridPanel.setFocusable(true);
			gridPanel.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					char keyChar = e.getKeyChar();

					if (Character.isLetter(keyChar)) {
						keyChar = Character.toLowerCase(keyChar);
						for (int i = 0; i < files.length; i++) {
							if (files[i].getName().toLowerCase().charAt(0) == keyChar) {
								gridPanel.scrollRectToVisible(new Rectangle(0, i * 40, gridPanel.getWidth(), 40));
								break;
							}
						}
					}
				}
			});

			gridPanel.addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					gridPanel.requestFocusInWindow(); // Assurez-vous que gridPanel a bien le focus
				}
			});

			SwingUtilities.invokeLater(() -> {
				gridPanel.requestFocusInWindow(); // Après le rendu de l'interface
			});

			for (File file : files) {
				
				JPanel filePanel = new JPanel(new BorderLayout());
				
				filePanel.putClientProperty("file", file); // Associe le fichier au panneau
		        filePanel.setTransferHandler(new FileTransferHandler()); // Assigne le TransferHandler
		        new DropTarget(filePanel, DnDConstants.ACTION_COPY_OR_MOVE, new FileDropTargetListener(null), true);

				
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
				                    JOptionPane.showMessageDialog(panel,
				                            "Impossible d'ouvrir le fichier : " + file.getName(), "Erreur",
				                            JOptionPane.ERROR_MESSAGE);
				                }
				            }
				        }
				    }

				    @Override
				    public void mousePressed(MouseEvent e) {
				        if (e.isPopupTrigger()) {
				            showPopupMenu(e, file);
				        } else if (SwingUtilities.isLeftMouseButton(e)) {
				            // Vérifier que le double-clic n'est pas en cours
				            if (e.getClickCount() == 1) {
				                filePanel.getTransferHandler().exportAsDrag(filePanel, e, TransferHandler.COPY); // Démarre le drag
				            }
				        }
				    }

				    @Override
				    public void mouseReleased(MouseEvent e) {
				        if (e.isPopupTrigger()) {
				            showPopupMenu(e, file);
				        }
				    }
				});


				gridPanel.add(filePanel);

			}
			table = new JTable();

			table.setDragEnabled(true);

			new DropTarget(table, DnDConstants.ACTION_COPY_OR_MOVE, new FileDropTargetListener(this), true);

			JScrollPane gridScrollPane = new JScrollPane(gridPanel);
			gridScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			panel.add(gridScrollPane, BorderLayout.CENTER);

			// mode list
		} else {
			table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // Sélection multiple pour la vue
																					// liste
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

			table.setDragEnabled(true);

			new DropTarget(table, DnDConstants.ACTION_COPY_OR_MOVE, new FileDropTargetListener(this), true);

			addPopupToTable();

			// Ajoutez un renderer personnalisé si nécessaire
			table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
						boolean hasFocus, int row, int column) {
					JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
							column);

					if (value instanceof File) {
						File file = (File) value;
						setText(fileSystemView.getSystemDisplayName(file));
						setIcon(fileSystemView.getSystemIcon(file));
					}
					return label;
				}
			});

			// Action du double clique sur les list 
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
									JOptionPane.showMessageDialog(panel,
											"Impossible d'ouvrir le fichier : " + file.getName(), "Erreur",
											JOptionPane.ERROR_MESSAGE);
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
		return file.exists() && file.isDirectory() && file.canRead() && !file.getAbsolutePath().equals("C:"); // Exemple
																												// pour
																												// Windows
	}

	private JPopupMenu createPopupMenu(File file) {
		JPopupMenu popupMenu = new JPopupMenu();
		if (file == null) {
			JMenuItem createFolder = new JMenuItem("Créer un dossier");
			createFolder.addActionListener(e -> createFolderInCurrentDirectory());
			popupMenu.add(createFolder);
		} else if (file.isDirectory()) {
			addMenuItemsForDirectory(popupMenu, file);
		} else {
			addMenuItemsForFile(popupMenu, file);
		}
		return popupMenu;
	}

	private void addMenuItemsForDirectory(JPopupMenu popupMenu, File file) {
		JMenuItem open = new JMenuItem("Ouvrir");
		open.addActionListener(e -> fileExplorer.showDirectoryContent(file));
		popupMenu.add(open);

		JMenuItem createFolder = new JMenuItem("Créer un dossier");
		createFolder.addActionListener(e -> createFolderInDirectory(file));
		popupMenu.add(createFolder);

		JMenuItem rename = new JMenuItem("Renommer");
		rename.addActionListener(e -> renameFile(file));
		popupMenu.add(rename);

		JMenuItem copy = new JMenuItem("Copier");
		copy.addActionListener(e -> copyFile(file));
		popupMenu.add(copy);

		JMenuItem cut = new JMenuItem("Couper");
		cut.addActionListener(e -> cutFile(file));
		popupMenu.add(cut);

		JMenuItem paste = new JMenuItem("Coller");
		paste.addActionListener(e -> pasteFile(file.getParentFile()));
		paste.setEnabled(isClipboardFileAvailable()); // Activer seulement si un fichier est copié/coupé
		popupMenu.add(paste);

		JMenuItem delete = new JMenuItem("Supprimer");
		delete.addActionListener(e -> deleteFile(file));
		popupMenu.add(delete);

		JMenuItem properties = new JMenuItem("Propriété");
		properties.addActionListener(e -> showProperties(file));
		popupMenu.add(properties);

	}

	private void addMenuItemsForFile(JPopupMenu popupMenu, File file) {
		JMenuItem open = new JMenuItem("Ouvrir");
		open.addActionListener(e -> openFile(file));
		popupMenu.add(open);

		JMenuItem rename = new JMenuItem("Renommer");
		rename.addActionListener(e -> renameFile(file));
		popupMenu.add(rename);

		JMenuItem copy = new JMenuItem("Copier");
		copy.addActionListener(e -> copyFile(file));
		popupMenu.add(copy);

		JMenuItem cut = new JMenuItem("Couper");
		cut.addActionListener(e -> cutFile(file));
		popupMenu.add(cut);

		JMenuItem paste = new JMenuItem("Coller");
		paste.addActionListener(e -> pasteFile(file.getParentFile()));
		paste.setEnabled(isClipboardFileAvailable()); // Activer seulement si un fichier est copié/coupé
		popupMenu.add(paste);

		JMenuItem delete = new JMenuItem("Supprimer");
		delete.addActionListener(e -> deleteFile(file));
		popupMenu.add(delete);

		JMenuItem properties = new JMenuItem("Propriété");
		properties.addActionListener(e -> showProperties(file));
		popupMenu.add(properties);

	}

	private void addPopupToTable() {
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				handlePopupTrigger(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				handlePopupTrigger(e);
			}

			private void handlePopupTrigger(MouseEvent e) {
				if (e.isPopupTrigger()) {
					int row = table.rowAtPoint(e.getPoint());
					File selectedFile = (row != -1) ? (File) table.getValueAt(row, 0) : null;
					showPopupMenu(e, selectedFile);
				}
			}
		});
	}

	// Méthode pour afficher le menu contextuel
	private void showPopupMenu(MouseEvent e, File file) {
		JPopupMenu popupMenu = createPopupMenu(file);
		popupMenu.show(e.getComponent(), e.getX(), e.getY());
	}

	private void showPopupMenu(MouseEvent e) {
		// Obtenez la sélection des fichiers
		int[] selectedRows = table.getSelectedRows();
		if (selectedRows.length == 0) {
			// Si rien n'est sélectionné, vous pouvez afficher un popup générique pour un
			// fichier
			int row = table.rowAtPoint(e.getPoint());
			File selectedFile = (row != -1) ? (File) table.getValueAt(row, 0) : null;
			JPopupMenu popupMenu = createPopupMenu(selectedFile);
			popupMenu.show(e.getComponent(), e.getX(), e.getY());
		} else {
			// Si plusieurs fichiers sont sélectionnés, afficher le menu pour la sélection
			// multiple
			JPopupMenu popupMenu = createPopupMenuForMultipleSelection(selectedRows);
			popupMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	private JPopupMenu createPopupMenuForMultipleSelection(int[] selectedRows) {
		JPopupMenu popupMenu = new JPopupMenu();

		JMenuItem copy = new JMenuItem("Copier");
		copy.addActionListener(e -> copyFiles(selectedRows));
		popupMenu.add(copy);

		JMenuItem cut = new JMenuItem("Couper");
		cut.addActionListener(e -> cutFiles(selectedRows));
		popupMenu.add(cut);

		JMenuItem delete = new JMenuItem("Supprimer");
		delete.addActionListener(e -> deleteFiles(selectedRows));
		popupMenu.add(delete);

		return popupMenu;
	}

	private void copyFiles(int[] selectedRows) {
		List<File> filesToCopy = new ArrayList<>();
		for (int row : selectedRows) {
			File file = (File) table.getValueAt(row, 0);
			filesToCopy.add(file);
		}
		// Copiez les fichiers
		clipboard.setContents(new FileTransferable(filesToCopy), null);
	}

	private void cutFiles(int[] selectedRows) {
		List<File> filesToCut = new ArrayList<>();
		for (int row : selectedRows) {
			File file = (File) table.getValueAt(row, 0);
			filesToCut.add(file);
		}
		// Mettez les fichiers dans le presse-papiers pour couper
		clipboard.setContents(new FileTransferable(filesToCut), null);
	}

	private void deleteFiles(int[] selectedRows) {
		for (int row : selectedRows) {
			File file = (File) table.getValueAt(row, 0);
			if (file.delete()) {
				showDirectoryContent(file.getParentFile()); // Rafraîchit la vue
			} else {
				JOptionPane.showMessageDialog(panel, "Impossible de supprimer " + file.getName(), "Erreur",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	// Méthodes pour les actions de menu
	private void openFile(File file) {
		try {
			Desktop.getDesktop().open(file);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(panel, "Erreur lors de l'ouverture du fichier : " + file.getName(), "Erreur",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void createFolderInCurrentDirectory() {
		File currentDir = new File(fileExplorer.getCurrentPath());
		createFolderInDirectory(currentDir);
	}

	private void createFolderInDirectory(File directory) {
		String folderName = JOptionPane.showInputDialog(panel, "Nom du nouveau dossier :");
		if (folderName != null) {
			File newFolder = new File(directory, folderName);
			if (newFolder.mkdir()) {
				showDirectoryContent(directory);
			} else {
				JOptionPane.showMessageDialog(panel, "Impossible de créer le dossier.", "Erreur",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void renameFile(File file) {
		String currentName = file.getName();
		String extension = "";

		// Vérifier si le fichier a une extension (par exemple, .pdf, .jpg, .png)
		int extIndex = currentName.lastIndexOf(".");
		if (extIndex != -1) {
			extension = currentName.substring(extIndex); // Extrait l'extension
		}

		// Demander à l'utilisateur de saisir un nouveau nom sans l'extension
		String newName = JOptionPane.showInputDialog(panel, "Nouveau nom :", currentName.substring(0, extIndex)); // Demander
																													// sans
																													// l'extension
		if (newName != null && !newName.trim().isEmpty()) {
			// Assurez-vous que le nom est différent et ajoutez l'extension
			if (!newName.equals(currentName.substring(0, extIndex))) {
				File newFile = new File(file.getParentFile(), newName + extension);

				// Vérifiez si le nom de fichier existe déjà
				if (newFile.exists()) {
					JOptionPane.showMessageDialog(panel, "Un fichier avec ce nom existe déjà.", "Erreur",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Essayez de renommer
				if (file.renameTo(newFile)) {
					showDirectoryContent(file.getParentFile()); // Rafraîchit la vue
				} else {
					JOptionPane.showMessageDialog(panel,
							"Impossible de renommer le fichier. Vérifiez les permissions et le chemin.", "Erreur",
							JOptionPane.ERROR_MESSAGE);
				}
			} else {
				JOptionPane.showMessageDialog(panel, "Le nom est identique au nom actuel.", "Information",
						JOptionPane.INFORMATION_MESSAGE);
			}
		} else {
//            JOptionPane.showMessageDialog(panel, "Nom de fichier invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void deleteFile(File file) {
		int confirmation = JOptionPane.showConfirmDialog(panel, "Supprimer " + file.getName() + " ?", "Confirmation",
				JOptionPane.YES_NO_OPTION);
		if (confirmation == JOptionPane.YES_OPTION) {
			if (file.delete()) {
				showDirectoryContent(file.getParentFile());
			} else {
				JOptionPane.showMessageDialog(panel, "Impossible de supprimer le fichier.", "Erreur",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	// Copier un fichier dans le presse-papiers
	private void copyFile(File file) {
		setClipboardFile(file, false);
	}

	// Couper un fichier dans le presse-papiers
	private void cutFile(File file) {
		setClipboardFile(file, true);
	}

	// Coller un fichier depuis le presse-papiers
	private void pasteFile(File targetDirectory) {
	    Transferable clipboardContent = clipboard.getContents(null);
	    if (clipboardContent != null && clipboardContent.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
	        try {
	            @SuppressWarnings("unchecked")
	            List<File> filesToPaste = (List<File>) clipboardContent.getTransferData(DataFlavor.javaFileListFlavor);
	            for (File file : filesToPaste) {
	                File destination = new File(targetDirectory, file.getName());
	                if (destination.exists()) {
	                    int overwriteOption = JOptionPane.showConfirmDialog(panel,
	                            "Le fichier " + file.getName() + " existe déjà. Voulez-vous le remplacer ?", "Confirmation",
	                            JOptionPane.YES_NO_OPTION);
	                    if (overwriteOption != JOptionPane.YES_OPTION) {
	                        continue;
	                    }
	                }
	                if (isCutOperation) {
	                    if (!file.renameTo(destination)) {
	                        JOptionPane.showMessageDialog(panel, "Impossible de déplacer le fichier " + file.getName(), "Erreur",
	                                JOptionPane.ERROR_MESSAGE);
	                    }
	                } else {
	                    Files.copy(file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
	                }
	            }
	            if (isCutOperation) {
	                isCutOperation = false;
	                clipboard.setContents(null, null); // Efface le presse-papiers après le déplacement
	            }
	            showDirectoryContent(targetDirectory); // Rafraîchit la vue après l'opération
	        } catch (UnsupportedFlavorException | IOException ex) {
	            JOptionPane.showMessageDialog(panel, "Erreur lors de l'opération de collage.", "Erreur",
	                    JOptionPane.ERROR_MESSAGE);
	        }
	    }
	}

	// Configurer le fichier dans le presse-papiers et indiquer si c'est une
	// opération de coupe

	// Configurer le fichier dans le presse-papiers et indiquer si c'est une opération de coupe
	private void setClipboardFile(File file, boolean isCut) {
	    try {
	        // Créer un FileTransferable pour le fichier
	        clipboard.setContents(new FileTransferable(file), null);

	        // Indiquer si l'opération dans le presse-papiers est une coupe ou une copie
	        isCutOperation = isCut;

	        // Vous pouvez également ajouter un peu de logique pour personnaliser l'état visuel
	        // ou gérer la logique d'interface utilisateur, par exemple, changer les couleurs
	        // ou désactiver des boutons de manière dynamique.
	    } catch (Exception e) {
	        JOptionPane.showMessageDialog(panel, "Erreur lors de la mise en presse-papiers du fichier.", "Erreur", JOptionPane.ERROR_MESSAGE);
	    }
	}


	// Vérifier si un fichier est disponible dans le presse-papiers
	private boolean isClipboardFileAvailable() {
		try {
			return clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor);
		} catch (Exception e) {
			return false;
		}
	}

	// Classe interne pour rendre un fichier transférable dans le presse-papiers
	private static class FileTransferable implements Transferable {
		private final List<File> files;

		public FileTransferable(File file) {
			this.files = List.of(file);
		}

		public FileTransferable(List<File> filesToCut) {
			this.files = null;
			// TODO Auto-generated constructor stub
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DataFlavor.javaFileListFlavor };
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.javaFileListFlavor.equals(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (!isDataFlavorSupported(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			return files;
		}
	}

	private void showProperties(File file) {
		JOptionPane.showMessageDialog(panel, "Propriétés du fichier : " + file.getAbsolutePath());
	}

	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	public JTable getTable() {
		// TODO Auto-generated method stub
		return table;
	}

	public JPanel getPanel() {
		// TODO Auto-generated method stub
		return panel;
	}

}
