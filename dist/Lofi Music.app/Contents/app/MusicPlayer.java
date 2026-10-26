import java.awt.*;
import javax.swing.*;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

public class MusicPlayer {

public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        JFrame frame = new JFrame("Lo-fi Music");
        frame.setSize(900, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(new Color(25, 25, 35));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );
        // Application heading
        JLabel heading = new JLabel(
                "LO-FI MUSIC",
                SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 26));
        heading.setForeground(Color.WHITE);
        mainPanel.add(heading, BorderLayout.NORTH);
        // Playlist section
        JPanel playlistPanel = new JPanel(new BorderLayout(10, 10));
        playlistPanel.setBackground(new Color(38, 38, 52));
        playlistPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );
        JLabel playlistHeading = new JLabel("YOUR PLAYLIST");
        playlistHeading.setForeground(Color.WHITE);
        playlistHeading.setFont(new Font("Arial", Font.BOLD, 18));
        DefaultListModel<String> songModel = new DefaultListModel<>();

        java.util.ArrayList<java.io.File> songFiles = new java.util.ArrayList<>();

        AudioPlayer audioPlayer = new AudioPlayer();
        int[] currentSongIndex = {-1};

        JList<String> songList = new JList<>(songModel);
        songList.setBackground(new Color(38, 38, 52));
        songList.setForeground(Color.WHITE);
        songList.setSelectionBackground(new Color(100, 75, 150));
        JScrollPane scrollPane = new JScrollPane(songList);
        JButton addButton = new JButton("Add Songs");
        playlistPanel.add(playlistHeading, BorderLayout.NORTH);
        playlistPanel.add(scrollPane, BorderLayout.CENTER);

        addButton.addActionListener(event -> {

    JFileChooser fileChooser = new JFileChooser();

    fileChooser.setDialogTitle("Select Music Files");

    fileChooser.setMultiSelectionEnabled(true);

    int result = fileChooser.showOpenDialog(frame);

    if (result == JFileChooser.APPROVE_OPTION) {

        java.io.File[] selectedFiles =
                fileChooser.getSelectedFiles();

        for (java.io.File file : selectedFiles) {

            String fileName = file.getName().toLowerCase();

            if (fileName.endsWith(".mp3")
                    || fileName.endsWith(".wav")
                    || fileName.endsWith(".aiff")
                    || fileName.endsWith(".aif")) {

                songFiles.add(file);

                songModel.addElement(file.getName());

            }
        }
    }
});

        playlistPanel.add(addButton, BorderLayout.SOUTH);
        // Player section
        JPanel playerPanel = new JPanel();
        playerPanel.setLayout(
                new BoxLayout(playerPanel, BoxLayout.Y_AXIS)
        );
        playerPanel.setBackground(new Color(38, 38, 52));
        playerPanel.setBorder(
                BorderFactory.createEmptyBorder(25, 20, 25, 20)
        );
        JLabel musicIcon = new JLabel("♫");
        musicIcon.setFont(new Font("Serif", Font.PLAIN, 100));
        musicIcon.setForeground(new Color(190, 160, 255));
        musicIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel songTitle = new JLabel("No Song Selected");
        songTitle.setFont(new Font("Arial", Font.BOLD, 20));
        songTitle.setForeground(Color.WHITE);
        songTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel artistLabel = new JLabel("Select a song from your library");
        artistLabel.setForeground(Color.LIGHT_GRAY);
        artistLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSlider progressSlider = new JSlider(0, 1000, 0);
progressSlider.setOpaque(false);
progressSlider.setEnabled(true);
progressSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
progressSlider.setMaximumSize(new Dimension(300, 40));

JLabel timeLabel = new JLabel("00:00 / 00:00");
timeLabel.setForeground(Color.LIGHT_GRAY);
timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

// Track slider movement
final boolean[] dragging = {false};

progressSlider.addChangeListener(e -> {
    if (progressSlider.getValueIsAdjusting()) {
        dragging[0] = true;
    } else if (dragging[0]) {
        long duration = audioPlayer.getDuration();

        long position =
                duration * progressSlider.getValue() / 1000;

        audioPlayer.seekTo(position);
        dragging[0] = false;
    }
});

// Update slider and time every 250 milliseconds
Timer progressTimer = new Timer(250, e -> {
    long position = audioPlayer.getPosition();
    long duration = audioPlayer.getDuration();

    if (duration > 0) {
        if (!dragging[0]) {
            int progress = (int) (
                    position * 1000 / duration
            );

            progressSlider.setValue(progress);
        }

        timeLabel.setText(
                formatTime(position) + " / " + formatTime(duration)
        );
    } else {
        progressSlider.setValue(0);
        timeLabel.setText("00:00 / 00:00");
    }
});

