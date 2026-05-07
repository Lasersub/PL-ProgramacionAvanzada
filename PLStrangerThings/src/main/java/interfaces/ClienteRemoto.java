package interfaces;

import clases.ISimulacionRemota;
import java.rmi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class ClienteRemoto extends JFrame {

    private ISimulacionRemota servidor;

    // ── Hawkins ────────────────────────────────────────────────────────────
    private JLabel numNinosHawkins  = valueLabel("-");
    private JLabel numSangreHawkins = valueLabel("-");

    // ── Portales ───────────────────────────────────────────────────────────
    private JLabel numNinosPortalLaboratorio     = valueLabel("- niños");
    private JLabel numNinosPortalCentroComercial = valueLabel("- niños");
    private JLabel numNinosPortalBosque          = valueLabel("- niños");
    private JLabel numNinosPortalAlcantarillado  = valueLabel("- niños");

    // ── Upside Down — niños ────────────────────────────────────────────────
    private JLabel numNinosLaboratorio       = valueLabel("- niños");
    private JLabel numNinosCentroComercial   = valueLabel("- niños");
    private JLabel numNinosBosque            = valueLabel("- niños");
    private JLabel numNinosAlcantarillado    = valueLabel("- niños");
    private JLabel numNinosCapturadosColmena = valueLabel("- niños");

    // ── Upside Down — demogorgons ──────────────────────────────────────────
    private JLabel numDemogLaboratorio     = valueLabel("- demogorgons");
    private JLabel numDemogCentroComercial = valueLabel("- demogorgons");
    private JLabel numDemogBosque          = valueLabel("- demogorgons");
    private JLabel numDemogAlcantarillado  = valueLabel("- demogorgons");

    // ── Ranking ────────────────────────────────────────────────────────────
    private JLabel idPrimerDemog           = subLabel("N0000");
    private JLabel numCapturasPrimerDemog  = valueLabel("- capturas");
    private JLabel idSegundoDemog          = subLabel("-");
    private JLabel numCapturasSegundoDemog = valueLabel("- capturas");
    private JLabel idTercerDemog           = subLabel("-");
    private JLabel numCapturasTercerDemog  = valueLabel("- capturas");

    // ── Evento ─────────────────────────────────────────────────────────────
    private JLabel eventoActivo       = valueLabel("Ningún evento activo");
    private JLabel segsRestantesEvento = valueLabel("- seg");

    // ── Botón ──────────────────────────────────────────────────────────────
    private JButton BotonDeControl;

    // ── Factorías de labels ────────────────────────────────────────────────

    private static JLabel valueLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Yatra One", Font.BOLD, 18));
        l.setForeground(Color.WHITE);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setBorder(BorderFactory.createLineBorder(new Color(242, 242, 242)));
        return l;
    }

    private static JLabel subLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Yatra One", Font.BOLD, 18));
        l.setForeground(new Color(204, 0, 0));
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    private static JLabel titleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Yatra One", Font.BOLD, 36));
        l.setForeground(new Color(155, 0, 50));
        return l;
    }

    private static JPanel valuePanel(JLabel label, int w, int h) {
        JPanel p = new JPanel(null);
        p.setBackground(new Color(155, 0, 50));
        label.setBounds(0, 0, w, h);
        p.add(label);
        return p;
    }

    private static JPanel darkPanel() {
        JPanel p = new JPanel(null);
        p.setBackground(new Color(38, 0, 0));
        p.setBorder(new javax.swing.border.LineBorder(new Color(155, 0, 50), 3, true));
        return p;
    }

    // ── Constructor ────────────────────────────────────────────────────────

    public ClienteRemoto() {
        setTitle("Cliente Remoto - Hawkins");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        pack();
        setLocationRelativeTo(null);

        try {
            servidor = (ISimulacionRemota) Naming.lookup("rmi://localhost/SimulacionHawkins");
            javax.swing.Timer timer = new javax.swing.Timer(500, e -> actualizarDatos());
            timer.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error al conectar con el servidor RMI:\n" + e.getMessage(),
                "Error de conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── initComponents ─────────────────────────────────────────────────────

    private void initComponents() {
        JPanel content = new JPanel(null);
        content.setBackground(Color.BLACK);
        content.setPreferredSize(new Dimension(1200, 555));
        setContentPane(content);

        // Títulos de sección
        JLabel lblHawkins = titleLabel("Hawkins");
        lblHawkins.setBounds(20, 30, 310, 40);
        content.add(lblHawkins);

        JLabel lblPortales = titleLabel("Portales");
        lblPortales.setBounds(20, 230, 180, 40);
        content.add(lblPortales);

        JLabel lblUD = titleLabel("Upside Down");
        lblUD.setBounds(350, 30, 510, 40);
        content.add(lblUD);

        JLabel lblCapturas = titleLabel("Capturas");
        lblCapturas.setBounds(870, 30, 310, 40);
        content.add(lblCapturas);

        JLabel lblEvento = titleLabel("Evento actual");
        lblEvento.setBounds(870, 360, 310, 40);
        content.add(lblEvento);

        // ── Panel Hawkins ──────────────────────────────────────────────────
        JPanel pHawkins = darkPanel();
        pHawkins.setBounds(20, 70, 310, 140);

        JLabel lNinos = subLabel("Niños");
        lNinos.setBounds(40, 20, 90, 22);
        pHawkins.add(lNinos);

        JLabel lSangre = subLabel("Sangre");
        lSangre.setBounds(180, 20, 90, 22);
        pHawkins.add(lSangre);

        JPanel vpNinosH = valuePanel(numNinosHawkins, 90, 60);
        vpNinosH.setBounds(40, 60, 90, 60);
        pHawkins.add(vpNinosH);

        JPanel vpSangreH = valuePanel(numSangreHawkins, 90, 60);
        vpSangreH.setBounds(180, 60, 90, 60);
        pHawkins.add(vpSangreH);

        content.add(pHawkins);

        // ── Panel Portales ─────────────────────────────────────────────────
        JPanel pPortales = darkPanel();
        pPortales.setBounds(20, 270, 310, 290);

        JLabel lEstado = subLabel("Estado actual");
        lEstado.setBounds(0, 10, 300, 22);
        pPortales.add(lEstado);

        String[]  portalNames  = {"Laboratorio", "Centro Comercial", "Bosque", "Alcantarillado"};
        JLabel[]  portalVals   = {numNinosPortalLaboratorio, numNinosPortalCentroComercial,
                                  numNinosPortalBosque, numNinosPortalAlcantarillado};
        int[]     portalY      = {50, 110, 170, 230};

        for (int i = 0; i < 4; i++) {
            JLabel name = subLabel(portalNames[i]);
            name.setBounds(0, portalY[i], 180, 40);
            pPortales.add(name);

            JPanel vp = valuePanel(portalVals[i], 110, 40);
            vp.setBounds(180, portalY[i], 110, 40);
            pPortales.add(vp);
        }

        content.add(pPortales);

        // ── Panel Upside Down ──────────────────────────────────────────────
        JPanel pUD = darkPanel();
        pUD.setBounds(350, 70, 500, 380);

        JLabel lSeres = subLabel("Seres vivos detectados en las ubicaciones");
        lSeres.setBounds(0, 20, 510, 22);
        pUD.add(lSeres);

        String[] udNames     = {"Laboratorio", "Centro Comercial", "Bosque", "Alcantarillado"};
        JLabel[] udNinos     = {numNinosLaboratorio, numNinosCentroComercial,
                                numNinosBosque, numNinosAlcantarillado};
        JLabel[] udDemogs    = {numDemogLaboratorio, numDemogCentroComercial,
                                numDemogBosque, numDemogAlcantarillado};
        int[]    udY         = {70, 130, 190, 250};

        for (int i = 0; i < 4; i++) {
            JLabel name = subLabel(udNames[i]);
            name.setBounds(0, udY[i], 190, 40);
            pUD.add(name);

            JPanel vpN = valuePanel(udNinos[i], 110, 40);
            vpN.setBounds(190, udY[i], 110, 40);
            pUD.add(vpN);

            JPanel vpD = valuePanel(udDemogs[i], 160, 40);
            vpD.setBounds(320, udY[i], 160, 40);
            pUD.add(vpD);
        }

        JLabel lColmena = new JLabel("Capturados en colmena");
        lColmena.setFont(new Font("Yatra One", Font.BOLD, 24));
        lColmena.setForeground(new Color(155, 0, 50));
        lColmena.setHorizontalAlignment(SwingConstants.CENTER);
        lColmena.setBounds(0, 320, 330, 40);
        pUD.add(lColmena);

        JPanel vpColmena = valuePanel(numNinosCapturadosColmena, 110, 40);
        vpColmena.setBounds(350, 320, 110, 40);
        pUD.add(vpColmena);

        content.add(pUD);

        // ── Panel Ranking ──────────────────────────────────────────────────
        JPanel pRanking = darkPanel();
        pRanking.setBounds(870, 70, 310, 270);

        JLabel lDemogMayor = subLabel("Demogorgons con mayor");
        lDemogMayor.setBounds(0, 20, 310, 22);
        pRanking.add(lDemogMayor);

        JLabel lNumCap = subLabel("número de capturas");
        lNumCap.setBounds(0, 40, 310, 22);
        pRanking.add(lNumCap);

        String[] ordinals    = {"1:", "2:", "3:"};
        JLabel[] idLabels    = {idPrimerDemog, idSegundoDemog, idTercerDemog};
        JLabel[] capLabels   = {numCapturasPrimerDemog, numCapturasSegundoDemog, numCapturasTercerDemog};
        int[]    rankY       = {90, 150, 210};

        for (int i = 0; i < 3; i++) {
            JLabel ord = new JLabel(ordinals[i]);
            ord.setFont(new Font("Yatra One", Font.BOLD, 24));
            ord.setForeground(new Color(204, 0, 0));
            ord.setHorizontalAlignment(SwingConstants.CENTER);
            ord.setBounds(20, rankY[i], 40, 40);
            pRanking.add(ord);

            idLabels[i].setBounds(60, rankY[i], 90, 40);
            pRanking.add(idLabels[i]);

            JPanel vpCap = valuePanel(capLabels[i], 130, 40);
            vpCap.setBounds(160, rankY[i], 130, 40);
            pRanking.add(vpCap);
        }

        content.add(pRanking);

        // ── Panel Eventos ──────────────────────────────────────────────────
        JPanel pEventos = darkPanel();
        pEventos.setBounds(870, 400, 310, 160);

        JPanel vpEvento = valuePanel(eventoActivo, 290, 50);
        vpEvento.setBounds(10, 20, 290, 50);
        pEventos.add(vpEvento);

        JLabel lTiempo = subLabel("Tiempo restante: ");
        lTiempo.setBounds(0, 90, 200, 50);
        pEventos.add(lTiempo);

        JPanel vpSegs = valuePanel(segsRestantesEvento, 90, 50);
        vpSegs.setBounds(200, 90, 90, 50);
        pEventos.add(vpSegs);

        content.add(pEventos);

        // ── Botón de control ───────────────────────────────────────────────
        BotonDeControl = new JButton("PAUSAR SIMULACIÓN");
        BotonDeControl.setFont(new Font("Yatra One", Font.BOLD, 36));
        BotonDeControl.setForeground(Color.WHITE);
        BotonDeControl.setBackground(new Color(155, 0, 50));
        BotonDeControl.setBorder(new javax.swing.border.LineBorder(Color.WHITE, 2, true));
        BotonDeControl.setCursor(new Cursor(Cursor.HAND_CURSOR));
        BotonDeControl.setBounds(350, 480, 500, 60);

        BotonDeControl.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent evt) {
                try {
                    BotonDeControl.setBackground(servidor != null && servidor.isPausado()
                        ? new Color(70, 170, 50) : new Color(200, 0, 50));
                } catch (Exception ignored) {}
            }
            @Override public void mouseExited(MouseEvent evt) {
                try {
                    BotonDeControl.setBackground(servidor != null && servidor.isPausado()
                        ? new Color(50, 150, 50) : new Color(155, 0, 50));
                } catch (Exception ignored) {}
            }
        });

        BotonDeControl.addActionListener(evt -> {
            if (servidor == null) return;
            try {
                servidor.pausarReanudar();
                boolean pausado = servidor.isPausado();
                BotonDeControl.setText(pausado ? "REANUDAR SIMULACIÓN" : "PAUSAR SIMULACIÓN");
                BotonDeControl.setBackground(pausado ? new Color(50, 150, 50) : new Color(155, 0, 30));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(ClienteRemoto.this,
                    "Error RMI:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        content.add(BotonDeControl);
    }

    // ── actualizarDatos ────────────────────────────────────────────────────

    private void actualizarDatos() {
        if (servidor == null) return;
        try {
            numNinosHawkins.setText(String.valueOf(servidor.getNinosEnHawkins()));

            numNinosPortalLaboratorio.setText(servidor.getNinosPortalLaboratorio() + " niños");
            numNinosPortalCentroComercial.setText(servidor.getNinosPortalCentroComercial() + " niños");
            numNinosPortalBosque.setText(servidor.getNinosPortalBosque() + " niños");
            numNinosPortalAlcantarillado.setText(servidor.getNinosPortalAlcantarillado() + " niños");

            numNinosLaboratorio.setText(servidor.getNinosLaboratorio() + " niños");
            numNinosCentroComercial.setText(servidor.getNinosCentroComercial() + " niños");
            numNinosBosque.setText(servidor.getNinosBosque() + " niños");
            numNinosAlcantarillado.setText(servidor.getNinosAlcantarillado() + " niños");
            numNinosCapturadosColmena.setText(servidor.getNinosColmena() + " niños");

            numDemogLaboratorio.setText(servidor.getDemogorgonesLaboratorio() + " demogorgons");
            numDemogCentroComercial.setText(servidor.getDemogorgonesCentroComercial() + " demogorgons");
            numDemogBosque.setText(servidor.getDemogorgonesBosque() + " demogorgons");
            numDemogAlcantarillado.setText(servidor.getDemogorgonesAlcantarillado() + " demogorgons");

            java.util.List<String> ranking = servidor.getRankingTop3();
            setRankEntry(0, ranking, idPrimerDemog, numCapturasPrimerDemog);
            setRankEntry(1, ranking, idSegundoDemog, numCapturasSegundoDemog);
            setRankEntry(2, ranking, idTercerDemog, numCapturasTercerDemog);

            eventoActivo.setText(servidor.getEventoActivo());
            segsRestantesEvento.setText(servidor.getSegundosRestantesEvento() + " seg");

            boolean pausado = servidor.isPausado();
            BotonDeControl.setText(pausado ? "REANUDAR SIMULACIÓN" : "PAUSAR SIMULACIÓN");
            BotonDeControl.setBackground(pausado ? new Color(50, 150, 50) : new Color(155, 0, 50));

        } catch (RemoteException e) {
            eventoActivo.setText("Error de conexión");
        }
    }

    private void setRankEntry(int i, java.util.List<String> ranking, JLabel idLabel, JLabel capLabel) {
        if (i < ranking.size()) {
            String[] parts = ranking.get(i).split(": ", 2);
            idLabel.setText(parts[0]);
            capLabel.setText(parts.length > 1 ? parts[1] : "-");
        }
    }

    // ── main ───────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClienteRemoto().setVisible(true));
    }
}
