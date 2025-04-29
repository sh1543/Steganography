import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class SteganographyGUI extends JFrame implements ActionListener {

    private final JButton encodeButton;
    private final JButton decodeButton;
    private final JButton selectButton;
    private final JTextArea textArea;
    private final JLabel imageLabel;
    private BufferedImage image;
    private File imageFile;

    public SteganographyGUI() 
    {
        setTitle("Steganography Tool");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        encodeButton = new JButton("Encode");
        decodeButton = new JButton("Decode");
        selectButton = new JButton("Select Image");

        encodeButton.addActionListener(this);
        decodeButton.addActionListener(this);
        selectButton.addActionListener(this);

        topPanel.add(selectButton);
        topPanel.add(encodeButton);
        topPanel.add(decodeButton);

        add(topPanel, BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        imageLabel = new JLabel("", SwingConstants.CENTER);
        add(imageLabel, BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SteganographyGUI gui = new SteganographyGUI();
            gui.setVisible(true);
        });
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == selectButton) {
            selectImage();
        } else if (e.getSource() == encodeButton) {
            if (image != null) {
                encodeImage();
            } else {
                JOptionPane.showMessageDialog(this, "No image selected!");
            }
        } else if (e.getSource() == decodeButton) {
            if (image != null) {
                decodeImage();
            } else {
                JOptionPane.showMessageDialog(this, "No image selected!");
            }
        }
    }

    private void selectImage() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                imageFile = fileChooser.getSelectedFile();
                image = ImageIO.read(imageFile);
                ImageIcon icon = new ImageIcon(image.getScaledInstance(300, 200, Image.SCALE_SMOOTH));
                imageLabel.setIcon(icon);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error loading image.");
            }
        }
    }

    private void encodeImage() {
    String data = textArea.getText();
    if (data.isEmpty()) {
        JOptionPane.showMessageDialog(this, "No text to encode!");
        return;
    }

    BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
    for (int x = 0; x < image.getWidth(); x++) {
        for (int y = 0; y < image.getHeight(); y++) {
            newImage.setRGB(x, y, image.getRGB(x, y));
        }
    }

    modPix(newImage, data);

    image = newImage;
    ImageIcon icon = new ImageIcon(image.getScaledInstance(300, 200, Image.SCALE_SMOOTH));
    imageLabel.setIcon(icon);

    JFileChooser fileChooser = new JFileChooser();
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        try {
            File outputFile = fileChooser.getSelectedFile();
            String formatName = outputFile.getName().substring(outputFile.getName().lastIndexOf('.') + 1);
            ImageIO.write(image, formatName, outputFile);
            JOptionPane.showMessageDialog(this, "Image saved successfully!");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving image.");
        }
    }
}

    
    private void decodeImage() {
        String decodedText = decode(image);
        textArea.setText(decodedText);
    }

    private void modPix(BufferedImage img, String data) {
        String[] datalist = genData(data);
        int lendata = datalist.length;

        int width = img.getWidth();
        int height = img.getHeight();
        int x = 0, y = 0;

        outer:
        for (int i = 0; i < lendata; i++) {
            int[] pixels = new int[9];
            for (int k = 0; k < 3; k++) {
                for (int l = 0; l < 3; l++) {
                    if (x >= width) {
                        x = 0;
                        y++;
                        if (y >= height) break outer;
                    }
                    pixels[k * 3 + l] = img.getRGB(x, y);
                    x++;
                }
            }

            int[] newPixels = new int[9];
            for (int j = 0; j < 8; j++) {
                int rgb = pixels[j];
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                if (datalist[i].charAt(j) == '0' && (r % 2 != 0)) r--;
                else if (datalist[i].charAt(j) == '1' && (r % 2 == 0)) r = (r == 0) ? 1 : r - 1;

                newPixels[j] = (r << 16) | (g << 8) | b;
            }

            int rgb = pixels[8];
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            if (i == lendata - 1) {
                if (r % 2 == 0) r = (r == 0) ? 1 : r - 1;
            } else {
                if (r % 2 != 0) r--;
            }
            newPixels[8] = (r << 16) | (g << 8) | b;

            x -= 9;
            for (int k = 0; k < 3; k++) {
                for (int l = 0; l < 3; l++) {
                    if (x >= width) {
                        x = 0;
                        y++;
                    }
                    if (y >= height) break outer;
                    img.setRGB(x, y, newPixels[k * 3 + l]);
                    x++;
                }
            }
        }
    }

    private String decode(BufferedImage img) {
        StringBuilder data = new StringBuilder();
        int width = img.getWidth();
        int height = img.getHeight();
        int x = 0, y = 0;

        while (true) {
            int[] pixels = new int[9];
            for (int k = 0; k < 3; k++) {
                for (int l = 0; l < 3; l++) {
                    if (x >= width) {
                        x = 0;
                        y++;
                    }
                    if (y >= height) break;
                    pixels[k * 3 + l] = img.getRGB(x, y);
                    x++;
                }
            }

            StringBuilder binstr = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                int r = (pixels[i] >> 16) & 0xFF;
                binstr.append((r % 2 == 0) ? '0' : '1');
            }

            int charCode = Integer.parseInt(binstr.toString(), 2);
            data.append((char) charCode);

            int lastPixel = pixels[8];
            int r = (lastPixel >> 16) & 0xFF;
            if (r % 2 != 0) {
                break;
            }
        }

        return data.toString();
    }

    private String[] genData(String data) {
        String[] binaryData = new String[data.length()];
        for (int i = 0; i < data.length(); i++) {
            binaryData[i] = String.format("%8s", Integer.toBinaryString(data.charAt(i))).replace(' ', '0');
        }
        return binaryData;
    }
}                            