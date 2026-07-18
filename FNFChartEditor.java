import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class FNFChartEditor extends JFrame {

    public static class SongData {
        public String song = "Test";
        public double bpm = 150.0;
        public boolean needsVoices = true;
        public String player1 = "bf";
        public String player2 = "dad";
        public double speed = 1.6;
        public String audioFilePath = ""; 
        public List<Section> notes = new ArrayList<>();
    }

    public static class Section {
        public int lengthInSteps = 16;
        public boolean mustHitSection = false;
        public List<double[]> sectionNotes = new ArrayList<>(); 
    }

    private SongData activeSong = new SongData();
    private int currentSectionIndex = 0;

    private Timer playbackTimer;
    private boolean isPlaying = false;
    private Clip audioClip;

    private long positionSteps = 0;
    private double positionStepsDouble = 0;
    private long lastTickMs = 0;

    private ChartGridPanel gridPanel;
    private JTextArea shortcutsInfo; 
    
    private JTextField songNameField;
    private JSpinner bpmSpinner;
    private JSpinner speedSpinner;
    private JComboBox<String> player1Combo;
    private JComboBox<String> player2Combo;
    private JCheckBox voiceTrackCheckbox;
    private JLabel audioTrackLabel;

    private JSpinner sustainSpinner;
    private JTextField strumTimeField;
    private JComboBox<String> noteTypeCombo;

    private JSpinner densitySpinner;
    private JSpinner strengthSpinner;
    private JCheckBox ezSpamCheckbox;

    private JSlider opacitySlider;

    public FNFChartEditor() {
        setTitle("Java FNF Chart Editor");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        if (activeSong.notes.isEmpty()) {
            activeSong.notes.add(new Section());
        }

        playbackTimer = new Timer(16, e -> updatePlayback());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(800);

        gridPanel = new ChartGridPanel();
        splitPane.setLeftComponent(gridPanel);

        JPanel controlPanel = createControlPanel();
        splitPane.setRightComponent(controlPanel);

        add(splitPane);
        gridPanel.repaint();
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Charting", createChartingTab());
        tabbedPane.addTab("Data", createDataTab());
        tabbedPane.addTab("Events", createEventsTab());
        tabbedPane.addTab("Note", createNoteTab());
        tabbedPane.addTab("Spamming", createSpammingTab());
        tabbedPane.addTab("Optimiser", createOptimiserTab());
        tabbedPane.addTab("Section", createSectionTab());
        tabbedPane.addTab("Song", createSongTab());

        panel.add(tabbedPane, BorderLayout.CENTER);

        shortcutsInfo = new JTextArea();
        shortcutsInfo.setEditable(false);
        shortcutsInfo.setBackground(Color.DARK_GRAY);
        shortcutsInfo.setForeground(Color.WHITE);
        panel.add(shortcutsInfo, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createChartingTab() {
        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(new JCheckBox("Metronome Enabled"));
        p.add(new JCheckBox("Disable Autoscroll", false));
        p.add(new JCheckBox("Show Grid", true));
        p.add(new JCheckBox("Save Undos", true));
        p.add(new JLabel("Playback Rate:"));
        p.add(new JSlider(25, 400, 100));
        return p;
    }

    private JPanel createDataTab() {
        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("Song Credit:"));
        p.add(new JTextField("Test"));
        p.add(new JLabel("Credit Icon:"));
        p.add(new JTextField("bf"));
        p.add(new JCheckBox("Disable Note RGB"));
        return p;
    }

    private JPanel createEventsTab() {
        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("Event:"));
        p.add(new JComboBox<>(new String[]{"---", "Hey!", "Camera Flash", "Play Animation"}));
        p.add(new JLabel("Value 1:"));
        p.add(new JTextField());
        p.add(new JLabel("Value 2:"));
        p.add(new JTextField());
        p.add(new JButton("Add Event"));
        return p;
    }

    private JPanel createNoteTab() {
        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        p.add(new JLabel("Sustain Length (ms):"));
        sustainSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 5000.0, 50.0));
        p.add(sustainSpinner);

        p.add(new JLabel("Strum Time (ms):"));
        strumTimeField = new JTextField("0.0");
        strumTimeField.setEditable(false);
        p.add(strumTimeField);

        p.add(new JLabel("Note Type:"));
        noteTypeCombo = new JComboBox<>(new String[]{"Default", "Alt Animation", "Mine", "Hurt Note"});
        p.add(noteTypeCombo);

        return p;
    }

    private JPanel createSpammingTab() {
        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        p.add(new JLabel("Spam Density (higher = closer notes):"));
        densitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        p.add(densitySpinner);

        p.add(new JLabel("Spam Strength (how many grid row units down to fill):"));
        strengthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        p.add(strengthSpinner);

        p.add(new JLabel("Lane range for spam (0-7):"));
        SpinnerNumberModel fromModel = new SpinnerNumberModel(0, 0, 7, 1);
        SpinnerNumberModel toModel = new SpinnerNumberModel(7, 0, 7, 1);
        JSpinner laneFromSpinner = new JSpinner(fromModel);
        JSpinner laneToSpinner = new JSpinner(toModel);
        JPanel laneRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        laneRangePanel.add(new JLabel("From:"));
        laneRangePanel.add(laneFromSpinner);
        laneRangePanel.add(new JLabel("To:"));
        laneRangePanel.add(laneToSpinner);
        p.add(laneRangePanel);

        JButton spamNotesBtn = new JButton("Spam Notes Across Grids");
        spamNotesBtn.addActionListener(e -> {
            int fromLane = (int) laneFromSpinner.getValue();
            int toLane = (int) laneToSpinner.getValue();
            int density = (int) densitySpinner.getValue();
            int strength = (int) strengthSpinner.getValue();
            if (fromLane > toLane) {
                int tmp = fromLane;
                fromLane = toLane;
                toLane = tmp;
            }
            gridPanel.spamNotesForCurrentSection(fromLane, toLane, density, strength);
        });
        p.add(spamNotesBtn);

        ezSpamCheckbox = new JCheckBox("Enable EZ Spam Mode");
        p.add(ezSpamCheckbox);
        
        p.add(new JButton("Stretch Notes"));
        p.add(new JButton("Shift Notes"));
        p.add(new JButton("Duplicate Notes"));

        return p;
    }

    private JPanel createOptimiserTab() {
        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        p.add(new JLabel("Note Rendering Transparency (Optimisation):"));
        
        opacitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
        opacitySlider.setMajorTickSpacing(20);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);
        
        opacitySlider.addChangeListener(e -> {
            if (gridPanel != null) {
                gridPanel.repaint();
            }
        });
        
        p.add(opacitySlider);
        
        JCheckBox fastGridCheck = new JCheckBox("Low-Latency Grid Rendering Mode");
        p.add(fastGridCheck);

        return p;
    }

    private JPanel createSectionTab() {
        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JCheckBox mustHit = new JCheckBox("Must Hit Section (BF Camera Focus)");
        mustHit.addActionListener(e -> {
            activeSong.notes.get(currentSectionIndex).mustHitSection = mustHit.isSelected();
            gridPanel.repaint();
        });
        p.add(mustHit);

        JButton clearSec = new JButton("Clear Section");
        clearSec.addActionListener(e -> {
            activeSong.notes.get(currentSectionIndex).sectionNotes.clear();
            gridPanel.repaint();
        });
        p.add(clearSec);

        JButton swapSec = new JButton("Swap Section Sides");
        swapSec.addActionListener(e -> {
            Section sec = activeSong.notes.get(currentSectionIndex);
            for (double[] note : sec.sectionNotes) {
                double rawLane = note[1];
                if (rawLane < 4) note[1] = rawLane + 4;
                else if (rawLane >= 4 && rawLane < 8) note[1] = rawLane - 4;
            }
            gridPanel.repaint();
        });
        p.add(swapSec);

        return p;
    }

    private JPanel createSongTab() {
        JPanel p = new JPanel(new GridLayout(0, 2, 5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        p.add(new JLabel("Song:"));
        songNameField = new JTextField("Test");
        p.add(songNameField);

        p.add(new JLabel("BPM:"));
        bpmSpinner = new JSpinner(new SpinnerNumberModel(150.0, 1.0, 500.0, 1.0));
        p.add(bpmSpinner);

        p.add(new JLabel("Speed:"));
        speedSpinner = new JSpinner(new SpinnerNumberModel(1.6, 0.5, 10.0, 0.1));
        p.add(speedSpinner);

        p.add(new JLabel("Boyfriend (P1):"));
        player1Combo = new JComboBox<>(new String[]{"bf", "bf-pixel", "bf-car"});
        p.add(player1Combo);

        p.add(new JLabel("Opponent (P2):"));
        player2Combo = new JComboBox<>(new String[]{"dad", "pico", "mom", "bf-pixel-opponent"});
        p.add(player2Combo);

        voiceTrackCheckbox = new JCheckBox("Has Voices", true);
        p.add(voiceTrackCheckbox);
        p.add(new JLabel("")); 

        JButton loadAudioBtn = new JButton("Import Audio Track (.wav)");
        loadAudioBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(".");
            chooser.setDialogTitle("Select Song Audio (.wav)");
            int choice = chooser.showOpenDialog(this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                activeSong.audioFilePath = file.getAbsolutePath();
                audioTrackLabel.setText("Loaded: " + file.getName());
                loadAudioEngine(file);
            }
        });
        p.add(loadAudioBtn);
        
        audioTrackLabel = new JLabel("No Audio loaded");
        p.add(audioTrackLabel);

        JButton saveBtn = new JButton("Save JSON");
        saveBtn.addActionListener(e -> {
            syncSongDataFromUI();
            JFileChooser chooser = new JFileChooser(".");
            chooser.setDialogTitle("Save Chart JSON Structure");
            int choice = chooser.showSaveDialog(this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                saveChart(chooser.getSelectedFile());
            }
        });
        p.add(saveBtn);

        JButton loadBtn = new JButton("Import JSON");
        loadBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(".");
            chooser.setDialogTitle("Select FNF Format Chart JSON");
            int choice = chooser.showOpenDialog(this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                loadChart(chooser.getSelectedFile());
            }
        });
        p.add(loadBtn);

        return p;
    }

    private void loadAudioEngine(File file) {
        try {
            if (audioClip != null && audioClip.isOpen()) {
                audioClip.close();
            }
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            audioClip = AudioSystem.getClip();
            audioClip.open(audioStream);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Audio conversion error: " + e.getMessage());
        }
    }

    private void togglePlayback() {
        if (isPlaying) {
            isPlaying = false;
            playbackTimer.stop();
            if (audioClip != null) {
                audioClip.stop();
            }
        } else {
            isPlaying = true;
            lastTickMs = System.currentTimeMillis();
            playbackTimer.start();
            if (audioClip != null) {
                long ms = stepsToMs(positionSteps);
                audioClip.setMicrosecondPosition(ms * 1000);
                audioClip.start();
            }
        }
    }

    private long stepsToMs(long steps) {
        double stepMs = (60000.0 / (double) bpmSpinner.getValue()) / 4.0;
        return (long) (steps * stepMs);
    }

    private void applyPositionSteps(long newSteps) {
        if (activeSong.notes.isEmpty()) {
            activeSong.notes.add(new Section());
        }

        if (newSteps < 0) newSteps = 0;

        long section = newSteps / 16;
        int offset = (int) (newSteps % 16);

        if (section >= activeSong.notes.size()) {
            section = activeSong.notes.size() - 1;
            offset = 15;
            newSteps = section * 16 + offset;
        }

        positionSteps = newSteps;
        positionStepsDouble = newSteps;
        currentSectionIndex = (int) section;
        gridPanel.setScrollRowOffset(offset);
    }

    private void updatePlayback() {
        if (!isPlaying) return;

        long now = System.currentTimeMillis();
        long deltaMs = now - lastTickMs;
        if (deltaMs < 0) deltaMs = 0;
        lastTickMs = now;

        double stepMs = (60000.0 / (double) bpmSpinner.getValue()) / 4.0;
        double deltaSteps = deltaMs / stepMs;

        positionStepsDouble += deltaSteps;
        long nextSteps = (long) Math.floor(positionStepsDouble);

        if (nextSteps != positionSteps) {
            applyPositionSteps(nextSteps);
            gridPanel.repaint();
        }
    }

    private void syncSongDataFromUI() {
        activeSong.song = songNameField.getText();
        activeSong.bpm = (double) bpmSpinner.getValue();
        activeSong.speed = (double) speedSpinner.getValue();
        activeSong.player1 = (String) player1Combo.getSelectedItem();
        activeSong.player2 = (String) player2Combo.getSelectedItem();
        activeSong.needsVoices = voiceTrackCheckbox.isSelected();
    }

    private void syncSongDataToUI() {
        songNameField.setText(activeSong.song);
        bpmSpinner.setValue(activeSong.bpm);
        speedSpinner.setValue(activeSong.speed);
        player1Combo.setSelectedItem(activeSong.player1);
        player2Combo.setSelectedItem(activeSong.player2);
        voiceTrackCheckbox.setSelected(activeSong.needsVoices);
        if(!activeSong.audioFilePath.isEmpty()) {
            audioTrackLabel.setText("Loaded: " + new File(activeSong.audioFilePath).getName());
            loadAudioEngine(new File(activeSong.audioFilePath));
        }
    }

    private void saveChart(File file) {
        syncSongDataFromUI();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"song\": {\n");
        sb.append("    \"song\": \"").append(activeSong.song).append("\",\n");
        sb.append("    \"bpm\": ").append(activeSong.bpm).append(",\n");
        sb.append("    \"needsVoices\": ").append(activeSong.needsVoices).append(",\n");
        sb.append("    \"player1\": \"").append(activeSong.player1).append("\",\n");
        sb.append("    \"player2\": \"").append(activeSong.player2).append("\",\n");
        sb.append("    \"speed\": ").append(activeSong.speed).append(",\n");
        sb.append("    \"notes\": [\n");

        for (int i = 0; i < activeSong.notes.size(); i++) {
            Section section = activeSong.notes.get(i);
            sb.append("      {\n");
            sb.append("        \"lengthInSteps\": ").append(section.lengthInSteps).append(",\n");
            sb.append("        \"mustHitSection\": ").append(section.mustHitSection).append(",\n");
            sb.append("        \"sectionNotes\": [\n");

            for (int j = 0; j < section.sectionNotes.size(); j++) {
                double[] note = section.sectionNotes.get(j);
                sb.append("          [").append(format3(note[0])).append(", ").append((int) note[1]).append(", ").append(format3(note[2])).append("]");
                if (j < section.sectionNotes.size() - 1) sb.append(",\n");
                else sb.append("\n");
            }

            sb.append("        ]\n");
            sb.append("      }");
            if (i < activeSong.notes.size() - 1) sb.append(",\n");
            else sb.append("\n");
        }

        sb.append("    ]\n  },\n  \"generatedBy\": \"SNIFF ver.6\"\n}");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(sb.toString());
            JOptionPane.showMessageDialog(this, "Chart saved successfully to " + file.getName() + "!");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving chart: " + e.getMessage());
        }
    }

    private void loadChart(File file) {
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            
            String json = content.toString();
            SongData loadedSong = new SongData();

            loadedSong.song = extractJSONString(json, "song");
            loadedSong.bpm = extractJSONDouble(json, "bpm");
            loadedSong.needsVoices = extractJSONBool(json, "needsVoices");
            loadedSong.player1 = extractJSONString(json, "player1");
            loadedSong.player2 = extractJSONString(json, "player2");
            loadedSong.speed = extractJSONDouble(json, "speed");

            List<Section> sections = new ArrayList<>();
            int notesStartIndex = json.indexOf("\"notes\":");
            if (notesStartIndex != -1) {
                int sectionSearchIndex = notesStartIndex;
                while (true) {
                    int secStart = json.indexOf("{", sectionSearchIndex);
                    int secEnd = json.indexOf("}", secStart);
                    if (secStart == -1 || secEnd == -1 || secStart > json.lastIndexOf("]")) break;

                    String secBlock = json.substring(secStart, secEnd + 1);
                    Section section = new Section();
                    
                    String stepStr = extractJSONString(secBlock, "lengthInSteps");
                    if (stepStr == null || stepStr.isEmpty()) stepStr = "16";
                    section.lengthInSteps = (int) Double.parseDouble(stepStr.replaceAll("[^0-9.]", ""));
                    section.mustHitSection = secBlock.contains("\"mustHitSection\":true");

                    int arrStart = secBlock.indexOf("[[");
                    int arrEnd = secBlock.indexOf("]]");
                    if (arrStart != -1 && arrEnd != -1) {
                        String notesRaw = secBlock.substring(arrStart + 2, arrEnd);
                        if (!notesRaw.trim().isEmpty()) {
                            String[] rawNotes = notesRaw.split("\\],\\[");
                            for (String rawNote : rawNotes) {
                                String[] values = rawNote.replaceAll("[\\[\\]]", "").split(",");
                                if (values.length >= 3) {
                                    double strumTime = Double.parseDouble(values[0].trim());
                                    int noteData = (int) Double.parseDouble(values[1].trim());
                                    double sustain = Double.parseDouble(values[2].trim());
                                    section.sectionNotes.add(new double[]{strumTime, noteData, sustain});
                                }
                            }
                        }
                    }

                    sections.add(section);
                    sectionSearchIndex = secEnd + 1;
                }
            }

            if (!sections.isEmpty()) {
                loadedSong.notes = sections;
            } else {
                loadedSong.notes.add(new Section());
            }

            this.activeSong = loadedSong;
            this.currentSectionIndex = 0;
            this.positionSteps = 0;
            this.positionStepsDouble = 0;
            this.gridPanel.setScrollRowOffset(0);
            syncSongDataToUI();
            gridPanel.repaint();

        } catch (Exception e) {
            System.err.println("Error parsing json: " + e.getMessage());
        }
    }

    private String extractJSONString(String raw, String key) {
        int idx = raw.indexOf("\"" + key + "\"");
        if (idx == -1) return "";
        int colon = raw.indexOf(":", idx);
        int startQuote = raw.indexOf("\"", colon);
        if (startQuote != -1 && startQuote < raw.indexOf(",", colon)) {
            int endQuote = raw.indexOf("\"", startQuote + 1);
            return raw.substring(startQuote + 1, endQuote);
        } else {
            int nextComma = raw.indexOf(",", colon);
            if (nextComma == -1) nextComma = raw.indexOf("}", colon);
            return raw.substring(colon + 1, nextComma).trim();
        }
    }

    private double extractJSONDouble(String raw, String key) {
        try {
            return Double.parseDouble(extractJSONString(raw, key).replaceAll("[^0-9.-]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private boolean extractJSONBool(String raw, String key) {
        return extractJSONString(raw, key).contains("true");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new FNFChartEditor().setVisible(true);
        });
    }

    private static String format3(double v) {
        boolean neg = v < 0;
        double abs = neg ? -v : v;
        long scaled = Math.round(abs * 1000.0d);
        long intPart = scaled / 1000;
        long fracPart = scaled % 1000;
        String frac;
        if (fracPart < 10) frac = "00" + fracPart;
        else if (fracPart < 100) frac = "0" + fracPart;
        else frac = Long.toString(fracPart);
        return neg ? ("-" + intPart + "." + frac) : (intPart + "." + frac);
    }

    private String formatEveryThirdDigit(int value) {
        boolean neg = value < 0;
        int v = Math.abs(value);
        String s = String.valueOf(v);
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            out.append(s.charAt(i));
            count++;
            if (count == 3 && i != 0) {
                out.append(',');
                count = 0;
            }
        }
        if (neg) out.append('-');
        return out.reverse().toString();
    }

    class ChartGridPanel extends JPanel {

        private BufferedImage gridGrey;
        private BufferedImage gridWhite;
        private BufferedImage bfIcon;
        private BufferedImage dadIcon;
        private BufferedImage[] noteArrows = new BufferedImage[8];

        private int rowHeight = 60;
        private int stepsPerSection = 16;
        private int laneWidth = 60; 
        private int gridStartX = 50;
        private int gridStartY = 60; 

        private int scrollRowOffset = 0;
        
        private long lastWheelTime = 0;
        private long lastATime = 0;
        private long lastDTime = 0;

        public void setScrollRowOffset(int offset) {
            this.scrollRowOffset = offset;
            repaint();
        }

        private int mapUserLaneToVisualLane(int noteData) {
            return Math.max(0, Math.min(7, noteData));
        }

        private int visualLaneToUserLane(int visualLane0to7) {
            return Math.max(0, Math.min(7, visualLane0to7));
        }

        public ChartGridPanel() {
            loadAssets();
            setFocusable(true);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();

                    int clickedLaneVisual = (e.getX() - gridStartX) / laneWidth;
                    int clickedRowVisual = (e.getY() - gridStartY) / rowHeight;
                    int clickedRow = clickedRowVisual + scrollRowOffset;

                    if (clickedLaneVisual >= 0 && clickedLaneVisual < 8 && clickedRowVisual >= 0 && clickedRowVisual < stepsPerSection) {
                        int clickedUserLane = visualLaneToUserLane(clickedLaneVisual);

                        if (ezSpamCheckbox != null && ezSpamCheckbox.isSelected() && !SwingUtilities.isRightMouseButton(e)) {
                            int density = (int) densitySpinner.getValue();
                            int strength = (int) strengthSpinner.getValue();
                            spamNotesForCurrentSection(clickedUserLane, clickedUserLane, density, strength);
                        } else {
                            Section currentSec = activeSong.notes.get(currentSectionIndex);
                            double stepTime = rowToMs(clickedRow);

                            double sustain = (double) sustainSpinner.getValue();
                            double timeTol = 0.5; 
                            
                            if (SwingUtilities.isRightMouseButton(e)) {
                                int before = currentSec.sectionNotes.size();
                                currentSec.sectionNotes.removeIf(n -> Math.abs(n[0] - stepTime) <= timeTol && (int) n[1] == clickedUserLane);
                                if (currentSec.sectionNotes.size() != before) repaint();
                            } else {
                                boolean replaced = false;
                                for (int i = 0; i < currentSec.sectionNotes.size(); i++) {
                                    double[] n = currentSec.sectionNotes.get(i);
                                    if (Math.abs(n[0] - stepTime) <= timeTol && (int) n[1] == clickedUserLane) {
                                        n[2] = sustain;
                                        replaced = true;
                                        break;
                                    }
                                }
                                if (!replaced) {
                                    currentSec.sectionNotes.add(new double[]{stepTime, clickedUserLane, sustain});
                                }
                                strumTimeField.setText(String.format("%.2f", stepTime));
                                repaint();
                            }
                        }
                    }
                }
            });

            addMouseWheelListener(e -> {
                long now = System.currentTimeMillis();
                if (now - lastWheelTime > 100) {
                    if (e.getWheelRotation() > 0) {
                        positionSteps += 16;
                    } else {
                        positionSteps -= 16;
                    }
                    applyPositionSteps(positionSteps);
                    lastWheelTime = now;
                }
            });

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int keyCode = e.getKeyCode();
                    long now = System.currentTimeMillis();

                    if (keyCode == KeyEvent.VK_SPACE) {
                        togglePlayback();
                        return;
                    }

                    if (keyCode == KeyEvent.VK_S) {
                        positionSteps++;
                        applyPositionSteps(positionSteps);
                        repaint();
                    } else if (keyCode == KeyEvent.VK_W) {
                        positionSteps--;
                        applyPositionSteps(positionSteps);
                        repaint();
                    } else if (keyCode == KeyEvent.VK_D) {
                        if (now - lastDTime > 100) {
                            positionSteps += 16;
                            applyPositionSteps(positionSteps);
                            lastDTime = now;
                            repaint();
                        }
                    } else if (keyCode == KeyEvent.VK_A) {
                        if (now - lastATime > 100) {
                            positionSteps -= 16;
                            applyPositionSteps(positionSteps);
                            lastATime = now;
                            repaint();
                        }
                    }
                }
            });
        }

        private double rowToMs(int row) {
            double sectionStartTime = currentSectionIndex * (4 * (60000.0 / activeSong.bpm));
            double stepTimeMs = (60000.0 / activeSong.bpm) / 4.0;
            return sectionStartTime + (row * stepTimeMs);
        }

        private void loadAssets() {
            try {
                gridGrey = ImageIO.read(new File("assets/charteditor/GridGrey.png"));
                gridWhite = ImageIO.read(new File("assets/charteditor/GridWhite.png"));
                
                bfIcon = ImageIO.read(new File("icons/bf.png"));
                dadIcon = ImageIO.read(new File("icons/dad.png"));

                noteArrows[0] = ImageIO.read(new File("notes/LeftComing.png"));
                noteArrows[1] = ImageIO.read(new File("notes/DownComing.png"));
                noteArrows[2] = ImageIO.read(new File("notes/UpComing.png"));
                noteArrows[3] = ImageIO.read(new File("notes/RightComing.png"));

                System.arraycopy(noteArrows, 0, noteArrows, 4, 4);
            } catch (Exception e) {
                System.out.println("Assets/Icons failed to load, falling back to layout shapes.");
            }
        }

        public void spamNotesForCurrentSection(int laneFrom, int laneTo, int densityValue, int strengthRowsToFill) {
            Section currentSec = activeSong.notes.get(currentSectionIndex);

            int safeDensity = Math.max(1, densityValue);
            int safeRowsToFill = Math.max(1, strengthRowsToFill);

            double sectionStartTime = currentSectionIndex * (4 * (60000.0 / activeSong.bpm));
            double stepTimeMs = (60000.0 / activeSong.bpm) / 4.0;

            int minLane = Math.min(laneFrom, laneTo);
            int maxLane = Math.max(laneFrom, laneTo);
            
            currentSec.sectionNotes.removeIf(n -> {
                int l = (int) n[1];
                return l >= minLane && l <= maxLane;
            });

            double rowStep = 1.0 / safeDensity;
            double targetMaxRow = (double) safeRowsToFill;

            for (double row = 0; row < targetMaxRow; row += rowStep) {
                double time = sectionStartTime + (row * stepTimeMs);

                for (int targetLane = minLane; targetLane <= maxLane; targetLane++) {
                    double sustain = (double) sustainSpinner.getValue();
                    currentSec.sectionNotes.add(new double[]{time, targetLane, sustain});
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            int totalNotesInSong = 0;
            for (Section s : activeSong.notes) {
                totalNotesInSong += s.sectionNotes.size();
            }

            Section secForCount = activeSong.notes.get(currentSectionIndex);
            int notesInThisSection = secForCount.sectionNotes.size();

            int renderedNotes = 0;
            double sectionStartTime = currentSectionIndex * (4 * (60000.0 / activeSong.bpm));
            double stepTimeMs = (60000.0 / activeSong.bpm) / 4.0;

            for (double[] note : secForCount.sectionNotes) {
                double relativeTime = note[0] - sectionStartTime;
                double rowActualDouble = relativeTime / stepTimeMs;
                double rowVisualDouble = rowActualDouble - scrollRowOffset;
                int userLane = (int) note[1];

                if (rowActualDouble >= -0.1 && rowActualDouble < stepsPerSection && 
                    rowVisualDouble >= -0.1 && rowVisualDouble < stepsPerSection && 
                    userLane >= 0 && userLane <= 7) {
                    renderedNotes++;
                }
            }

            if (shortcutsInfo != null) {
                shortcutsInfo.setText(
                    " SPACE BAR - Play / Pause Song & Auto-Scroll Tracker\n" +
                    " W/S - Scroll Grid Up/Down (Changes Section at limits)\n" +
                    " A/D - Prev/Next Section\n" +
                    " Mouse Wheel - Scroll Sections\n" +
                    " Left Click - Place Note | Right Click - Delete Note\n" +
                    " -----------------------------------------------------\n" +
                    " Total Song Notes: " + formatEveryThirdDigit(totalNotesInSong) + "\n" +
                    " Section Notes: " + formatEveryThirdDigit(notesInThisSection) + "\n" +
                    " Rendered Notes: " + formatEveryThirdDigit(renderedNotes)
                );
            }

            g2d.setColor(Color.WHITE);
            g2d.drawString(
                "Section: " + currentSectionIndex + " | Grid Offset: " + scrollRowOffset +
                " | Status: " + (isPlaying ? "PLAYING" : "PAUSED") +
                " | Notes: " + formatEveryThirdDigit(totalNotesInSong),
                20, 20
            );

            boolean mustHit = activeSong.notes.get(currentSectionIndex).mustHitSection;
            BufferedImage leftSideIcon = mustHit ? bfIcon : dadIcon;
            BufferedImage rightSideIcon = mustHit ? dadIcon : bfIcon;

            if (leftSideIcon != null) {
                g2d.drawImage(leftSideIcon, gridStartX + (2 * laneWidth) - 25, gridStartY - 50, 50, 50, null);
            } else {
                g2d.setColor(Color.RED);
                g2d.drawString(mustHit ? "BF Side" : "DAD Side", gridStartX + laneWidth, gridStartY - 15);
            }

            if (rightSideIcon != null) {
                g2d.drawImage(rightSideIcon, gridStartX + (6 * laneWidth) - 25, gridStartY - 50, 50, 50, null);
            } else {
                g2d.setColor(Color.GREEN);
                g2d.drawString(mustHit ? "DAD Side" : "BF Side", gridStartX + (5 * laneWidth), gridStartY - 15);
            }

            for (int rowVisual = 0; rowVisual < stepsPerSection; rowVisual++) {
                int rowActual = rowVisual + scrollRowOffset;

                for (int laneVisual = 0; laneVisual < 8; laneVisual++) {
                    int x = gridStartX + (laneVisual * laneWidth);
                    int y = gridStartY + (rowVisual * rowHeight);

                    BufferedImage gridImg = ((rowActual / 4) % 2 == 0) ? gridGrey : gridWhite;
                    if (gridImg != null) {
                        g2d.drawImage(gridImg, x, y, laneWidth, rowHeight, null);
                    } else {
                        g2d.setColor((rowActual % 2 == 0) ? Color.DARK_GRAY : Color.GRAY);
                        g2d.fillRect(x, y, laneWidth, rowHeight);
                    }
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(x, y, laneWidth, rowHeight);
                }
            }

            g2d.setColor(Color.CYAN);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(gridStartX + (4 * laneWidth), gridStartY, gridStartX + (4 * laneWidth), gridStartY + (stepsPerSection * rowHeight));

            float opacity = 1.0f;
            if (opacitySlider != null) {
                opacity = opacitySlider.getValue() / 100.0f;
            }

            Composite originalComposite = g2d.getComposite();
            Section sec = activeSong.notes.get(currentSectionIndex);

            for (double[] note : sec.sectionNotes) {
                double time = note[0];
                int userLane = (int) note[1]; 
                double sustain = note[2];

                double relativeTime = time - sectionStartTime;
                double rowActualDouble = relativeTime / stepTimeMs;
                double rowVisualDouble = rowActualDouble - scrollRowOffset;

                if (rowActualDouble >= -0.1 && rowActualDouble < stepsPerSection && rowVisualDouble >= -0.1 && rowVisualDouble < stepsPerSection && userLane >= 0 && userLane <= 7) {
                    int laneVisual = mapUserLaneToVisualLane(userLane);
                    int x = gridStartX + (laneVisual * laneWidth);
                    int y = gridStartY + (int) Math.round(rowVisualDouble * rowHeight);

                    if (opacity > 0.0f) {
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

                        if (sustain > 0) {
                            int tailHeight = (int) ((sustain / stepTimeMs) * rowHeight);
                            g2d.setColor(new Color(0, 255, 0, 150));
                            g2d.fillRect(x + (laneWidth / 3), y + (rowHeight / 2), laneWidth / 3, tailHeight);
                        }

                        if (noteArrows[laneVisual] != null) {
                            g2d.drawImage(noteArrows[laneVisual], x, y, laneWidth, rowHeight, null);
                        } else {
                            g2d.setColor(Color.YELLOW);
                            g2d.fillOval(x + 2, y + 2, laneWidth - 4, rowHeight - 4);
                        }
                        g2d.setComposite(originalComposite);
                    }
                }
            }
        }
    }
}
