/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

// ============================================================
// CLASE: Zona.java
// RESPONSABILIDAD: Representa una zona del Upside Down (Bosque, Laboratorio,
//   Centro Comercial, Alcantarillado). Gestiona de forma segura la presencia
//   simultánea de niños y demogorgons, y sincroniza la mecánica de ataque.
//
// MECANISMOS DE CONCURRENCIA USADOS:
//   [1] ReentrantLock  → exclusión mutua en entradas/salidas y marcado de ataques
//   [2] Condition      → el hilo Niño espera bloqueado mientras es atacado;
//                        el hilo Demogorgon lo despierta al terminar el ataque
//   [3] CopyOnWriteArrayList → lectura concurrente sin bloqueo (la UI lee estas
//                        listas constantemente sin interferir con los hilos)
//
// PREGUNTA TÍPICA DE DEFENSA:
//   "¿Por qué usas ReentrantLock en vez de synchronized?"
//   → Porque necesitamos una Condition asociada al cerrojo para bloquear/despertar
//     al niño atacado de forma precisa. Con synchronized solo tenemos wait/notifyAll
//     globales; con Condition controlamos exactamente QUÉ hilos se despiertan y CUÁNDO.
// ============================================================

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Zona concurrente del mundo del juego que puede albergar niños y demogorgons
 * de forma simultánea. Utiliza un {@link java.util.concurrent.locks.ReentrantLock}
 * para garantizar exclusión mutua en las operaciones de entrada/salida y un
 * {@link java.util.concurrent.locks.Condition} para sincronizar los ataques.
 */
public class Zona {

    // [CopyOnWriteArrayList] Lista de niños presentes en la zona.
    // Se elige CopyOnWriteArrayList porque la UI (PanelInfo) lee esta lista
    // frecuentemente desde otro hilo sin necesitar bloqueo. Las escrituras
    // (entrar/salir) son poco frecuentes y ya están protegidas por el cerrojo.
    private List listaNinos;

    // [CopyOnWriteArrayList] Lista de demogorgons presentes en la zona.
    // Misma razón que listaNinos: lecturas frecuentes desde la UI sin coste de lock.
    private List listaDemogorgons;

    // [ReentrantLock] Cerrojo principal de la zona. Protege:
    //   - Modificaciones a listaNinos y listaDemogorgons
    //   - La operación atómica obtenerYMarcarNino() (buscar + marcar en un solo paso)
    //   - El ciclo await/signalAll del ataque
    // Es "Reentrant" (reentrante) por si el mismo hilo necesita adquirirlo dos veces
    // en una pila de llamadas (aunque en este diseño no ocurre, es buena práctica).
    private Lock cerrojo;

    // [Condition] Variable de condición asociada al cerrojo de ESTA zona.
    // Permite que el hilo Niño se bloquee dentro de esperarFinAtaque() liberando
    // el cerrojo temporalmente (await), y que el Demogorgon lo despierte en
    // finalizarAtaque() (signalAll). Cada zona tiene su propia condición → los
    // niños de distintas zonas no se despiertan entre sí innecesariamente.
    private Condition condicionFinAtaque;

    public Zona() {
        // Cada zona crea sus propias listas concurrentes independientes
        this.listaNinos = new CopyOnWriteArrayList<>();
        this.listaDemogorgons = new CopyOnWriteArrayList<>();

        // Cada zona tiene su propio cerrojo → zonas distintas no compiten entre sí
        // (máxima concurrencia: demogorgons en zonas diferentes actúan en paralelo)
        this.cerrojo = new ReentrantLock();

        // La Condition DEBE nacer del mismo cerrojo que la protege.
        // Internamente, await() libera 'cerrojo' y bloquea el hilo de forma atómica.
        this.condicionFinAtaque = this.cerrojo.newCondition();
    }


    /**
     * Registra la entrada de un niño en esta zona.
     * Protegido con cerrojo para que dos niños no se añadan simultáneamente
     * y corrompan el estado de la lista.
     *
     * @param nino niño que entra
     */
    public void entrarNino(Nino nino) {
        // [LOCK] Sección crítica: modificación de listaNinos
        cerrojo.lock();
        try {
            listaNinos.add(nino);
        } finally {
            // finally garantiza que el cerrojo SIEMPRE se libera, incluso si
            // add() lanzara una excepción inesperada (evita deadlocks)
            cerrojo.unlock();
        }
    }

