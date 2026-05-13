/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

// ============================================================
// CLASE: Nino.java
// RESPONSABILIDAD: Hilo que representa a un niño de Hawkins. Recorre
//   cíclicamente el mundo: Sótano → Portal → Upside Down → Hawkins.
//   Puede ser atacado por Demogorgons durante la recolección y, si es
//   capturado, queda bloqueado en la Colmena hasta que Eleven lo libere.
//
// IMPLEMENTA: Runnable → cada instancia corre en su propio Thread
//
// CICLO COMPLETO (run):
//   1. Nace en Calle Principal (0.5-2s)
//   2. [LOOP] Sótano Byers (1-2s preparación)
//   3.        Elige portal aleatorio → cruzarHaciaUpsideDown (grupo + 1 a 1)
//   4.        Entra a zona insegura → intentarRecolectarSangre()
//   5a. Si fue CAPTURADO → esperarRescate() en Colmena → Calle Principal → continue
//   5b. Si NO capturado → salir zona → cruzarHaciaHawkins (prioridad vuelta)
//   6.        Radio WSQK (2-4s descanso + depósito de sangre)
//   7.        Calle Principal (3-5s deambulando)
//   8.        Volver al paso 2
//
// MECANISMOS DE CONCURRENCIA USADOS:
//   [1] volatile boolean siendoAtacado → escrito por Demogorgon (hilo externo),
//       leído por Zona.obtenerYMarcarNino() y Zona.esperarFinAtaque(). volatile
//       garantiza visibilidad cruzada entre hilos.
//   [2] Thread.interrupt() recibido → el Demogorgon interrumpe el sleep de
//       recolección; la InterruptedException es capturada en intentarRecolectarSangre()
//   [3] Zona.esperarFinAtaque() → el niño se bloquea en Condition.await() hasta
//       que el Demogorgon llame finalizarAtaque() → signalAll()
//   [4] Colmena.esperarRescate() → se bloquea en condicionRescate.await() hasta
//       que Eleven llame liberarNinos() → signalAll()
//
// LA FUNCIÓN CLAVE: intentarRecolectarSangre()
//   Toda la complejidad del ataque está encapsulada aquí. Maneja:
//   - Primer ataque (InterruptedException en el sleep principal)
//   - Tiempo restante de recolección tras sobrevivir el ataque
//   - Segundo ataque durante el tiempo restante
//   - Evento TORMENTA: duplica el tiempo de recolección
//
// PREGUNTA TÍPICA DE DEFENSA:
//   "¿Cómo sabe el niño si fue capturado o solo atacado?"
//   → Por el flag capturado (booleano). El Demogorgon llama setCapturado(true)
//     ANTES de llamar finalizarAtaque(). Cuando el niño se despierta en
//     esperarFinAtaque(), comprueba isCapturado() para decidir qué hacer:
//     true → no recolecta, run() lo manda a la Colmena
//     false → continúa recolectando el tiempo restante
//
//   "¿Por qué siendoAtacado es volatile y capturado no?"
//   → siendoAtacado es ESCRITO por el hilo Demogorgon y LEÍDO por el hilo Nino
//     dentro de Zona (métodos sincronizados con cerrojo). El cerrojo de Zona
//     garantiza visibilidad para capturado porque Demogorgon lo escribe y el
//     Niño lo lee siempre dentro del mismo bloqueo (o justo después del await).
//     siendoAtacado se lee también fuera del cerrojo (en obtenerYMarcarNino),
//     por eso necesita volatile adicionalmente.
// ============================================================

import java.util.concurrent.ThreadLocalRandom;

/**
 * Hilo que representa a un niño de Hawkins. Cada instancia recorre cíclicamente
 * el mundo: deambula por Hawkins, cruza un portal aleatorio al Upside Down,
 * recolecta sangre y regresa, o es capturado y espera rescate en la Colmena.
 */
public class Nino implements Runnable {

    private String id; // Identificador único: "N0001", "N0002", ... "N1500"

    // [volatile] Escrito por el hilo Demogorgon (Zona.obtenerYMarcarNino → setSiendoAtacado(true))
    // y leído por el hilo Nino (en Zona.esperarFinAtaque → while(isSiendoAtacado())).
    // volatile garantiza que la escritura del Demogorgon sea visible inmediatamente
    // para el Nino sin necesidad de cerrojo adicional en la lectura.
    private volatile boolean siendoAtacado = false;

