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
 *
 * @author User
 */
public class Colmena extends Zona{
    
    private Condition condicionRescate;
    private final AtomicInteger totalDepositados = new AtomicInteger(0);

    public Colmena() {
        super(); // Llama al constructor de Zona y hereda sus atributos comunes
        this.condicionRescate = this.getCerrojo().newCondition(); // Usamos el cerrojo heredado para crear la nueva condicion
    }
    
    @Override
    public void entrarNino(Nino nino) {
        super.entrarNino(nino);
        totalDepositados.incrementAndGet();
    }

    public int getTotalDepositados() { return totalDepositados.get(); }

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
    
    public void liberarNinos(int cantidad, Zona callePrincipal, LogSimulacion log) {
        List<Nino> liberados = new ArrayList<>();
        this.getCerrojo().lock();
        try {
            List lista = this.getListaNinos();
            for (int i = 0; i < lista.size() && liberados.size() < cantidad; i++) {
                Nino nino = (Nino) lista.get(i);
                nino.setCapturado(false);
                liberados.add(nino);
                callePrincipal.entrarNino(nino); // antes del signalAll para evitar la race con salirNino
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
