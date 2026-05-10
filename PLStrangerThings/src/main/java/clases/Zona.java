/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

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
    
    private List listaNinos; //CopyOnWriteArrayList
    private List listaDemogorgons; //CopyOnWriteArrayList
    private Lock cerrojo; //Reentrar Lock
    private Condition condicionFinAtaque;

    public Zona() {
        // Cada zona crea sus propias listas concurrentes
        this.listaNinos = new CopyOnWriteArrayList<>();
        this.listaDemogorgons = new CopyOnWriteArrayList<>();
        
        // Cada zona crea su propio cerrojo independiente
        this.cerrojo = new ReentrantLock();
        
        // La condición nace estrictamente del cerrojo de ESTA zona
        this.condicionFinAtaque = this.cerrojo.newCondition();
    }
    
    
    /**
     * Registra la entrada de un niño en esta zona.
     *
     * @param nino niño que entra
     */
    public void entrarNino(Nino nino){
        cerrojo.lock();
        try{
            listaNinos.add(nino);
        }finally{
            cerrojo.unlock();
        }
        
    }
    
    /**
     * Registra la entrada de un demogorgon en esta zona.
     *
     * @param demog demogorgon que entra
     */
    public void entrarDemogorgon(Demogorgon demog){
        cerrojo.lock();
        try{
            listaDemogorgons.add(demog);
        }finally{
            cerrojo.unlock();
        }
    }
    
    /**
     * Elimina un niño de esta zona.
     *
     * @param nino niño que sale
     */
    public void salirNino(Nino nino){
        cerrojo.lock();
        try{
            listaNinos.remove(nino);
        }finally{
            cerrojo.unlock();
        }
        
    }
    
    /**
     * Elimina un demogorgon de esta zona.
     *
     * @param demog demogorgon que sale
     */
    public void salirDemogorgon(Demogorgon demog){
        cerrojo.lock();
        try{
            listaDemogorgons.remove(demog);
        }finally{
            cerrojo.unlock();
        }
    }
    
    /**
     * Bloquea el hilo del niño hasta que el demogorgon que lo está atacando
     * señalice el fin del ataque mediante {@link #finalizarAtaque}.
     *
     * @param nino niño que espera a que su ataque concluya
     */
    public void esperarFinAtaque(Nino nino){
        cerrojo.lock();
        try{
            while(nino.isSiendoAtacado() == true){
                condicionFinAtaque.await();
            }
        }
        catch(InterruptedException e){
            System.out.println("Niño interrumpido"); //Manejacion de error temporal
        }
        finally{
            cerrojo.unlock();
        }
    }
    
    /**
     * Busca y devuelve el primer niño de la zona que no esté siendo atacado,
     * marcándolo como {@code siendoAtacado = true} de forma atómica.
     *
     * @return un niño disponible para atacar, o {@code null} si no hay ninguno libre
     */
    public Nino obtenerYMarcarNino() {
        cerrojo.lock();
        try {
            for (int i = 0; i < listaNinos.size(); i++) {
                Nino nino = (Nino) listaNinos.get(i);
                if (!nino.isSiendoAtacado()) {
                    nino.setSiendoAtacado(true);
                    return nino;
                }
            }
            return null;
        } finally {
            cerrojo.unlock();
        }
    }
    
    /**
     * Marca al niño como no atacado y notifica a todos los hilos que esperan
     * en {@link #esperarFinAtaque}.
     *
     * @param nino niño cuyo ataque ha concluido
     */
    public void finalizarAtaque(Nino nino) {
        cerrojo.lock();
        try {
            nino.setSiendoAtacado(false);
            condicionFinAtaque.signalAll(); // El niño es despertado
        } finally {
            cerrojo.unlock();
        }
    }
    
    
    
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
