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
public class Portal {
    
    private Lock cerrojo; //ReentrantLock
    private int capacidadNecesaria;
    private boolean alguienCruzando;
    private boolean grupoCruzando;
    private boolean apagonActivo;
    private Condition colaIda;
    private Condition colaVuelta;
    private Condition colaTurnoCruzar;
    private List<Nino> ninosListosParaCruzar; //CopyOnWriteArrayList
    private List<Nino> ninosEsperandoIda; //CopyOnWriteArrayList
    private List<Nino> ninosEsperandoVuelta; //CopyOnWriteArrayList
    
    public void CruzarHaciaUpsideDown(Nino nino){
        
        cerrojo.lock();
        ninosEsperandoIda.add(nino);
        try{
            // Filtro de entrada
            while(!ninosListosParaCruzar.contains(nino) && 
                  (apagonActivo || alguienCruzando || ninosEsperandoVuelta.size() > 0 || grupoCruzando || ninosEsperandoIda.size() < capacidadNecesaria)) {

                colaIda.await();
            }
            
            //Sacamos a los niños en espera, forman grupo y se meten a la lista de cruze, solo el ultimo niño gestiona el grupo
            if(ninosEsperandoIda.size() >= capacidadNecesaria){
                grupoCruzando = true;
                
                for(int i = 0; i < capacidadNecesaria; i++ ){
                    Nino ninoCruza = ninosEsperandoIda.removeFirst();
                    ninosListosParaCruzar.add(ninoCruza);
                }
                colaIda.signalAll();
            }
            
            //El primer niño se pide el turno de cruzar, los demas esperan
            while(alguienCruzando){
                colaTurnoCruzar.await();
            }
            
            alguienCruzando = true;
            
            //El niño cruza y avisa de que ha terminado de cruzar
            cerrojo.unlock(); // Suelto el cerrojo general de la Zona
            try {
                Thread.sleep(1000); // Cruzo el tubo
            } catch(InterruptedException e) {
                System.out.println("Niño interrumpido en el tubo");
            } finally {
                cerrojo.lock(); // Lo recupero SÍ o SÍ antes de seguir modificando listas
            }
            ninosListosParaCruzar.remove(nino);
            alguienCruzando = false;
            
            //
            if(ninosListosParaCruzar.isEmpty() != true){
                
                colaTurnoCruzar.signalAll(); 
                
            }else{
                
                grupoCruzando = false;
                
                //Primero hacemos signal a la cola de vuelta para respetar la prioridad del enunciado
                colaVuelta.signalAll();
                colaIda.signalAll();
            }
            
            
            
        } catch(InterruptedException e){
        
        } finally{
            cerrojo.unlock();
        }
        
                
    }
}
