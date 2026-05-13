/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

// ============================================================
// CLASE: Colmena.java
// RESPONSABILIDAD: Zona especial del Upside Down donde los demogorgons depositan
//   niños capturados. Los niños quedan bloqueados aquí hasta que el evento
//   "Intervención de Eleven" los libera y regresan a la Calle Principal.
//
// HERENCIA: extiende Zona → reutiliza sus listas (CopyOnWriteArrayList),
//   su ReentrantLock y su Condition de ataque. Añade una SEGUNDA Condition
//   propia (condicionRescate) para la mecánica de liberación.
//
// MECANISMOS DE CONCURRENCIA USADOS:
//   [1] ReentrantLock (heredado de Zona) → mismo cerrojo para todo; coherencia
//       garantizada porque rescate y depósito comparten el mismo lock
//   [2] Condition condicionRescate       → los niños capturados esperan aquí;
//       Eleven los despierta con signalAll()
//   [3] AtomicInteger totalDepositados   → contador histórico actualizado desde
//       múltiples hilos Demogorgon sin necesitar cerrojo
//
// DECISIÓN DE DISEÑO CLAVE:
//   condicionRescate se crea a partir del MISMO cerrojo heredado de Zona
//   (getCerrojo().newCondition()). Esto es obligatorio: una Condition debe
//   pertenecer al cerrojo que se adquiere antes de llamar await()/signal().
//   Si usáramos un cerrojo distinto obtendríamos IllegalMonitorStateException.
//
// PREGUNTA TÍPICA DE DEFENSA:
//   "¿Por qué Colmena extiende Zona y no es una clase independiente?"
//   → Porque una Colmena ES una Zona del Upside Down con comportamiento extra.
//     Hereda la gestión thread-safe de listas y el cerrojo, evitando duplicar
//     código. Solo añade la lógica de rescate (condicionRescate + liberarNinos).
//
//   "¿Por qué dos Conditions distintas en la Colmena?"
//   → condicionFinAtaque (heredada) → sincroniza ataques de Demogorgon a Niño
//     condicionRescate (propia)     → sincroniza la espera de rescate de Eleven
//     Separarlas evita que un signalAll de rescate despierte accidentalmente
//     a niños que están esperando el fin de un ataque, y viceversa.
// ============================================================

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;

/**
 * Zona especial del Upside Down donde los demogorgons depositan a los niños
 * capturados. Extiende {@link Zona} añadiendo la lógica de rescate: los niños
 * permanecen bloqueados hasta que Eleven los libera.
 */
public class Colmena extends Zona {

    // [Condition] Variable de condición para el rescate de Eleven.
    // Los niños capturados hacen await() aquí en esperarRescate().
    // Eleven (GestorEventos) llama signalAll() en liberarNinos().
    // IMPORTANTE: creada a partir del cerrojo HEREDADO de Zona, no de uno nuevo.
    private Condition condicionRescate;

    // [AtomicInteger] Contador histórico de niños depositados en la Colmena
    // desde el inicio de la simulación. Se incrementa en entrarNino() desde
    // hilos Demogorgon concurrentes → AtomicInteger evita race conditions
    // sin necesidad de cerrojo extra.
    private final AtomicInteger totalDepositados = new AtomicInteger(0);

    public Colmena() {
        super(); // Hereda: listaNinos, listaDemogorgons, cerrojo, condicionFinAtaque

        // condicionRescate DEBE nacer del mismo cerrojo que protege la lista de niños.
        // getCerrojo() devuelve el ReentrantLock de Zona → coherencia garantizada.
        this.condicionRescate = this.getCerrojo().newCondition();
    }

    /**
     * Deposita un niño capturado en la Colmena e incrementa el contador histórico.
     * Sobreescribe entrarNino() de Zona para añadir el conteo.
     *
     * @param nino niño que es depositado
     */
    @Override
    public void entrarNino(Nino nino) {
        // Delega en Zona.entrarNino() → añade el niño a listaNinos con cerrojo
        super.entrarNino(nino);

        // [ATOMIC] Incremento sin cerrojo: AtomicInteger garantiza atomicidad
        // aunque varios Demogorgons depositen niños simultáneamente.
        totalDepositados.incrementAndGet();
    }

    /** @return total histórico de niños depositados en la Colmena (nunca decrece). */
    public int getTotalDepositados() {
        return totalDepositados.get();
    }

    /**
     * Bloquea el hilo del niño capturado hasta que Eleven lo libere.
     *
     * FLUJO:
     *   1. El niño adquiere el cerrojo heredado de Zona.
     *   2. Comprueba isCapturado() en bucle while (protección spurious wakeups).
     *   3. await() → libera el cerrojo y duerme el hilo del niño.
     *   4. Cuando Eleven llama liberarNinos() → signalAll() despierta a todos.
     *   5. El niño reevalúa isCapturado(); si es false, sale y reanuda su ciclo.
     *
     * @param nino niño capturado que espera ser rescatado
     */
    public void esperarRescate(Nino nino) {
        // [LOCK] Adquirimos el cerrojo heredado de Zona (el mismo que protege listaNinos)
        this.getCerrojo().lock();
        try {
            // Bucle while obligatorio: protección contra spurious wakeups y contra
            // que signalAll despierte a niños que aún tienen isCapturado=true
            // (solo los que fueron marcados false por liberarNinos() saldrán del bucle)
            while (nino.isCapturado()) {
                condicionRescate.await(); // [CONDITION.await()] Libera cerrojo y duerme
            }
        } catch (InterruptedException e) {
            // Interrupción externa (ej. shutdown de la simulación)
        } finally {
            this.getCerrojo().unlock();
        }
    }

    /**
     * Libera hasta {@code cantidad} niños capturados: los marca como no capturados
     * y notifica a todos los hilos bloqueados en {@link #esperarRescate}.
     *
     * Llamado por GestorEventos durante el evento "Intervención de Eleven".
     * La cantidad liberada = unidades de sangre de Vecna recolectadas en ese momento.
     *
     * ATOMICIDAD: el marcado (setCapturado(false)) y el signalAll() ocurren dentro
     * del mismo bloqueo → no puede haber un niño que sea marcado libre pero que
     * aún no haya sido notificado (y se quede dormido para siempre).
     *
     * @param cantidad       número máximo de niños a liberar
     * @param callePrincipal zona a la que regresan los niños (usado para el log)
     * @param log            registro de eventos donde se anota cada liberación
     */
    public void liberarNinos(int cantidad, Zona callePrincipal, LogSimulacion log) {
        List<Nino> liberados = new ArrayList<>();

        // [LOCK] Sección crítica: marcar niños + signalAll deben ser atómicos
        this.getCerrojo().lock();
        try {
            List lista = this.getListaNinos();
            for (int i = 0; i < lista.size() && liberados.size() < cantidad; i++) {
                Nino nino = (Nino) lista.get(i);
                nino.setCapturado(false); // Marca al niño como libre → saldrá del while en esperarRescate()
                liberados.add(nino);
            }
            // [CONDITION.signalAll()] Despierta a TODOS los niños en esperarRescate().
            // Solo los marcados con isCapturado=false saldrán del bucle while;
            // el resto (si los hubiera) volvería a dormirse inmediatamente.
            condicionRescate.signalAll();
        } finally {
            this.getCerrojo().unlock();
        }

        // El log se escribe FUERA del cerrojo para no mantenerlo bloqueado
        // innecesariamente mientras se registran eventos (escritura a fichero)
        for (Nino nino : liberados) {
            log.registrarEvento("ELEVEN ha liberado a " + nino.getId() + " de la Colmena");
        }
    }
}
