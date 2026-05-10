/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;

/**
 * Zona especial del Upside Down donde los demogorgons depositan a los niños
 * capturados. Extiende {@link Zona} añadiendo la lógica de rescate: los niños
 * permanecen bloqueados hasta que Eleven los libera.
 */
public class Colmena extends Zona{
    
    private Condition condicionRescate;
    private final AtomicInteger totalDepositados = new AtomicInteger(0);

    public Colmena() {
        super(); // Llama al constructor de Zona y hereda sus atributos comunes
        this.condicionRescate = this.getCerrojo().newCondition(); // Usamos el cerrojo heredado para crear la nueva condicion
    }
    
    /**
     * Deposita un niño capturado en la Colmena e incrementa el contador histórico.
     *
     * @param nino niño que es depositado
     */
    @Override
    public void entrarNino(Nino nino) {
        super.entrarNino(nino);
        totalDepositados.incrementAndGet();
    }

    public int getTotalDepositados() { return totalDepositados.get(); }

    /**
     * Bloquea el hilo del niño hasta que sea liberado por {@link #liberarNinos}.
     *
     * @param nino niño capturado que espera ser rescatado
     */
    public void esperarRescate(Nino nino) {
        this.getCerrojo().lock();
        try {
            while (nino.isCapturado()) {
                condicionRescate.await();
            }
        } catch (InterruptedException e) {
            // Manejo de interrupción
        } finally {
            this.getCerrojo().unlock();
        }
    }
    
    /**
     * Libera hasta {@code cantidad} niños capturados, marcándolos como no capturados
     * y notificando a los hilos bloqueados en {@link #esperarRescate}.
     *
     * @param cantidad      número máximo de niños a liberar
     * @param callePrincipal zona a la que regresan los niños (usado para el log)
     * @param log           registro de eventos donde se anota cada liberación
     */
    public void liberarNinos(int cantidad, Zona callePrincipal, LogSimulacion log) {
        List<Nino> liberados = new ArrayList<>();
        this.getCerrojo().lock();
        try {
            List lista = this.getListaNinos();
            for (int i = 0; i < lista.size() && liberados.size() < cantidad; i++) {
                Nino nino = (Nino) lista.get(i);
                nino.setCapturado(false);
                liberados.add(nino);
            }
            condicionRescate.signalAll();
        } finally {
            this.getCerrojo().unlock();
        }
        for (Nino nino : liberados) {
            log.registrarEvento("ELEVEN ha liberado a " + nino.getId() + " de la Colmena");
        }
    }
}