    private Hawkins hawkins;
    private UpsideDown upsideDown;

    // Sangre recolectada en la excursión actual (se resetea a 0 al depositar o ser capturado).
    // Solo accedida por el propio hilo Niño → no necesita sincronización.
    private int sangreRecolectada = 0;

    // Flag de captura. Escrito por Demogorgon dentro del cerrojo de Zona
    // (antes de llamar finalizarAtaque), leído por Niño justo después de
    // salir del await (dentro o inmediatamente después del cerrojo).
    // La visibilidad está garantizada por el protocolo lock/unlock de Zona.
    private boolean capturado = false;

    // Referencia al Thread que ejecuta este Runnable. Se asigna en Main/Hawkins
    // antes de arrancar el hilo. El Demogorgon lo usa para llamar interrupt().
    private Thread miHilo;

    // Contador estático para generar IDs únicos. Solo usado en inicialización → no necesita sync.
    private static int contadorIds = 1;

    private LogSimulacion log;
    private SimulacionBackend backend; // Para comprobarPausa() y acceder al GestorEventos

    /**
     * Crea un nuevo niño con referencias al mundo y al backend de la simulación.
     *
     * @param id         identificador único del niño (p. ej. "N0001")
     * @param hawkins    referencia al mundo de Hawkins
     * @param upsideDown referencia al Upside Down
     * @param log        registro de eventos de la simulación
     * @param backend    backend que gestiona la pausa y el gestor de eventos
     */
    public Nino(String id, Hawkins hawkins, UpsideDown upsideDown, LogSimulacion log, SimulacionBackend backend) {
        this.id = id;
        this.hawkins = hawkins;
        this.upsideDown = upsideDown;
        this.log = log;
        this.backend = backend;
    }

