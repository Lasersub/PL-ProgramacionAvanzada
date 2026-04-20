/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 *
 * @author User
 */
public class Zona {
    
    private List listaNinos; //CopyOnWriteArrayList
    private List listaDemogorgons; //CopyOnWriteArrayList
    private Lock cerrojo; //Reentrar Lock
    private Condition condicionFinAtaque;
    
    public void entrarNino(Nino nino){
        cerrojo.lock();
        try{
            listaNinos.add(nino);
        }
        catch(Exception e){
            
        }
        finally{
            cerrojo.unlock();
        }
        
    }
    
    public void entrarDemogorgon(Demogorgon demog){
        cerrojo.lock();
        try{
            listaDemogorgons.add(demog);
        }
        catch(Exception e){
            
        }
        finally{
            cerrojo.unlock();
        }
    }
    
    public void salirNino(Nino nino){
        cerrojo.lock();
        try{
            listaNinos.remove(nino);
        }
        catch(Exception e){
            
        }
        finally{
            cerrojo.unlock();
        }
        
    }
    
    public void salirDemogorgon(Demogorgon demog){
        cerrojo.lock();
        try{
            listaDemogorgons.remove(demog);
        }
        catch(Exception e){
            
        }
        finally{
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
}
