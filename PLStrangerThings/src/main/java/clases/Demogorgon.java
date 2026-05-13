/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

// ============================================================
// CLASE: Demogorgon.java
// RESPONSABILIDAD: Hilo que representa a un demogorgon del Upside Down.
//   Patrulla zonas aleatoriamente, ataca niños y los traslada a la Colmena.
//   Reacciona a los 4 eventos globales del GestorEventos.
//
// IMPLEMENTA: Runnable → cada instancia se ejecuta en su propio Thread
//
// CICLO DE VIDA (run):
//   1. Comprobar pausa / evento ELEVEN (paralización)
//   2. Elegir zona: aleatoria normalmente, zona con más niños si RED_MENTAL activa
//   3. Entrar a la zona
//   4a. Si no hay niños → esperar 4-5s (bloqueado en zona si hay apagón)
//   4b. Si hay niños → obtenerYMarcarNino() (atómico), atacar, resolver resultado
//       - Éxito (prob. 1/3): capturar, trasladar a Colmena, incrementar capturas
//       - Fallo  (prob. 2/3): liberar al niño, salir de zona
//   5. Siempre al final: finalizarAtaque() → desbloquea el hilo Niño
//
// MECANISMOS DE CONCURRENCIA USADOS:
//   [1] AtomicInteger capturas  → incrementado desde un único hilo (este mismo),
//       pero leído concurrentemente desde SimulacionBackend (ranking RMI).
//       AtomicInteger garantiza lectura consistente sin cerrojo.
//   [2] volatile (en GestorEventos) → el Demogorgon lee flags del gestor
//       (isElevenActiva, isTormentaActiva, isRedMentalActiva) sin cerrojo.
//   [3] Zona.obtenerYMarcarNino() → operación atómica (cerrojo dentro de Zona)
//       que selecciona y reserva un niño; evita que dos Demogorgons ataquen al mismo.
//   [4] Zona.finalizarAtaque()   → libera la Condition del Niño atacado
//       (CRÍTICO: debe llamarse siempre, tanto en éxito como en fallo)
//
// INTERACCIÓN CON HILO NIÑO:
//   - Demogorgon llama interrupt() en el hilo del Niño para sacarlo de su sleep
//     de recolección y forzarle a entrar en esperarFinAtaque().
//   - Demogorgon resuelve el ataque y llama finalizarAtaque() → signalAll()
//     desbloquea al Niño, que comprueba si fue capturado o puede continuar.
//
// PREGUNTA TÍPICA DE DEFENSA:
//   "¿Por qué llamas interrupt() al hilo del niño?"
//   → Para sacarlo del Thread.sleep() de recolección inmediatamente. Sin interrupt(),
//     el niño acabaría su sleep, ignoraría el ataque y cruzaría el portal. Con
//     interrupt(), la excepción InterruptedException es capturada en Nino.run(),
//     que entonces llama a esperarFinAtaque() y queda bloqueado hasta que el
//     Demogorgon resuelva el resultado.
//
//   "¿Qué pasa si el Demogorgon captura al niño pero olvida llamar finalizarAtaque?"
//   → Deadlock. El niño estaría bloqueado para siempre en condicionFinAtaque.await()
//     porque nadie haría signalAll(). Por eso finalizarAtaque() se llama en AMBAS
//     ramas (éxito y fallo): es la garantía de que el niño siempre se desbloquea.
// ============================================================

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hilo que representa a un demogorgon del Upside Down. Patrulla zonas, ataca
 * niños y los traslada a la Colmena. Su comportamiento se ve alterado por los
 * eventos globales (tormenta, red mental, intervención de Eleven).
 */
public class Demogorgon implements Runnable {

    private String id;          // Identificador único: "D0000", "D0001", etc.
    private boolean atacando = false;
    private Hawkins hawkins;
    private UpsideDown upsideDown;

    // [AtomicInteger] Contador de capturas de ESTE demogorgon.
    // Incrementado solo por este hilo, pero leído por SimulacionBackend para
    // el ranking RMI → AtomicInteger garantiza visibilidad sin cerrojo.
    private final AtomicInteger capturas = new AtomicInteger(0);

    // Contador estático compartido por todos los Demogorgons para generar IDs únicos.
    // Solo se usa en el constructor (fase de inicialización, un solo hilo) → no necesita sincronización.
    private static int contadorIds = 1;

    private LogSimulacion log;
    private SimulacionBackend backend; // Para comprobarPausa() y acceder al GestorEventos

    /**
     * Crea un nuevo demogorgon con referencias al mundo y al backend de la simulación.
     *
     * @param id         identificador único del demogorgon (p. ej. "D0001")
     * @param hawkins    referencia al mundo de Hawkins
     * @param upsideDown referencia al Upside Down
     * @param log        registro de eventos de la simulación
     * @param backend    backend que gestiona la pausa y el gestor de eventos
     */
    public Demogorgon(String id, Hawkins hawkins, UpsideDown upsideDown, LogSimulacion log, SimulacionBackend backend) {
        this.id = id;
        this.hawkins = hawkins;
        this.upsideDown = upsideDown;
        this.log = log;
        this.backend = backend;
    }