    /**
     * Ciclo de vida del niño: nace en la Calle Principal, entra en el Sótano Byers,
     * cruza un portal aleatorio al Upside Down, recolecta sangre y regresa.
     * Si es capturado, espera rescate en la Colmena antes de reincorporarse.
     * El bucle se interrumpe únicamente cuando el hilo es terminado externamente.
     */
    @Override
    public void run() {
        try {
            // ── Nacimiento: Calle Principal (solo al inicio, no en cada ciclo) ──
            hawkins.getCallePrincipal().entrarNino(this);
            Thread.sleep(ThreadLocalRandom.current().nextLong(500, 2001));
            hawkins.getCallePrincipal().salirNino(this);

            while (true) {
                // Comprueba pausa antes de cada ciclo (igual que Demogorgon y GestorEventos)
                backend.comprobarPausa();

                // ── 1. Sótano Byers: preparación (1-2s) ──────────────────────
                hawkins.getSotanoByers().entrarNino(this);
                Thread.sleep(ThreadLocalRandom.current().nextLong(1000, 2001));
                hawkins.getSotanoByers().salirNino(this);

                // ── 2. Elegir portal aleatorio ────────────────────────────────
                // 0=Bosque(cap.2), 1=Laboratorio(cap.3), 2=CentroComercial(cap.4), 3=Alcantarillado(cap.2)
                int portalAleatorio = ThreadLocalRandom.current().nextInt(4);
                Portal portal;
                Zona zonaInsegura;
                if (portalAleatorio == 0) {
                    portal = hawkins.getPortalBosque();
                    zonaInsegura = upsideDown.getBosque();
                } else if (portalAleatorio == 1) {
                    portal = hawkins.getPortalLaboratorio();
                    zonaInsegura = upsideDown.getLaboratorio();
                } else if (portalAleatorio == 2) {
                    portal = hawkins.getPortalCentroComercial();
                    zonaInsegura = upsideDown.getCentroComercial();
                } else {
                    portal = hawkins.getPortalAlcantarillado();
                    zonaInsegura = upsideDown.getAlcantarillado();
                }

                // ── 3. Cruzar hacia el Upside Down (grupo + 1 a 1) ───────────
                // Puede lanzar InterruptedException si un apagón rompe la barrera.
                // En ese caso, el niño vuelve al inicio del while (reintentar).
                try {
                    portal.cruzarHaciaUpsideDown(this);
                } catch (InterruptedException e) {
                    continue; // Apagón rompió la CyclicBarrier; reintentar en la siguiente iteración
                }

                // ── 4. Recolección en zona insegura ───────────────────────────
                zonaInsegura.entrarNino(this);

                // intentarRecolectarSangre encapsula toda la lógica de ataque:
                // duerme el tiempo de recolección, maneja interrupciones del Demogorgon,
                // espera el fin del ataque y decide si recolecta o no.
                intentarRecolectarSangre(zonaInsegura);

                // ── 5. Consecuencias post-recolección ─────────────────────────

                if (this.isCapturado()) {
                    // RAMA CAPTURA: el Demogorgon ya movió al niño a la Colmena.
                    // El niño se bloquea en esperarRescate() hasta que Eleven lo libere.
                    upsideDown.getColmena().esperarRescate(this); // Condition.await()
                    upsideDown.getColmena().salirNino(this);       // Salir de la Colmena
                    this.setCapturado(false);                      // Resetear flag
                    this.sangreRecolectada = 0;                    // No deposita sangre si fue capturado
                    log.registrarEvento("Niño " + id + " ha sido rescatado de la Colmena");

                    // Regresa directamente a Calle Principal (no pasa por Radio WSQK)
                    hawkins.getCallePrincipal().entrarNino(this);
                    Thread.sleep(ThreadLocalRandom.current().nextLong(3000, 5001));
                    hawkins.getCallePrincipal().salirNino(this);
                    continue; // Vuelve al inicio del ciclo (Sótano Byers)
                }

                // RAMA LIBRE: el niño sobrevivió, regresa a Hawkins con su sangre
                zonaInsegura.salirNino(this);

                // ── 6. Cruzar de vuelta (PRIORIDAD sobre los de ida) ─────────
                portal.cruzarHaciaHawkins(this);

                // ── 7. Radio WSQK: descanso y depósito de sangre (2-4s) ───────
                hawkins.getRadioWSQK().entrarNino(this);
                if (this.sangreRecolectada > 0) {
                    log.registrarEvento("Niño " + id + " deposita " + sangreRecolectada
                            + " unidades de sangre en Radio WSQK");
                }
                this.sangreRecolectada = 0; // Sangre depositada, contador a cero
                Thread.sleep(ThreadLocalRandom.current().nextLong(2000, 4001));
                hawkins.getRadioWSQK().salirNino(this);

                // ── 8. Calle Principal: deambular (3-5s) ─────────────────────
                hawkins.getCallePrincipal().entrarNino(this);
                Thread.sleep(ThreadLocalRandom.current().nextLong(3000, 5001));
                hawkins.getCallePrincipal().salirNino(this);
                // → Siguiente iteración: vuelve al Sótano Byers
            }

        } catch (InterruptedException e) {
            // Interrupción externa de nivel superior (shutdown de la simulación)
            System.out.println("El niño " + this.id + " ha sido eliminado del sistema.");
        }
    }