progressTimer.start();

        // Playback buttons
        JPanel controls = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 10)
        );
        controls.setOpaque(false);
        
        //Play
        JButton playButton = new JButton("Play");
        playButton.addActionListener(event -> {

    int selectedIndex = songList.getSelectedIndex();

    // Automatically select the first song if none is selected

    if (selectedIndex == -1 && !songFiles.isEmpty()) {

        selectedIndex = 0;

        songList.setSelectedIndex(0);

    }

    // Check whether a song is available

    if (selectedIndex == -1 || songFiles.isEmpty()) {

        JOptionPane.showMessageDialog(

                frame,

                "Please add a song to your playlist first."

        );

        return;

    }

    java.io.File selectedSong = songFiles.get(selectedIndex);
    loadSongPoster(musicIcon, selectedSong);

    // Play MP3 files

    if (selectedSong.getName().toLowerCase().endsWith(".mp3")) {

        currentSongIndex[0] = selectedIndex;

        songTitle.setText(selectedSong.getName());

        artistLabel.setText("Now Playing");

        audioPlayer.playMP3(selectedSong);

    } else {

        JOptionPane.showMessageDialog(

                frame,

                "Currently, only MP3 playback is supported."

        );

    }

});
    
    JButton previousButton = new JButton("Previous");
    previousButton.addActionListener(event -> {

    if (songFiles.isEmpty()) {
        JOptionPane.showMessageDialog(frame, "Please add songs first.");
        return;
    }

    int index = currentSongIndex[0];

    if (index == -1) {
        index = songList.getSelectedIndex();
    }

    if (index <= 0) {
        index = songFiles.size() - 1;
    } else {
        index--;
    }

    currentSongIndex[0] = index;
    songList.setSelectedIndex(index);

    java.io.File previousSong = songFiles.get(index);
    loadSongPoster(musicIcon, previousSong);

    songTitle.setText(previousSong.getName());
    artistLabel.setText("Now Playing");

    if (previousSong.getName().toLowerCase().endsWith(".mp3")) {
        audioPlayer.playMP3(previousSong);
    } else {
        JOptionPane.showMessageDialog(
                frame,
                "Currently, only MP3 playback is supported."
        );
    }
});

        JButton pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> {
    if (pauseButton.getText().equals("Pause")) {
        audioPlayer.pause();
        pauseButton.setText("Resume");
    } else {
        audioPlayer.resume();
        pauseButton.setText("Pause");
    }
});

        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(event -> {

    if (songFiles.isEmpty()) {
        JOptionPane.showMessageDialog(frame, "Please add songs first.");
        return;
    }

    if (currentSongIndex[0] == -1) {
        currentSongIndex[0] = 0;
    } else {
        currentSongIndex[0] =
                (currentSongIndex[0] + 1) % songFiles.size();
    }

    songList.setSelectedIndex(currentSongIndex[0]);
    playButton.doClick();
});

        controls.add(previousButton);
        controls.add(playButton);
        controls.add(pauseButton);
        controls.add(nextButton);
        // Lofi mode switch
        JCheckBox lofiSwitch = new JCheckBox("LOFI MODE");
        lofiSwitch.setFont(new Font("Arial", Font.BOLD, 16));
        lofiSwitch.setForeground(new Color(200, 175, 255));
        lofiSwitch.setBackground(new Color(38, 38, 52));
        lofiSwitch.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lofiStatus = new JLabel("LOFI effects: OFF");
        lofiStatus.setForeground(Color.LIGHT_GRAY);
        lofiStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        lofiSwitch.addActionListener(event -> {
        boolean enabled = lofiSwitch.isSelected();

        audioPlayer.setLofiEnabled(enabled);
        if (enabled) {
        lofiStatus.setText("LOFI effects: ON");
        } else {
        lofiStatus.setText("LOFI effects: OFF");
}

});

        // Volume control
        JLabel volumeLabel = new JLabel("VOLUME");
        volumeLabel.setForeground(Color.WHITE);
        volumeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSlider volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.addChangeListener(e -> {
        audioPlayer.setVolume(volumeSlider.getValue());
        });

        volumeSlider.setOpaque(false);
        volumeSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
        playerPanel.add(musicIcon);
        playerPanel.add(Box.createVerticalStrut(10));
        playerPanel.add(songTitle);
        playerPanel.add(Box.createVerticalStrut(5));
        playerPanel.add(artistLabel);
        playerPanel.add(Box.createVerticalStrut(15));
        playerPanel.add(progressSlider);
        playerPanel.add(timeLabel);
        playerPanel.add(Box.createVerticalStrut(10));
        playerPanel.add(controls);
        playerPanel.add(Box.createVerticalStrut(15));
        playerPanel.add(lofiSwitch);
        playerPanel.add(Box.createVerticalStrut(5));
        playerPanel.add(lofiStatus);
        playerPanel.add(Box.createVerticalStrut(15));
        playerPanel.add(volumeLabel);
        playerPanel.add(volumeSlider);
        // Arrange playlist and player side by side
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        centerPanel.setOpaque(false);
        centerPanel.add(playlistPanel);
        centerPanel.add(playerPanel);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        frame.add(mainPanel);
        frame.setVisible(true);
    });
}
static String formatTime(long milliseconds) {
    long seconds = milliseconds / 1000;
    long minutes = seconds / 60;
    seconds = seconds % 60;

    return String.format("%02d:%02d", minutes, seconds);
}

static void loadSongPoster(JLabel musicIcon, java.io.File songFile) {
try {
AudioFile audioFile = AudioFileIO.read(songFile);
Tag tag = audioFile.getTag();

    if (tag != null) {
        Artwork artwork = tag.getFirstArtwork();
        if (artwork != null) {
            byte[] imageData = artwork.getBinaryData();
            ImageIcon icon = new ImageIcon(imageData);
            Image image = icon.getImage().getScaledInstance(
                    220, 220, Image.SCALE_SMOOTH
            );
            musicIcon.setText("");
            musicIcon.setIcon(new ImageIcon(image));
            musicIcon.setPreferredSize(new Dimension(220, 220));
            return;
        }
    }
    showDefaultMusicIcon(musicIcon);
} catch (Exception e) {
    System.out.println("Could not load album artwork: " + e.getMessage());
    showDefaultMusicIcon(musicIcon);
}

}

static void showDefaultMusicIcon(JLabel musicIcon) {
musicIcon.setIcon(null);
musicIcon.setIcon(null);
musicIcon.setText("\u266B");
musicIcon.setForeground(new Color(190, 160, 255));
}

}