    /**
     * Ciclo de vida del demogorgon: selecciona una zona (aleatoria o la de más niños
     * si hay red mental activa), la patrulla, ataca a un niño si lo hay y lo
     * traslada a la Colmena si el ataque tiene éxito. Se detiene si el hilo
     * es interrumpido.
     */
    @Override
    public void run() {
        try {
            while (true) {
                GestorEventos gestor = backend.getGestorEventos();

                // ── Comprobar pausa de la simulación ─────────────────────────
                // Si la simulación está pausada, este hilo se bloquea aquí hasta
                // que se reanude (igual que Nino y GestorEventos).
                backend.comprobarPausa();

                // ── EVENTO: INTERVENCION_ELEVEN → Demogorgon paralizado ───────
                // Eleven paraliza a todos los demogorgons durante el evento.
                // Usamos espera activa corta (200ms) para no consumir CPU en exceso
                // y salir rápidamente cuando el evento termine.
                if (gestor != null && gestor.isElevenActiva()) {
                    Thread.sleep(200); // Espera activa hasta que isElevenActiva() sea false
                    continue;          // Vuelve al inicio del bucle sin patrullar
                }

                // ── Elegir zona a patrullar ───────────────────────────────────
                Zona zonaActual;

                // EVENTO: RED_MENTAL → todos los Demogorgons van a la zona con más niños.
                // Si no hay evento, se mueve aleatoriamente entre las 4 zonas.
                if (gestor != null && gestor.isRedMentalActiva()) {
                    zonaActual = zonaConMasNinos(); // Dirección psíquica del Mindflayer
                } else {
                    // Movimiento aleatorio normal: 0=Laboratorio, 1=CentroComercial,
                    // 2=Bosque, 3=Alcantarillado
                    int zonaAleatoria = ThreadLocalRandom.current().nextInt(4);
                    if (zonaAleatoria == 0) {
                        zonaActual = upsideDown.getLaboratorio();
                    } else if (zonaAleatoria == 1) {
                        zonaActual = upsideDown.getCentroComercial();
                    } else if (zonaAleatoria == 2) {
                        zonaActual = upsideDown.getBosque();
                    } else {
                        zonaActual = upsideDown.getAlcantarillado();
                    }
                }

                zonaActual.entrarDemogorgon(this); // Registro de presencia en la zona
                log.registrarEvento("Demogorgon " + id + " ha entrado a " + zonaActual);

                // ── Rama A: zona vacía → esperar y moverse ────────────────────
                if (zonaActual.getListaNinos().isEmpty()) {
                    this.setAtacando(false);

                    // Tiempo base de espera: 4-5 segundos.
                    // Durante APAGÓN el demogorgon permanece en la zona actual:
                    // no puede moverse a otra zona, comprueba cada 500ms si sigue el apagón.
                    long tiempoEspera = ThreadLocalRandom.current().nextLong(4000, 5001);
                    long inicio = System.currentTimeMillis();
                    while (System.currentTimeMillis() - inicio < tiempoEspera) {
                        if (gestor != null && gestor.isApagonActivo()) {
                            Thread.sleep(500); // Bloqueado en zona durante apagón (sin moverse)
                        } else {
                            Thread.sleep(Math.min(500, tiempoEspera - (System.currentTimeMillis() - inicio)));
                        }
                    }
                    zonaActual.salirDemogorgon(this);

                } else {
                    // ── Rama B: hay niños → atacar ────────────────────────────
                    this.setAtacando(true);

                    // [ZONA.obtenerYMarcarNino()] Operación ATÓMICA: busca un niño libre
                    // y lo marca como "siendoAtacado=true" en un solo bloqueo.
                    // Garantiza que dos Demogorgons no seleccionan el mismo niño.
                    Nino ninoAtacado = zonaActual.obtenerYMarcarNino();

                    if (ninoAtacado != null) {
                        // INTERRUPT al hilo del Niño: lo saca de su Thread.sleep()
                        // de recolección. El Niño captura la InterruptedException y
                        // llama a esperarFinAtaque(), donde se bloquea hasta que
                        // nosotros llamemos finalizarAtaque().
                        ninoAtacado.getMiHilo().interrupt();

                        log.registrarEvento("Demogorgon " + id + " esta atacando a " + ninoAtacado.getId());

                        // Duración del ataque: 0.5-1.5 segundos.
                        // EVENTO TORMENTA: el tiempo se reduce a la mitad (demogorgons más ágiles)
                        long tiempoAtaque = ThreadLocalRandom.current().nextLong(500, 1501);
                        if (gestor != null && gestor.isTormentaActiva()) {
                            tiempoAtaque = tiempoAtaque / 2; // Más agresivo durante la tormenta
                        }
                        Thread.sleep(tiempoAtaque);

                        // Resolución del ataque: prob. 1/3 éxito, 2/3 fallo.
                        // nextInt(3)==0 → exactamente 1 de cada 3 casos es éxito.
                        boolean ataqueExitoso = (ThreadLocalRandom.current().nextInt(3) == 0);

                        if (ataqueExitoso) {
                            // ── Captura exitosa ───────────────────────────────
                            log.registrarEvento("Demogorgon " + id + " ha capturado a " + ninoAtacado.getId());

                            // Sacar al niño y al demogorgon de la zona insegura
                            zonaActual.salirNino(ninoAtacado);
                            zonaActual.salirDemogorgon(this);

                            // Marcar al niño como capturado ANTES de depositarlo en la Colmena.
                            // El Niño comprueba isCapturado() en su catch de InterruptedException
                            // para saber si debe ir a esperarRescate() o continuar su ciclo.
                            ninoAtacado.setCapturado(true);

                            // Tiempo de traslado a la Colmena: 0.5-1 segundo
                            Thread.sleep(ThreadLocalRandom.current().nextLong(500, 1001));

                            // Depositar al niño en la Colmena (incrementa totalDepositados)
                            upsideDown.getColmena().entrarNino(ninoAtacado);

                            // Presencia transitoria del Demogorgon en la Colmena (para la UI)
                            upsideDown.getColmena().entrarDemogorgon(this);
                            upsideDown.getColmena().salirDemogorgon(this);

                            this.incrementarCapturas(); // [ATOMIC] Suma 1 al contador
                            log.registrarEvento("El niño " + ninoAtacado.getId() +
                                " ha sido depositado en la Colmena (capturas " + id + ": " + capturas.get() + ")");

                            // CRÍTICO: finalizarAtaque() SIEMPRE debe llamarse, también en éxito.
                            // El niño está bloqueado en esperarFinAtaque() esperando este signalAll().
                            // Si no lo llamamos → DEADLOCK: el hilo del niño nunca se desbloquea.
                            zonaActual.finalizarAtaque(ninoAtacado);

                        } else {
                            // ── Ataque fallido: el niño resiste ──────────────
                            log.registrarEvento("Demogorgon " + id + " ha fallado el ataque a " + ninoAtacado.getId());
                            zonaActual.salirDemogorgon(this);

                            // CRÍTICO: también en fallo hay que finalizar el ataque.
                            // El niño sigue en esperarFinAtaque() y necesita ser despertado
                            // para continuar su ciclo (volver a Hawkins).
                            zonaActual.finalizarAtaque(ninoAtacado);
                        }
                    } else {
                        // Todos los niños de la zona ya están siendo atacados por otros Demogorgons.
                        // Salir y reintentar en el siguiente ciclo.
                        zonaActual.salirDemogorgon(this);
                        Thread.sleep(500);
                    }

                    this.setAtacando(false);
                }
            }

        } catch (InterruptedException e) {
            // El hilo fue interrumpido externamente (ej. shutdown de la simulación)
            System.out.println("Demogorgon: " + id + " ha sido interrumpido");
        }
    }

