package clases;

// ============================================================
// IMPLEMENTACIÓN EXTRA — OPCIÓN I: Demogorgon puede matar niños
//
// CONCEPTO:
//   El resultado de un ataque pasa de ser binario (captura/fallo) a tener
//   tres resultados posibles con igual probabilidad (1/3 cada uno):
//     - RESULTADO 0 → CAPTURA  (comportamiento actual sin cambios)
//     - RESULTADO 1 → MUERTE   (el hilo del niño termina definitivamente)
//     - RESULTADO 2 → FALLO    (comportamiento actual sin cambios)
//
// ARCHIVOS QUE SE MODIFICAN:
//   1. Nino.java      → añadir flag 'muerto' y getter isMuerto()
//   2. Demogorgon.java → cambiar la lógica de resolución del ataque
//
// ARCHIVOS QUE NO SE MODIFICAN:
//   Zona.java, Portal.java, Colmena.java, GestorEventos.java,
//   SimulacionBackend.java, SimulacionRemota.java
//
// PREGUNTA TÍPICA DE DEFENSA:
//   "¿Cómo matas al hilo del niño?"
//   → Llamando nino.getMiHilo().interrupt() después de marcar muerto=true
//     y sacarlo de la zona. El hilo del Niño tiene un catch(InterruptedException)
//     en su run() que imprime un mensaje y deja terminar el método run() → el
//     hilo muere de forma limpia. Es la misma mecánica que ya existe para
//     el shutdown de la simulación, reutilizada para la muerte en combate.
//
//   "¿Por qué necesitas el flag muerto en Nino si ya tienes interrupt()?"
//   → Porque interrupt() solo lanza la excepción si el hilo está en un
//     método bloqueante (sleep, await, etc). Si el niño está en una línea
//     de código normal cuando lo marcamos, podría no morir inmediatamente.
//     El flag muerto=true garantiza que aunque el niño no esté bloqueado
//     en ese instante, en su próxima comprobación en intentarRecolectarSangre
//     sabrá que debe terminar. Además, finalizarAtaque() lo despierta si
//     estaba en esperarFinAtaque(), y al comprobar muerto=true en el catch
//     de intentarRecolectarSangre lanza una excepción que sube hasta el
//     catch exterior de run() → muerte limpia.
// ============================================================

public class EXTRA_E_Demogorgon_Mata_Cambios {

    // =========================================================================
    // CAMBIOS EN Nino.java
    // =========================================================================

    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO N1: Añadir atributo 'muerto' junto a 'capturado'
    // ─────────────────────────────────────────────────────────────────────────
    // En la sección de atributos de Nino.java, junto a:
    //   private boolean capturado = false;
    //
    // Añadir:
    //   private volatile boolean muerto = false;
    //
    // [volatile] porque es escrito por el Demogorgon (hilo externo) y leído
    // por el propio hilo del Niño. Mismo razonamiento que siendoAtacado.


    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO N2: Añadir getter y setter en Nino.java
    // ─────────────────────────────────────────────────────────────────────────
    // Junto a los otros getters/setters de Nino.java:
    //
    //   public boolean isMuerto() { return muerto; }
    //   public void setMuerto(boolean muerto) { this.muerto = muerto; }


    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO N3: En intentarRecolectarSangre(), manejar la muerte en el catch
    // ─────────────────────────────────────────────────────────────────────────
    // En el catch(InterruptedException e) de intentarRecolectarSangre(),
    // ANTES de llamar esperarFinAtaque(), añadir comprobación de muerte:
    //
    // ORIGINAL:
    //   } catch (InterruptedException e) {
    //       zonaInsegura.esperarFinAtaque(this);
    //       if (!this.isCapturado()) { ... }
    //   }
    //
    // MODIFICADO:
    //   } catch (InterruptedException e) {
    //       zonaInsegura.esperarFinAtaque(this);
    //
    //       // ← NUEVO: si el Demogorgon nos mató, relanzar para llegar al
    //       // catch exterior de run() y terminar el hilo limpiamente
    //       if (this.isMuerto()) {
    //           throw new InterruptedException("Niño " + id + " ha muerto");
    //       }
    //
    //       if (!this.isCapturado()) { ... }  // resto sin cambios
    //   }
    //
    // Cuando se lanza la InterruptedException desde aquí, sube hasta el
    // catch(InterruptedException e) del run() → el hilo imprime el mensaje
    // de muerte y termina. Muerte limpia sin cambiar el catch del run().


