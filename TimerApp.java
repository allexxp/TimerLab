import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

// Acțiunea timerului 1: semnal sonor repetat
class SoundPlayer extends TimerTask {
    private final IntConsumer onTick;
    private int count = 0;

    public SoundPlayer(IntConsumer onTick) {
        this.onTick = onTick;
    }

    @Override
    public void run() {
        Toolkit.getDefaultToolkit().beep();
        count++;
        System.out.println("Sunet redat nr. " + count);
        onTick.accept(count);
    }
}

// Acțiunea timerului 3: afișarea unui mesaj la un moment exact
class Message extends TimerTask {
    private final String msg;
    private final Consumer<String> out;

    public Message(String msg, Consumer<String> out) {
        this.msg = msg;
        this.out = out;
    }

    @Override
    public void run() {
        System.out.println(msg);
        out.accept(msg);
    }
}

public class TimerApp extends JFrame {
    private static final SimpleDateFormat HMS = new SimpleDateFormat("HH:mm:ss");

    private Timer soundTimer, stopTimer, clockTimer, progressTimer;

    private final JLabel lbl1 = new JLabel();
    private final JLabel lbl2 = new JLabel();
    private final JLabel lbl3 = new JLabel();
    private final JProgressBar bar = new JProgressBar(0, 100);
    private final JTextArea log = new JTextArea(9, 40);
    private final JSpinner seconds = new JSpinner(new SpinnerNumberModel(10, 6, 120, 1));
    private final JButton startBtn = new JButton("Pornește timerele");
    private final JButton stopBtn = new JButton("Oprește tot");

    public TimerApp() {
        super("Laborator 1 – Timere (Timer / TimerTask)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panou cu starea celor trei timere
        JPanel status = new JPanel(new GridLayout(4, 1, 4, 4));
        status.setBorder(BorderFactory.createTitledBorder("Starea timerelor"));
        status.add(lbl1);
        status.add(lbl2);
        status.add(lbl3);
        bar.setStringPainted(true);
        status.add(bar);

        // Panou de comandă
        JPanel top = new JPanel();
        top.add(new JLabel("Mesajul final peste (secunde):"));
        top.add(seconds);
        top.add(startBtn);
        top.add(stopBtn);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createTitledBorder("Jurnal"));

        add(top, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(status, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        startBtn.addActionListener(e -> startTimers());
        stopBtn.addActionListener(e -> {
            cancelAll();
            log("Toate timerele au fost oprite manual.");
        });

        resetLabels();
        pack();
        setLocationRelativeTo(null);
    }

    private void resetLabels() {
        lbl1.setText("Timer 1 (scheduleAtFixedRate, 2 s): oprit");
        lbl2.setText("Timer 2 (schedule, 5 s): în așteptare");
        lbl3.setText("Timer 3 (schedule, moment exact): în așteptare");
        bar.setValue(0);
        bar.setString("0%");
    }

    private void ui(Runnable r) {
        SwingUtilities.invokeLater(r);
    }

    private void log(String s) {
        ui(() -> {
            log.append("[" + HMS.format(new Date()) + "] " + s + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    private void cancelAll() {
        for (Timer t : new Timer[]{soundTimer, stopTimer, clockTimer, progressTimer}) {
            if (t != null) t.cancel();
        }
    }

    private void startTimers() {
        cancelAll();
        resetLabels();
        log.setText("");
        final int sec = (Integer) seconds.getValue();
        final long t0 = System.currentTimeMillis();
        log("Aplicația a pornit. Timerele au fost setate.");

        // ===== TIMER 1: cu perioadă indicată – semnal sonor la fiecare 2 secunde =====
        soundTimer = new Timer();
        soundTimer.scheduleAtFixedRate(new SoundPlayer(n -> {
            ui(() -> {
                lbl1.setForeground(new Color(0, 110, 0));
                lbl1.setText("Timer 1 (scheduleAtFixedRate, 2 s): semnal sonor nr. " + n);
            });
            log("Sunet redat nr. " + n);
        }), 0, 2000);

        // ===== TIMER 2: la un interval de timp – după 5 secunde oprește Timerul 1 =====
        stopTimer = new Timer();
        stopTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                soundTimer.cancel();
                System.out.println("Au trecut 5 secunde. Opresc sunetul.");
                log("Au trecut 5 secunde. Opresc sunetul.");
                ui(() -> {
                    lbl1.setForeground(Color.GRAY);
                    lbl1.setText("Timer 1: oprit prin cancel()");
                    lbl2.setForeground(new Color(0, 110, 0));
                    lbl2.setText("Timer 2 (schedule, 5 s): executat");
                });
            }
        }, 5000);

        // ===== TIMER 3: la un anumit timp – moment exact (acum + sec secunde) =====
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, sec);
        Date dataExacta = calendar.getTime();
        lbl3.setText("Timer 3 (schedule, moment exact): planificat la " + HMS.format(dataExacta));
        clockTimer = new Timer();
        clockTimer.schedule(new Message(">>> Este ora mesei! <<<", m -> {
            log(m);
            ui(() -> {
                lbl3.setForeground(new Color(0, 110, 0));
                lbl3.setText("Timer 3: " + m + " (" + HMS.format(new Date()) + ")");
            });
        }), dataExacta);

        // ===== TIMER 4: auxiliar – actualizează bara de progres a interfeței =====
        progressTimer = new Timer();
        progressTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int p = (int) Math.min(100, (System.currentTimeMillis() - t0) * 100 / (sec * 1000L));
                ui(() -> {
                    bar.setValue(p);
                    bar.setString(p + "%");
                });
                if (p >= 100) cancel();
            }
        }, 0, 100);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TimerApp().setVisible(true));
    }
}