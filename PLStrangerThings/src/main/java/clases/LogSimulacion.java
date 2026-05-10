/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;


import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Registrador de eventos de la simulación. Escribe cada evento con marca de tiempo
 * en el fichero {@code hawkins.txt} y en la consola de forma thread-safe,
 * usando un {@link java.util.concurrent.locks.ReentrantLock} para serializar
 * los accesos concurrentes al fichero.
 */
public class LogSimulacion {

    // El Lock que protegerá la escritura
    private final ReentrantLock cerrojo = new ReentrantLock();
    
    // Nombre del fichero y formato de la fecha/hora
    private final String RUTA_FICHERO = "hawkins.txt";
    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Registra un evento con marca de tiempo en el fichero de log y en consola.
     * Este método es thread-safe: serializa las escrituras con un cerrojo.
     *
     * @param evento descripción textual del evento ocurrido
     */
    public void registrarEvento(String evento) {
        cerrojo.lock(); 
        try {
            // Obtenemos la hora exacta en este instante
            LocalDateTime ahora = LocalDateTime.now();
            String fechaFormateada = ahora.format(formatoFecha);
            
            // Construimos la línea final a escribir
            String lineaLog = "[" + fechaFormateada + "] --> " + evento;
            
            // Usamos try-with-resources para que el fichero se cierre automáticamente
            // El parámetro 'true' en FileWriter indica que se añadirá al final del fichero (append)
            try (PrintWriter escritor = new PrintWriter(new FileWriter(RUTA_FICHERO, true))) {
                escritor.println(lineaLog);
                
                // Imprimir también por consola
                System.out.println(lineaLog); 
            } catch (IOException e) {
                System.err.println("Error al escribir en el log de la Simulacion: " + e.getMessage());
            }
            
        } finally {
            cerrojo.unlock(); 
        }
    }
}