    /**
     * Registra la entrada de un demogorgon en esta zona.
     *
     * @param demog demogorgon que entra
     */
    public void entrarDemogorgon(Demogorgon demog) {
        // [LOCK] Sección crítica: modificación de listaDemogorgons
        cerrojo.lock();
        try {
            listaDemogorgons.add(demog);
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Elimina un niño de esta zona.
     *
     * @param nino niño que sale
     */
    public void salirNino(Nino nino) {
        // [LOCK] Sección crítica: modificación de listaNinos
        cerrojo.lock();
        try {
            listaNinos.remove(nino);
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Elimina un demogorgon de esta zona.
     *
     * @param demog demogorgon que sale
     */
    public void salirDemogorgon(Demogorgon demog) {
        // [LOCK] Sección crítica: modificación de listaDemogorgons
        cerrojo.lock();
        try {
            listaDemogorgons.remove(demog);
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Bloquea el hilo del niño hasta que el demogorgon que lo está atacando
     * señalice el fin del ataque mediante {@link #finalizarAtaque}.
     *
     * FLUJO INTERNO:
     *   1. El niño adquiere el cerrojo.
     *   2. Comprueba su flag siendoAtacado (bucle while → guarda frente a
     *      "spurious wakeups", despertares espurios que Java puede generar).
     *   3. await() → libera el cerrojo ATÓMICAMENTE y duerme el hilo.
     *      Mientras duerme, el Demogorgon puede entrar a finalizarAtaque().
     *   4. Al recibir signalAll(), el niño recompite por el cerrojo, reevalúa
     *      la condición y, si siendoAtacado==false, sale del bucle y continúa.
     *
     * @param nino niño que espera a que su ataque concluya
     */
    public void esperarFinAtaque(Nino nino) {
        // [LOCK + CONDITION.await()] El niño se bloquea aquí durante el ataque
        cerrojo.lock();
        try {
            // Bucle while obligatorio: protección contra spurious wakeups.
            // Si el hilo se despertara sin que el Demogorgon haya llamado a
            // finalizarAtaque(), volvería a dormirse inmediatamente.
            while (nino.isSiendoAtacado() == true) {
                condicionFinAtaque.await(); // Libera cerrojo y duerme el hilo
            }
        } catch (InterruptedException e) {
            System.out.println("Niño interrumpido");
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Busca y devuelve el primer niño de la zona que no esté siendo atacado,
     * marcándolo como {@code siendoAtacado = true} de forma atómica.
     *
     * ATOMICIDAD CRÍTICA: buscar un niño libre Y marcarlo ocurre dentro del
     * mismo bloqueo. Si no fuera atómico, dos demogorgons podrían seleccionar
     * al mismo niño simultáneamente (race condition).
     *
     * @return un niño disponible para atacar, o {@code null} si no hay ninguno libre
     */
    public Nino obtenerYMarcarNino() {
        // [LOCK] Operación atómica: buscar + marcar en un solo bloqueo.
        // Sin este cerrojo, dos Demogorgons podrían seleccionar el mismo niño.
        cerrojo.lock();
        try {
            for (int i = 0; i < listaNinos.size(); i++) {
                Nino nino = (Nino) listaNinos.get(i);
                if (!nino.isSiendoAtacado()) {
                    nino.setSiendoAtacado(true); // Reserva atómica del niño
                    return nino;
                }
            }
            return null; // No hay niños libres en esta zona
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Marca al niño como no atacado y notifica a todos los hilos que esperan
     * en {@link #esperarFinAtaque}.
     *
     * IMPORTANTE: signalAll() en lugar de signal() porque podría haber varios
     * niños en la zona esperando. Solo el que tenga siendoAtacado==false
     * continuará; el resto volverá a dormirse en el bucle while.
     *
     * @param nino niño cuyo ataque ha concluido
     */
    public void finalizarAtaque(Nino nino) {
        // [LOCK + CONDITION.signalAll()] Despierta al niño atacado
        cerrojo.lock();
        try {
            nino.setSiendoAtacado(false);
            // signalAll() despierta a TODOS los que esperan en condicionFinAtaque.
            // Solo el niño correcto (siendoAtacado==false) saldrá del bucle while;
            // los demás volverán a hacer await(). Se usa signalAll y no signal()
            // para no arriesgarse a despertar al niño equivocado.
            condicionFinAtaque.signalAll();
        } finally {
            cerrojo.unlock();
        }
    }


    // ── Getters ──────────────────────────────────────────────────────────────
    // Usados principalmente por SimulacionBackend (capa RMI) y PanelInfo (UI)
    // para lectura de estado en tiempo real.

    public List getListaNinos() {
        return listaNinos;
    }

    public List getListaDemogorgons() {
        return listaDemogorgons;
    }

    public Lock getCerrojo() {
        return cerrojo;
    }

    public Condition getCondicionFinAtaque() {
        return condicionFinAtaque;
    }
}
