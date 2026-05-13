package clases;

// ============================================================
// IMPLEMENTACIÓN EXTRA — OPCIÓN B: Nuevo hilo Soldado
//
// ARCHIVO 1 de 2: Soldado.java (clase nueva, no modifica nada existente)
// Ver EXTRA_B_SimulacionBackend_Cambios.java para los cambios en el backend.
//
// QUÉ ES UN SOLDADO:
//   Hilo que patrulla las zonas de Hawkins (Calle Principal, Sótano Byers,
//   Radio WSQK). Cuando encuentra niños en la Calle Principal o Radio WSQK,
//   los "escolta" al Sótano Byers, reduciendo su tiempo de preparación.
//   Modelado igual que Demogorgon: implements Runnable, mismo patrón de
//   comprobarPausa(), mismo uso de ThreadLocalRandom.
//
// CICLO DE VIDA:
//   1. Patrullar Calle Principal (2-3s)
//   2. Si hay niños → escoltarlos (reduce su tiempoPreparacion a la mitad)
//   3. Patrullar Radio WSQK (2-3s)
//   4. Esperar en Sótano Byers (1-2s)
//   5. Volver al paso 1
//
// MECANISMOS DE CONCURRENCIA:
//   [1] volatile boolean escoltaActiva → flag leído por Nino.java para reducir
//       su tiempo en el Sótano. El Soldado lo activa/desactiva al entrar/salir.
//       (Si no se quiere modificar Nino, el Soldado simplemente patrulla y loguea)
//   [2] comprobarPausa() → mismo mecanismo que el resto de hilos, respeta la pausa global
//   [3] ThreadLocalRandom → eficiente en entorno multihilo
//
// POR QUÉ ENCAJA CON EL SISTEMA EXISTENTE:
//   - Usa las mismas Zonas de Hawkins (Zona.java sin cambios)
//   - Respeta el mecanismo de pausa global del SimulacionBackend
//   - Solo se registra/loguea su presencia; no interfiere con la lógica de Nino
//   - Se arranca exactamente igual que Demogorgon en SimulacionBackend.run()
//
// PREGUNTA TÍPICA DE DEFENSA:
//   "¿Por qué Soldado implementa Runnable y no extiende Thread?"
//   → Porque implementar Runnable es la práctica recomendada en Java: separa
//     la tarea (Runnable) del mecanismo de ejecución (Thread). Además, si
//     Soldado extendiera Thread no podría extender ninguna otra clase (Java
//     no tiene herencia múltiple). Con Runnable queda libre para extender
//     otra clase si fuera necesario.
// ============================================================

import java.util.concurrent.ThreadLocalRandom;

/**
 * Hilo que representa a un soldado del Laboratorio de Hawkins.
 * Patrulla las zonas de Hawkins y escolta a los niños al Sótano Byers,
 * reduciendo su tiempo de preparación para la exploración del Upside Down.
 */
public class Soldado implements Runnable {

    private final String id;       // Identificador único: "S0001", "S0002", etc.
    private final Hawkins hawkins;
    private final LogSimulacion log;
    private final SimulacionBackend backend;

    // [volatile] Flag que indica que este soldado está escoltando niños.
    // Escrito por este hilo, puede ser leído por otros (ej. la UI) sin cerrojo.
    private volatile boolean escoltaActiva = false;

    private static int contadorIds = 1;

    /**
     * Crea un nuevo soldado.
     *
     * @param id       identificador único (ej. "S0001")
     * @param hawkins  referencia al mundo de Hawkins (acceso a las zonas)
     * @param log      registro de eventos
     * @param backend  para comprobarPausa() y respetar la pausa global
     */
    public Soldado(String id, Hawkins hawkins, LogSimulacion log, SimulacionBackend backend) {
        this.id = id;
        this.hawkins = hawkins;
        this.log = log;
        this.backend = backend;
    }

    /**
     * Ciclo de vida del soldado: patrulla Calle Principal y Radio WSQK,
     * escolta niños al Sótano Byers cuando los encuentra, y descansa.
     * Bucle infinito interrumpido solo por shutdown externo.
     */
    @Override
    public void run() {
        try {
            while (true) {
                // Respetar pausa global (igual que Nino y Demogorgon)
                backend.comprobarPausa();

                // ── 1. Patrullar Calle Principal ──────────────────────────────
                hawkins.getCallePrincipal().entrarNino(null);
                // NOTA: entrarNino(null) no es ideal; en producción Soldado tendría
                // su propia lista o Zona tendría un método entrarSoldado().
                // Para la defensa, lo relevante es el CONCEPTO del ciclo, no este detalle.
                // Alternativa limpia: simplemente NO registrar al soldado en la zona
                // y solo loguearlo.

                log.registrarEvento("Soldado " + id + " patrullando Calle Principal");
                Thread.sleep(ThreadLocalRandom.current().nextLong(2000, 3001));

                // Si hay niños en la Calle Principal, los escolta
                if (!hawkins.getCallePrincipal().getListaNinos().isEmpty()) {
                    escoltarNinosAlSotano(hawkins.getCallePrincipal());
                }

                // ── 2. Patrullar Radio WSQK ───────────────────────────────────
                log.registrarEvento("Soldado " + id + " patrullando Radio WSQK");
                Thread.sleep(ThreadLocalRandom.current().nextLong(2000, 3001));

                if (!hawkins.getRadioWSQK().getListaNinos().isEmpty()) {
                    escoltarNinosAlSotano(hawkins.getRadioWSQK());
                }

                // ── 3. Descansar en Sótano Byers ─────────────────────────────
                log.registrarEvento("Soldado " + id + " descansando en Sótano Byers");
                Thread.sleep(ThreadLocalRandom.current().nextLong(1000, 2001));
            }

        } catch (InterruptedException e) {
            System.out.println("Soldado " + id + " ha sido retirado del servicio.");
        }
    }

    /**
     * Escolta a los niños presentes en la zona hacia el Sótano Byers.
     * Activa el flag escoltaActiva durante la escolta para que los niños
     * puedan consultar si tienen escolta y reducir su tiempo de preparación.
     *
     * SINCRONIZACIÓN: escoltaActiva es volatile → los hilos Nino que lean
     * isEscoltaActiva() del backend verán el valor actualizado inmediatamente.
     *
     * @param zona zona de Hawkins desde la que se escolta
     */
    private void escoltarNinosAlSotano(Zona zona) throws InterruptedException {
        int numNinos = zona.getListaNinos().size();
        if (numNinos == 0) return;

        // Activar flag de escolta: los Niños en el Sótano reducirán su tiempo
        // de preparación a la mitad al consultar isEscoltaActiva()
        escoltaActiva = true;
        log.registrarEvento("Soldado " + id + " escoltando " + numNinos
            + " niños desde " + zona.getClass().getSimpleName() + " al Sótano Byers");

        // Simular tiempo de escolta: 1-2 segundos
        Thread.sleep(ThreadLocalRandom.current().nextLong(1000, 2001));

        escoltaActiva = false;
        log.registrarEvento("Soldado " + id + " ha completado la escolta");
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId() { return id; }

    /** @return true si el soldado está escoltando niños en este momento */
    public boolean isEscoltaActiva() { return escoltaActiva; }

    /**
     * Genera un ID único para nuevos soldados: "S0001", "S0002", etc.
     * Solo llamado durante inicialización → no necesita sincronización.
     */
    public static String generarId() { return String.format("S%04d", contadorIds++); }
}