    /**
     * Intenta recolectar 1 unidad de sangre en la zona insegura.
     * Encapsula toda la lógica de ataque: maneja la interrupción del Demogorgon,
     * espera el resultado del ataque y completa la recolección si el niño sobrevive.
     *
     * FLUJO NORMAL (sin ataque):
     *   sleep(tiempoNecesario) completa → sangreRecolectada++ → retorna
     *
     * FLUJO CON ATAQUE (InterruptedException):
     *   1. Demogorgon llama interrupt() → sleep lanza InterruptedException
     *   2. Niño llama esperarFinAtaque() → se bloquea en Condition.await()
     *   3. Demogorgon resuelve ataque → finalizarAtaque() → signalAll()
     *   4. Niño se despierta y comprueba isCapturado():
     *      - true  → return (no recolecta; run() gestionará la Colmena)
     *      - false → duerme el tiempo RESTANTE y luego recolecta
     *
     * EVENTO TORMENTA: duplica tiempoNecesario al inicio del método.
     *
     * @param zonaInsegura zona donde está el niño (para llamar esperarFinAtaque)
     */
    private void intentarRecolectarSangre(Zona zonaInsegura) {
        // Tiempo base de recolección: 3-5 segundos.
        // EVENTO TORMENTA: se duplica (6-10 segundos).
        long tiempoNecesario = ThreadLocalRandom.current().nextLong(3000, 5001);
        if (backend.getGestorEventos() != null && backend.getGestorEventos().isTormentaActiva()) {
            tiempoNecesario = tiempoNecesario * 2; // El evento Tormenta duplica el tiempo
        }
        long tiempoInicio = System.currentTimeMillis();

        try {
            Thread.sleep(tiempoNecesario); // FLUJO NORMAL: duerme sin ser interrumpido

            // Recolección completada sin incidencias
            this.sangreRecolectada++;
            if (backend.getGestorEventos() != null) {
                backend.getGestorEventos().agregarSangre(1); // Suma al contador global (AtomicInteger)
            }

        } catch (InterruptedException e) {
            // ── PRIMER ATAQUE: Demogorgon nos interrumpió ────────────────────
            // El Demogorgon ya llamó setSiendoAtacado(true) y ahora esperamos
            // bloqueados en la Condition hasta que llame finalizarAtaque().
            zonaInsegura.esperarFinAtaque(this); // [CONDITION.await()] Bloqueado aquí

            // Al despertar: el Demogorgon ya llamó setSiendoAtacado(false).
            // Comprobamos si además nos capturó.
            if (!this.isCapturado()) {
                // Niño LIBRE: completar el tiempo de recolección restante
                long tiempoConsumido = System.currentTimeMillis() - tiempoInicio;
                long tiempoRestante = tiempoNecesario - tiempoConsumido;

                if (tiempoRestante > 0) {
                    try {
                        Thread.sleep(tiempoRestante); // Completa la recolección

                    } catch (InterruptedException e2) {
                        // ── SEGUNDO ATAQUE durante el tiempo restante ─────────
                        // Otro Demogorgon nos ataca mientras completábamos la recolección
                        zonaInsegura.esperarFinAtaque(this); // Esperamos resultado de este segundo ataque

                        if (this.isCapturado()) {
                            return; // Capturado en el segundo ataque; run() gestionará la Colmena
                        }
                        // Si sobrevivimos el segundo ataque, no recolectamos
                        // (simplificación: demasiados ataques consecutivos = sangre perdida)
                    }
                }

                // Recolección completada tras sobrevivir el ataque
                this.sangreRecolectada++;
                if (backend.getGestorEventos() != null) {
                    backend.getGestorEventos().agregarSangre(1);
                }
            }
            // Si isCapturado()==true: no recolecta; el run() detectará capturado=true
            // y mandará al niño a esperarRescate() en la Colmena.
        }
    }

    // ── Getters y setters ─────────────────────────────────────────────────────

    /** @return true si un Demogorgon está atacando a este niño en este momento */
    public boolean isSiendoAtacado() { return siendoAtacado; }

    /** @return identificador único de este niño (ej. "N0001") */
    public String getId() { return id; }

    /** @return true si el niño está actualmente capturado en la Colmena */
    public boolean isCapturado() { return capturado; }

    // [volatile write] Llamado por Demogorgon desde su propio hilo
    public void setSiendoAtacado(boolean siendoAtacado) { this.siendoAtacado = siendoAtacado; }

    // Llamado por Demogorgon (setCapturado(true)) y por run() tras el rescate (setCapturado(false))
    public void setCapturado(boolean capturado) { this.capturado = capturado; }

    /**
     * Devuelve el Thread que ejecuta este Runnable.
     * Usado por el Demogorgon para llamar miHilo.interrupt() al inicio del ataque.
     */
    public Thread getMiHilo() { return miHilo; }

    /** Asignado desde Main/Hawkins justo antes de llamar thread.start(). */
    public void setMiHilo(Thread miHilo) { this.miHilo = miHilo; }

    /**
     * Genera un identificador único para un nuevo niño: "N0001", "N0002", ..., "N1500".
     * Solo llamado durante la inicialización (un solo hilo) → no necesita sincronización.
     */
    public static String generarId() { return String.format("N%04d", contadorIds++); }
}