    /**
     * Encuentra y devuelve la zona del Upside Down con mayor número de niños.
     * Usada cuando el evento RED_MENTAL está activo (Mindflayer dirige a todos
     * los demogorgons psíquicamente hacia donde hay más presas).
     *
     * Nota: la lectura de getListaNinos().size() no está bajo cerrojo, pero
     * CopyOnWriteArrayList garantiza una vista consistente en un instante dado.
     */
    private Zona zonaConMasNinos() {
        Zona[] zonas = {
            upsideDown.getLaboratorio(),
            upsideDown.getCentroComercial(),
            upsideDown.getBosque(),
            upsideDown.getAlcantarillado()
        };
        Zona max = zonas[0];
        for (Zona z : zonas) {
            if (z.getListaNinos().size() > max.getListaNinos().size()) {
                max = z;
            }
        }
        return max;
    }

    /** Incrementa en uno el contador de capturas de este demogorgon. */
    public void incrementarCapturas() {
        capturas.incrementAndGet(); // [ATOMIC] Thread-safe sin cerrojo
    }

    /**
     * Devuelve el número de niños capturados por este demogorgon.
     * Leído por SimulacionBackend para el ranking RMI.
     */
    public int getCapturas() { return capturas.get(); }

    /** @return identificador único de este demogorgon (ej. "D0001") */
    public String getId() { return id; }

    /**
     * Genera un identificador único para un nuevo demogorgon con formato "D0001", "D0002", etc.
     * Solo se llama durante la inicialización (un solo hilo) → no necesita sincronización.
     */
    public static String generarId() { return String.format("D%04d", contadorIds++); }

    public void setAtacando(boolean atacando) {
        this.atacando = atacando;
    }
}