import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.io.File;

public class DirectoryTreePanel {
	private JTree tree;
	private DefaultTreeModel treeModel;
	private JScrollPane scrollPane;
	private FileExplorer fileExplorer;
	private FileSystemView fileSystemView;
	private File copiedOrCutFile = null; // Variable pour stocker le fichier copié ou coupé
	private boolean isCutFile = false; // Variable pour savoir si le fichier est coupé
	private DefaultMutableTreeNode hoveredNode = null;

	public DirectoryTreePanel(FileExplorer explorer) {
		this.fileExplorer = explorer;
		this.fileSystemView = FileSystemView.getFileSystemView();

		// Initialisation de l'arborescence des dossiers
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("PC");
		File[] roots = File.listRoots();
		for (File root : roots) {
			DefaultMutableTreeNode driveNode = new DefaultMutableTreeNode(root);
			rootNode.add(driveNode);
			if (fileSystemView.isDrive(root)) {
				addSubDirectories(driveNode, root);
			}

			// Vérification des périphériques amovibles et affichage spécifique
			if (isRemovableDrive(root)) {
				driveNode.setUserObject("Appareil Mobile: " + root.getAbsolutePath());
			}
		}

		treeModel = new DefaultTreeModel(rootNode);
		tree = new JTree(treeModel); // Assurez-vous que tree est initialisé ici avant d'appeler setDragEnabled
		tree.setRootVisible(true);

		tree.setTransferHandler(new FileTransferHandler());
		new DropTarget(tree, new FileDropTargetListener());

		tree.setDragEnabled(true); // Active le glissement de l'arbre

		// Appliquer le renderer personnalisé pour les icônes et noms
		tree.setCellRenderer(new FileTreeCellRenderer());

		// Le reste de ton code continue ici...

		// Ajout du MouseListener pour afficher les menus contextuels
		tree.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) { // Détecter le clic droit
					TreePath path = tree.getPathForLocation(e.getX(), e.getY());
					if (path != null) {
						DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
						File selectedFile = (File) selectedNode.getUserObject();

						JPopupMenu popupMenu = createPopupMenu(selectedFile);
						popupMenu.show(tree, e.getX(), e.getY());
					}
				}
			}
		});

		scrollPane = new JScrollPane(tree);
		scrollPane.setPreferredSize(new Dimension(250, 0));

		// Listener pour double-clics sur les noeuds de l'arborescence
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());

				if (path != null) {
					DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
					Object userObject = selectedNode.getUserObject();

					// Vérification du double-clic
					if (e.getClickCount() == 2) {
						// Vérifier si le double-clic est sur la racine "PC"
						if ("PC".equals(userObject)) {
							JOptionPane.showMessageDialog(null, "Double-clic sur la racine PC !");
							fileExplorer.showDirectoryContent(null); // Action spécifique pour la racine
						}
						// Vérifier si c'est un fichier ou un dossier
						else if (userObject instanceof File) {
							File selectedFile = (File) userObject;
							if (selectedFile.isDirectory()) {
								fileExplorer.navigateTo(selectedFile); // Navigation vers le dossier
							} else {
								// Optionnel : Vous pouvez ajouter une logique pour ouvrir un fichier
								openFile(selectedFile);
							}
						}
					}
				}
			}
		});

		// Listener pour détection de survol
		tree.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());
				tree.clearSelection();
				if (path != null) {
					tree.setSelectionPath(path); // Sélection temporaire du nœud au survol
				}
			}
		});

		// Ajouter un KeyListener pour naviguer en fonction de la lettre
		tree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				char typedChar = Character.toUpperCase(e.getKeyChar());
				navigateToNodeStartingWith(typedChar);
			}
		});

	}

	// Méthode pour ajouter les sous-dossiers à un noeud en utilisant un SwingWorker
	private void addSubDirectories(DefaultMutableTreeNode node, File file) {
		node.add(new DefaultMutableTreeNode("Chargement..."));

		SwingWorker<Void, DefaultMutableTreeNode> worker = new SwingWorker<>() {
			@Override
			protected Void doInBackground() throws Exception {
				File[] subFiles = fileSystemView.getFiles(file, true); // Utiliser fileSystemView pour obtenir les
																		// fichiers
				if (subFiles != null) {
					node.removeAllChildren();
					for (File subFile : subFiles) {
						DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(subFile);
						node.add(subNode);
						if (subFile.isDirectory()) {
							subNode.add(new DefaultMutableTreeNode("Chargement..."));
						}
						publish(subNode);
					}
				}
				return null;
			}

			@Override
			protected void process(List<DefaultMutableTreeNode> chunks) {
				for (DefaultMutableTreeNode subNode : chunks) {
					treeModel.nodeStructureChanged(subNode);
				}
			}

			@Override
			protected void done() {
				treeModel.nodeStructureChanged(node);
			}
		};

		worker.execute();
	}

	// Méthode pour vérifier si un périphérique est amovible
	private boolean isRemovableDrive(File file) {
		// Utilisation de canRead() ou d'autres propriétés pour vérifier un périphérique
		// amovible
		return file.canRead() && !file.isDirectory() && file.exists();
	}

	// Méthode pour créer le menu contextuel
	private JPopupMenu createPopupMenu(File selectedFile) {
		JPopupMenu popupMenu = new JPopupMenu();

		// Si c'est un dossier
		if (selectedFile.isDirectory()) {
			JMenuItem openItem = new JMenuItem("Ouvrir");
			openItem.addActionListener(e -> fileExplorer.navigateTo(selectedFile));
			popupMenu.add(openItem);

			JMenuItem createFolderItem = new JMenuItem("Créer un nouveau dossier");
			createFolderItem.addActionListener(e -> createNewFolder(selectedFile));
			popupMenu.add(createFolderItem);

			JMenuItem renameItem = new JMenuItem("Renommer");
			renameItem.addActionListener(e -> renameFile(selectedFile));
			popupMenu.add(renameItem);

			JMenuItem deleteItem = new JMenuItem("Supprimer");
			deleteItem.addActionListener(e -> deleteFile(selectedFile));
			popupMenu.add(deleteItem);

			JMenuItem propertiesItem = new JMenuItem("Propriétés");
			propertiesItem.addActionListener(e -> showProperties(selectedFile));
			popupMenu.add(propertiesItem);
		}
		// Si c'est un fichier
		else if (selectedFile.isFile()) {
			JMenuItem openItem = new JMenuItem("Ouvrir");
			openItem.addActionListener(e -> openFile(selectedFile));
			popupMenu.add(openItem);

			JMenuItem deleteItem = new JMenuItem("Supprimer");
			deleteItem.addActionListener(e -> deleteFile(selectedFile));
			popupMenu.add(deleteItem);

			JMenuItem propertiesItem = new JMenuItem("Propriétés");
			propertiesItem.addActionListener(e -> showProperties(selectedFile));
			popupMenu.add(propertiesItem);
		}

		// Action "Copier"
		JMenuItem copyItem = new JMenuItem("Copier");
		copyItem.addActionListener(e -> {
			copiedOrCutFile = selectedFile;
			isCutFile = false; // L'utilisateur a copié, donc isCutFile est faux
		});
		popupMenu.add(copyItem);

		// Action "Couper"
		JMenuItem cutItem = new JMenuItem("Couper");
		cutItem.addActionListener(e -> {
			copiedOrCutFile = selectedFile;
			isCutFile = true; // L'utilisateur a coupé, donc isCutFile est vrai
		});
		popupMenu.add(cutItem);

		// Action "Coller"
		JMenuItem pasteItem = new JMenuItem("Coller");
		pasteItem.setEnabled(copiedOrCutFile != null); // Activer l'option "Coller" uniquement si un fichier est copié
														// ou coupé
		pasteItem.addActionListener(e -> {
			if (copiedOrCutFile != null) {
				File destination = selectedFile; // Le dossier où coller
				File target = new File(destination, copiedOrCutFile.getName());

				try {
					if (Files.exists(target.toPath())) {
						// Si le fichier existe déjà, il est écrasé
						Files.copy(copiedOrCutFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
					} else {
						// Si c'est un couper, on déplace le fichier
						if (isCutFile) {
							Files.move(copiedOrCutFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
						} else {
							// Sinon, on le copie
							Files.copy(copiedOrCutFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
					}
					refreshTree(); // Rafraîchir l'arborescence après le collage
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(null, "Erreur lors du collage du fichier.");
				}
			}
		});
		popupMenu.add(pasteItem);

		return popupMenu;
	}

	// Méthode pour créer un nouveau dossier
	private void createNewFolder(File parentDirectory) {
		String folderName = JOptionPane.showInputDialog("Nom du nouveau dossier:");
		if (folderName != null && !folderName.trim().isEmpty()) {
			File newFolder = new File(parentDirectory, folderName);
			if (newFolder.mkdir()) {
				refreshTree(); // Rafraîchir l'arborescence pour refléter les changements
			} else {
				JOptionPane.showMessageDialog(null, "Échec de la création du dossier.");
			}
		}
	}

	// Méthode pour renommer un fichier
	private void renameFile(File file) {
		String newName = JOptionPane.showInputDialog("Nouveau nom:");
		if (newName != null && !newName.trim().isEmpty()) {
			File newFile = new File(file.getParent(), newName);
			if (file.renameTo(newFile)) {
				refreshTree(); // Rafraîchir l'arborescence
			} else {
				JOptionPane.showMessageDialog(null, "Échec du renommage.");
			}
		}
	}

	// Méthode pour supprimer un fichier
	private void deleteFile(File file) {
		int confirm = JOptionPane.showConfirmDialog(null, "Êtes-vous sûr de vouloir supprimer ce fichier ?",
				"Confirmer la suppression", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION) {
			if (file.delete()) {
				refreshTree(); // Rafraîchir l'arborescence
			} else {
				JOptionPane.showMessageDialog(null, "Échec de la suppression.");
			}
		}
	}

	// Méthode pour afficher les propriétés d'un fichier
	private void showProperties(File file) {
		JOptionPane.showMessageDialog(null, "Propriétés du fichier: " + file.getAbsolutePath());
	}

// Méthode pour ouvrir un fichier (par exemple, pour les disques)
	private void openFile(File file) {
		// Implémentation d'ouverture de fichier (par exemple, dans le système par
		// défaut)
		try {
			if (file.exists()) {
				Desktop.getDesktop().open(file); // Ouvrir le fichier avec l'application par défaut du système
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Impossible d'ouvrir le fichier.");
		}
	}

	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	public void refreshTree() {
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("PC");
		File[] roots = File.listRoots();
		for (File root : roots) {
			DefaultMutableTreeNode driveNode = new DefaultMutableTreeNode(root);
			rootNode.add(driveNode);
			if (fileSystemView.isDrive(root)) {
				addSubDirectories(driveNode, root);
			}

			// Vérification des périphériques amovibles et affichage spécifique
			if (isRemovableDrive(root)) {
				driveNode.setUserObject("Appareil Mobile: " + root.getAbsolutePath());
			}
		}
		treeModel.setRoot(rootNode);
	}

	// Méthode pour naviguer vers le premier dossier dont le nom commence par
	// typedChar
	private void navigateToNodeStartingWith(char typedChar) {
		TreeNode rootNode = (TreeNode) treeModel.getRoot();
		TreePath path = findNodeStartingWith(rootNode, typedChar);

		if (path != null) {
			tree.setSelectionPath(path);
			tree.scrollPathToVisible(path);
		}
	}

	// Méthode récursive pour rechercher un nœud dont le nom commence par typedChar
	private TreePath findNodeStartingWith(TreeNode node, char typedChar) {
		for (int i = 0; i < node.getChildCount(); i++) {
			TreeNode childNode = node.getChildAt(i);
			DefaultMutableTreeNode mutableNode = (DefaultMutableTreeNode) childNode;
			Object userObject = mutableNode.getUserObject();

			if (userObject instanceof File) {
				File file = (File) userObject;
				String name = fileSystemView.getSystemDisplayName(file);

				if (!name.isEmpty() && Character.toUpperCase(name.charAt(0)) == typedChar) {
					return new TreePath(mutableNode.getPath());
				}
			}

			// Recherche dans les sous-dossiers
			TreePath childPath = findNodeStartingWith(childNode, typedChar);
			if (childPath != null) {
				return childPath;
			}
		}
		return null;
	}

	// Classe pour personnaliser le rendu des icônes et noms dans le JTree
	private class FileTreeCellRenderer extends DefaultTreeCellRenderer {
	    private Icon rootIcon;

	    public FileTreeCellRenderer() {
	        rootIcon = UIManager.getIcon("FileView.computerIcon");
	    }

	    @Override
	    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
	                                                  boolean leaf, int row, boolean hasFocus) {
	        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

	        if (value instanceof DefaultMutableTreeNode) {
	            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
	            Object userObject = node.getUserObject();

	            if (userObject instanceof File) {
	                File file = (File) userObject;
	                setText(fileSystemView.getSystemDisplayName(file));
	                setIcon(fileSystemView.getSystemIcon(file));

	                // Appliquer la couleur lorsque le nœud est survolé pendant le glissement
	                if (node == hoveredNode) {
	                    setBackgroundNonSelectionColor(new Color(173, 216, 230)); // Couleur de survol
	                    setOpaque(true); // Assurez-vous que le fond est visible
	                } else {
	                    setBackgroundNonSelectionColor(getBackground()); // Rétablir la couleur d'origine
	                    setOpaque(false);
	                }
	            } else if (userObject instanceof String && userObject.equals("PC")) {
	                setText("PC");
	                setIcon(rootIcon);
	            }
	        }
	        return this;
	    }
	}


	// Classe pour gérer le transfert des fichiers (glissement)
	private class FileTransferHandler extends TransferHandler {
		@Override
		public int getSourceActions(JComponent c) {
			return TransferHandler.MOVE; // Définir l'action de glissement comme un déplacement
		}

		@Override
		protected Transferable createTransferable(JComponent c) {
			JTree tree = (JTree) c;
			TreePath selectedPath = tree.getSelectionPath();
			if (selectedPath != null) {
				DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
				File file = (File) selectedNode.getUserObject();
				return new FileTransferable(file); // Créer un objet transférable pour le fichier
			}
			return null;
		}
	}

	// Classe pour le transfert du fichier
	private class FileTransferable implements Transferable {
		private final File file;

		public FileTransferable(File file) {
			this.file = file;
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
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, java.io.IOException {
			return java.util.Collections.singletonList(file); // Renvoie la liste contenant le fichier
		}
	}

	// Classe pour gérer le dépôt (drop) des fichiers avec effet de survol
	// Modifiez la classe pour mettre à jour le tooltip
	private class FileDropTargetListener extends DropTargetAdapter {
	    private File draggedFile;

	    @Override
	    public void dragEnter(DropTargetDragEvent dtde) {
	        try {
	            // Obtenez le fichier en cours de glissement pour afficher le nom
	            Transferable transferable = dtde.getTransferable();
	            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
	                List<File> draggedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
	                draggedFile = draggedFiles.get(0); // On suppose qu'on glisse un seul fichier
	            }
	        } catch (Exception e) {
	            draggedFile = null;
	        }
	    }

	    @Override
	    public void dragOver(DropTargetDragEvent dtde) {
	        TreePath path = tree.getPathForLocation(dtde.getLocation().x, dtde.getLocation().y);
	        if (path != null) {
	            DefaultMutableTreeNode destinationNode = (DefaultMutableTreeNode) path.getLastPathComponent();
	            File destinationDir = (File) destinationNode.getUserObject();

	            // Vérifiez si on survole un dossier et construisez le message
	            if (destinationDir.isDirectory() && draggedFile != null) {
	                tree.setToolTipText(draggedFile.getName() + " copier vers " + destinationDir.getName());
	            }

	            // Mettez à jour le nœud en surbrillance
	            if (destinationNode != hoveredNode) {
	                if (hoveredNode != null) {
	                    tree.repaint(); // Réinitialise l'ancien nœud survolé
	                }
	                hoveredNode = destinationNode;
	                tree.repaint(); // Redessine le nouveau nœud survolé
	            }
	        }
	    }

	    @Override
	    public void drop(DropTargetDropEvent dtde) {
	        // Effectuer les actions nécessaires pour le dépôt et réinitialiser le tooltip
	        tree.setToolTipText(null);

	        try {
	            Transferable transferable = dtde.getTransferable();
	            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
	                List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
	                File droppedFile = droppedFiles.get(0);
	                TreePath path = tree.getPathForLocation(dtde.getLocation().x, dtde.getLocation().y);
	                if (path != null) {
	                    DefaultMutableTreeNode destinationNode = (DefaultMutableTreeNode) path.getLastPathComponent();
	                    File destinationDir = (File) destinationNode.getUserObject();
	                    if (destinationDir.isDirectory()) {
	                        File targetFile = new File(destinationDir, droppedFile.getName());
	                        if (!droppedFile.renameTo(targetFile)) {
	                            Files.copy(droppedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	                        }
	                        refreshTree();
	                    }
	                }
	                dtde.dropComplete(true);
	                hoveredNode = null; // Réinitialiser le nœud survolé après le dépôt
//	                tree.repaint(); // Redessiner pour enlever la couleur du survol
	            }
	        } catch (Exception ex) {
	            dtde.dropComplete(false);
	        }
	    }
	}

}