    // =========================================================================
    // CAMBIOS EN Demogorgon.java
    // =========================================================================

    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO D1: Añadir contador de muertes (opcional, para estadísticas)
    // ─────────────────────────────────────────────────────────────────────────
    // Junto al AtomicInteger capturas, añadir:
    //
    //   private final AtomicInteger muertes = new AtomicInteger(0);
    //
    // Y su getter:
    //   public int getMuertes() { return muertes.get(); }


    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO D2: Reemplazar la resolución del ataque (el bloque más importante)
    // ─────────────────────────────────────────────────────────────────────────
    // Localizar en Demogorgon.run() estas líneas (tras Thread.sleep(tiempoAtaque)):
    //
    // ORIGINAL:
    //   boolean ataqueExitoso = (ThreadLocalRandom.current().nextInt(3) == 0);
    //   if (ataqueExitoso) {
    //       // ... lógica de captura ...
    //   } else {
    //       // ... lógica de fallo ...
    //   }
    //
    // REEMPLAZAR POR:
    //
    //   // Tres resultados posibles con igual probabilidad (1/3 cada uno)
    //   int resultado = ThreadLocalRandom.current().nextInt(3);
    //   // resultado == 0 → CAPTURA
    //   // resultado == 1 → MUERTE
    //   // resultado == 2 → FALLO
    //
    //   if (resultado == 0) {
    //       // ── CAPTURA (igual que antes, sin cambios) ────────────────────
    //       log.registrarEvento("Demogorgon " + id + " ha capturado a " + ninoAtacado.getId());
    //       zonaActual.salirNino(ninoAtacado);
    //       zonaActual.salirDemogorgon(this);
    //       ninoAtacado.setCapturado(true);
    //       Thread.sleep(ThreadLocalRandom.current().nextLong(500, 1001));
    //       upsideDown.getColmena().entrarNino(ninoAtacado);
    //       upsideDown.getColmena().entrarDemogorgon(this);
    //       upsideDown.getColmena().salirDemogorgon(this);
    //       this.incrementarCapturas();
    //       log.registrarEvento("El niño " + ninoAtacado.getId() + " depositado en Colmena");
    //       zonaActual.finalizarAtaque(ninoAtacado);   // CRÍTICO: siempre desbloquear
    //
    //   } else if (resultado == 1) {
    //       // ── MUERTE ────────────────────────────────────────────────────
    //       log.registrarEvento("Demogorgon " + id + " ha MATADO a " + ninoAtacado.getId());
    //       zonaActual.salirNino(ninoAtacado);         // Sacar de la zona
    //       zonaActual.salirDemogorgon(this);
    //       ninoAtacado.setMuerto(true);               // Marcar como muerto
    //       muertes.incrementAndGet();                 // Contar la muerte (opcional)
    //       zonaActual.finalizarAtaque(ninoAtacado);   // CRÍTICO: despertar al niño
    //       // El niño se despierta en esperarFinAtaque(), ve isMuerto()==true
    //       // en intentarRecolectarSangre(), lanza InterruptedException
    //       // → sube al catch de run() → hilo termina limpiamente
    //       ninoAtacado.getMiHilo().interrupt();       // Asegurar despertar si no estaba en await
    //
    //   } else {
    //       // ── FALLO (igual que antes, sin cambios) ─────────────────────
    //       log.registrarEvento("Demogorgon " + id + " ha fallado el ataque a " + ninoAtacado.getId());
    //       zonaActual.salirDemogorgon(this);
    //       zonaActual.finalizarAtaque(ninoAtacado);   // CRÍTICO: siempre desbloquear
    //   }


    // ─────────────────────────────────────────────────────────────────────────
    // POR QUÉ finalizarAtaque() SE LLAMA TAMBIÉN EN LA MUERTE
    // ─────────────────────────────────────────────────────────────────────────
    // El niño puede estar bloqueado en esperarFinAtaque() → Condition.await().
    // Si no llamamos finalizarAtaque(), el niño nunca se despierta → DEADLOCK,
    // aunque el niño esté "muerto" conceptualmente.
    // finalizarAtaque() hace signalAll() → el niño se despierta, comprueba
    // isMuerto()==true y lanza InterruptedException → hilo termina.
    // Regla de oro: finalizarAtaque() se llama SIEMPRE en los tres casos.


    // ─────────────────────────────────────────────────────────────────────────
    // ORDEN CORRECTO EN LA MUERTE: finalizarAtaque ANTES de interrupt()
    // ─────────────────────────────────────────────────────────────────────────
    // Si el niño está en Condition.await() (esperarFinAtaque):
    //   → finalizarAtaque() hace signalAll() → se despierta → ve muerto=true → muere
    //   → interrupt() ya no es necesario pero no hace daño
    //
    // Si el niño está en Thread.sleep() (recolectando):
    //   → finalizarAtaque() no hace nada (el niño no está en await)
    //   → interrupt() lanza InterruptedException en el sleep → catch → ve muerto=true → muere
    //
    // Por eso se llaman los dos: cubren los dos posibles estados del niño.
}
