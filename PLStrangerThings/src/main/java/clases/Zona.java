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
 *
 * @author User
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
    
    
    public void entrarNino(Nino nino){
        cerrojo.lock();
        try{
            listaNinos.add(nino);
        }finally{
            cerrojo.unlock();
        }
        
    }
    
    public void entrarDemogorgon(Demogorgon demog){
        cerrojo.lock();
        try{
            listaDemogorgons.add(demog);
        }finally{
            cerrojo.unlock();
        }
    }
    
    public void salirNino(Nino nino){
        cerrojo.lock();
        try{
            listaNinos.remove(nino);
        }finally{
            cerrojo.unlock();
        }
        
    }
    
    public void salirDemogorgon(Demogorgon demog){
        cerrojo.lock();
        try{
            listaDemogorgons.remove(demog);
        }finally{
            cerrojo.unlock();
        }
    }
    
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
