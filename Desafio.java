import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Desafio extends JFrame {

    private BufferedImage image;
    private static final int IMAGE_WIDTH = 1024;
    private static final int IMAGE_HEIGHT = 856;

    public Desafio() {
        setTitle("Desafio - Processamento Digital de Imagens");
        setSize(IMAGE_WIDTH, IMAGE_HEIGHT + 80); // Espaço extra para o botão
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Painel para a imagem
        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (image != null) {
                    int x = (getWidth() - image.getWidth()) / 2;
                    int y = (getHeight() - image.getHeight()) / 2;
                    g.drawImage(image, x, y, this);
                }
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(IMAGE_WIDTH, IMAGE_HEIGHT);
            }
        };

        // Botão para carregar imagem
        JButton loadButton = new JButton("Carregar Imagem");
        loadButton.addActionListener(e -> carregarImagem(imagePanel));

        // Layout
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loadButton);

        add(imagePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
    }

    private void carregarImagem(JPanel panel) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selecione uma imagem PNG");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagens PNG", "png"));

    int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                image = ImageIO.read(file);

                if (image.getWidth() != IMAGE_WIDTH || image.getHeight() != IMAGE_HEIGHT) {
                    JOptionPane.showMessageDialog(this,
                        "A imagem deve ter exatamente " + IMAGE_WIDTH + "x" + IMAGE_HEIGHT + " pixels.",
                        "Erro de Tamanho",
                        JOptionPane.ERROR_MESSAGE);
                    image = null;
                } else {
                    image = binarizarImagem(image);
                    image = limparImagem(image);
                    contarComprimidos(image);
                }

                panel.repaint();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao carregar imagem: " + ex.getMessage());
            }
        }
    }

private void contarComprimidos(BufferedImage img) {
    int width = img.getWidth();
    int height = img.getHeight();
    boolean[][] visited = new boolean[width][height];

    int pequenos = 0;
    int medios = 0;
    int grandes = 0;

    List<Integer> areas = new ArrayList<>();

    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
            if (!visited[x][y] && isBranco(img.getRGB(x, y))) {
                int area = floodFill(img, visited, x, y);

                if (area >= 500) { // ignora ruído
                    areas.add(area);

                    if (area < 10000) {
                        pequenos++;
                    } else if (area <= 12000) {
                        medios++;
                    } else {
                        grandes++;
                    }
                }
            }
        }
    }

    // Mostrar estatísticas
    StringBuilder sb = new StringBuilder("Resumo por categoria:\n\n");

    sb.append("Total: ").append(grandes + medios).append("\n");
    sb.append("Quebrados: ").append(pequenos).append("\n");
    sb.append("Redondos: ").append(medios).append("\n");
    sb.append("Cápsulas: ").append(grandes).append("\n");

    JTextArea textArea = new JTextArea(sb.toString());
    textArea.setEditable(false);
    textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setPreferredSize(new Dimension(350, 150));
    JOptionPane.showMessageDialog(this, scrollPane, "Resumo por categoria", JOptionPane.INFORMATION_MESSAGE);
}



private int floodFill(BufferedImage img, boolean[][] visited, int startX, int startY) {
    int width = img.getWidth();
    int height = img.getHeight();
    int area = 0;

    LinkedList<Point> stack = new LinkedList<>();
    stack.push(new Point(startX, startY));

    while (!stack.isEmpty()) {
        Point p = stack.pop();
        int x = p.x;
        int y = p.y;

        if (x < 0 || y < 0 || x >= width || y >= height) continue;
        if (visited[x][y]) continue;
        if (!isBranco(img.getRGB(x, y))) continue;

        visited[x][y] = true;
        area++;

        stack.push(new Point(x + 1, y));
        stack.push(new Point(x - 1, y));
        stack.push(new Point(x, y + 1));
        stack.push(new Point(x, y - 1));
    }

    return area;
}

private BufferedImage binarizarImagem(BufferedImage original) {
    int width = original.getWidth();
    int height = original.getHeight();
    BufferedImage binarizada = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
            Color c = new Color(original.getRGB(x, y));

            // Verifica se é preto (fundo) — permite variação leve
            if (c.getRed() < 30 && c.getGreen() < 30 && c.getBlue() < 30) {
                binarizada.setRGB(x, y, Color.BLACK.getRGB());
            } else {
                binarizada.setRGB(x, y, Color.WHITE.getRGB());
            }
        }
    }

    return binarizada;
}

private BufferedImage limparImagem(BufferedImage binarizada) {
    int width = binarizada.getWidth();
    int height = binarizada.getHeight();
    boolean[][] visited = new boolean[width][height];

    BufferedImage limpa = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
    Graphics2D g2 = limpa.createGraphics();
    g2.setColor(Color.BLACK);
    g2.fillRect(0, 0, width, height); // fundo preto
    g2.dispose();

    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
            if (!visited[x][y] && isBranco(binarizada.getRGB(x, y))) {
                List<Point> pixels = new ArrayList<>();
                int area = floodFillCollect(binarizada, visited, x, y, pixels);

                if (area >= 500) { // mantém apenas regiões maiores que 500 pixels
                    for (Point p : pixels) {
                        limpa.setRGB(p.x, p.y, Color.WHITE.getRGB());
                    }
                }
            }
        }
    }

    return limpa;
}

private int floodFillCollect(BufferedImage img, boolean[][] visited, int startX, int startY, List<Point> pixels) {
    int width = img.getWidth();
    int height = img.getHeight();
    int area = 0;

    LinkedList<Point> stack = new LinkedList<>();
    stack.push(new Point(startX, startY));

    while (!stack.isEmpty()) {
        Point p = stack.pop();
        int x = p.x;
        int y = p.y;

        if (x < 0 || y < 0 || x >= width || y >= height) continue;
        if (visited[x][y]) continue;
        if (!isBranco(img.getRGB(x, y))) continue;

        visited[x][y] = true;
        pixels.add(p);
        area++;

        stack.push(new Point(x + 1, y));
        stack.push(new Point(x - 1, y));
        stack.push(new Point(x, y + 1));
        stack.push(new Point(x, y - 1));
    }

    return area;
}

private boolean isBranco(int rgb) {
    Color c = new Color(rgb);
    int limiar = 200; 
    return c.getRed() > limiar && c.getGreen() > limiar && c.getBlue() > limiar;
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Desafio frame = new Desafio();
            frame.setVisible(true);
        });
    }
}
