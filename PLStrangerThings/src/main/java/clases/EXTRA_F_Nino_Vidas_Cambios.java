package clases;

// ============================================================
// IMPLEMENTACIÓN EXTRA — OPCIÓN K: Niño con vidas
//
// CONCEPTO:
//   Cada niño nace con 3 vidas. Un ataque exitoso del Demogorgon no captura
//   directamente al niño: primero le resta una vida. Solo cuando vidas == 0
//   el niño es capturado y llevado a la Colmena. Mientras tenga vidas (> 0),
//   el Demogorgon "falla" aunque ganó el ataque: el niño resiste y continúa.
//
//   Opcionalmente: si se combina con la Opción I (muerte), con 0 vidas
//   el Demogorgon puede matar al niño en lugar de capturarlo.
//
// ARCHIVOS QUE SE MODIFICAN:
//   1. Nino.java       → añadir atributo vidas + lógica de pérdida de vida
//   2. Demogorgon.java → consultar vidas antes de decidir si capturar
//
// ARCHIVOS QUE NO SE MODIFICAN:
//   Zona.java, Portal.java, Colmena.java, GestorEventos.java,
//   SimulacionBackend.java, SimulacionRemota.java
//
// PREGUNTA TÍPICA DE DEFENSA:
//   "¿Por qué vidas es AtomicInteger y no int normal?"
//   → Porque es leído y modificado desde dos hilos distintos: el Demogorgon
//     llama perderVida() desde su hilo, y el propio Niño lee getVidas() en
//     su ciclo. Con int normal podría haber una race condition: dos Demogorgons
//     leyendo vidas==1 simultáneamente, ambos creyendo que es la última vida
//     y ambos capturando al niño. AtomicInteger.decrementAndGet() garantiza
//     que la operación leer-decrementar-escribir es atómica: solo uno de los
//     dos Demogorgons verá el 0 y procederá a capturar.
//
//   "¿Dónde se resetean las vidas al ser rescatado?"
//   → En run(), tras salir de la Colmena (después de esperarRescate), se
//     llama vidas.set(3) para que el niño renazca con todas sus vidas.
//     Sin esto, el niño rescatado seguiría con 0 vidas y sería capturado
//     inmediatamente en el siguiente ataque.
// ============================================================

public class EXTRA_K_Nino_Vidas_Cambios {

    // =========================================================================
    // CAMBIOS EN Nino.java
    // =========================================================================

    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO N1: Añadir atributo vidas junto a capturado
    // ─────────────────────────────────────────────────────────────────────────
    // En la sección de atributos de Nino.java, junto a:
    //   private boolean capturado = false;
    //
    // Añadir:
    //   private final AtomicInteger vidas = new AtomicInteger(3);
    //
    // [AtomicInteger] porque el Demogorgon llama perderVida() desde su hilo
    // y el Niño lee getVidas() desde el suyo. decrementAndGet() es atómico:
    // evita que dos Demogorgons simultáneos crean los dos que agotan la última vida.
    // Con int normal habría race condition en el decremento.


    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO N2: Añadir método perderVida() y getter getVidas()
    // ─────────────────────────────────────────────────────────────────────────
    // Junto a los otros getters/setters de Nino.java:
    //
    //   /**
    //    * Resta una vida al niño de forma atómica.
    //    * @return vidas restantes DESPUÉS de restar (0 significa capturado)
    //    */
    //   public int perderVida() {
    //       return vidas.decrementAndGet();  // [ATOMIC] leer-decrementar-escribir atómico
    //   }
    //
    //   /** @return número de vidas actuales del niño */
    //   public int getVidas() { return vidas.get(); }
    //
    //   /** Restaura las vidas al máximo (llamado tras el rescate de la Colmena) */
    //   public void resetearVidas() { vidas.set(3); }


    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO N3: Resetear vidas en run() tras ser rescatado de la Colmena
    // ─────────────────────────────────────────────────────────────────────────
    // En run(), dentro del if (this.isCapturado()) { ... }, tras setCapturado(false):
    //
    // ORIGINAL:
    //   upsideDown.getColmena().salirNino(this);
    //   this.setCapturado(false);
    //   this.sangreRecolectada = 0;
    //   log.registrarEvento("Niño " + id + " ha sido rescatado de la Colmena");
    //
    // MODIFICADO:
    //   upsideDown.getColmena().salirNino(this);
    //   this.setCapturado(false);
    //   this.resetearVidas();          // ← NUEVO: renace con 3 vidas completas
    //   this.sangreRecolectada = 0;
    //   log.registrarEvento("Niño " + id + " ha sido rescatado de la Colmena con 3 vidas");


    // =========================================================================
    // CAMBIOS EN Demogorgon.java
    // =========================================================================

    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO D1: Reemplazar la resolución del ataque exitoso
    // ─────────────────────────────────────────────────────────────────────────
    // Localizar en Demogorgon.run() el bloque:
    //   boolean ataqueExitoso = (ThreadLocalRandom.current().nextInt(3) == 0);
    //   if (ataqueExitoso) { ... }
    //
    // REEMPLAZAR POR:
    //
    //   boolean ataqueExitoso = (ThreadLocalRandom.current().nextInt(3) == 0);
    //
    //   if (ataqueExitoso) {
    //       // ← NUEVO: restar una vida antes de decidir si capturar
    //       int vidasRestantes = ninoAtacado.perderVida();  // [ATOMIC decrementAndGet]
    //
    //       if (vidasRestantes <= 0) {
    //           // ── Sin vidas → CAPTURA (comportamiento original) ─────────
    //           log.registrarEvento("Demogorgon " + id + " ha capturado a "
    //               + ninoAtacado.getId() + " (0 vidas)");
    //           zonaActual.salirNino(ninoAtacado);
    //           zonaActual.salirDemogorgon(this);
    //           ninoAtacado.setCapturado(true);
    //           Thread.sleep(ThreadLocalRandom.current().nextLong(500, 1001));
    //           upsideDown.getColmena().entrarNino(ninoAtacado);
    //           upsideDown.getColmena().entrarDemogorgon(this);
    //           upsideDown.getColmena().salirDemogorgon(this);
    //           this.incrementarCapturas();
    //           log.registrarEvento("Niño " + ninoAtacado.getId() + " depositado en Colmena");
    //           zonaActual.finalizarAtaque(ninoAtacado);  // CRÍTICO: siempre desbloquear
    //
    //       } else {
    //           // ── Tiene vidas → RESISTE (el Demogorgon "falla" aunque ganó) ──
    //           log.registrarEvento("Demogorgon " + id + " hirió a "
    //               + ninoAtacado.getId() + " (" + vidasRestantes + " vidas restantes)");
    //           // El niño resiste: misma lógica que el fallo normal
    //           zonaActual.salirDemogorgon(this);
    //           zonaActual.finalizarAtaque(ninoAtacado);  // CRÍTICO: siempre desbloquear
    //       }
    //
    //   } else {
    //       // FALLO sin cambios
    //       log.registrarEvento("Demogorgon " + id + " ha fallado el ataque a " + ninoAtacado.getId());
    //       zonaActual.salirDemogorgon(this);
    //       zonaActual.finalizarAtaque(ninoAtacado);
    //   }


    // ─────────────────────────────────────────────────────────────────────────
    // TABLA DE RESULTADOS CON VIDAS (resumen visual para la defensa)
    // ─────────────────────────────────────────────────────────────────────────
    //
    //   nextInt(3) == 0  (prob 1/3) → ataque exitoso → perderVida()
    //       vidasRestantes > 0  → RESISTE  (log "herido", N vidas restantes)
    //       vidasRestantes == 0 → CAPTURA  (va a la Colmena)
    //
    //   nextInt(3) != 0  (prob 2/3) → FALLO (sin cambios)
    //
    //   Probabilidad de captura en un solo ataque:
    //     Niño con 3 vidas → necesita 3 ataques exitosos consecutivos para ser capturado
    //     Cada ataque exitoso: prob 1/3
    //     Prob captura directa (3 ataques seguidos): (1/3)^3 = ~3.7%
    //     En práctica, entre ataques el niño puede marcharse → más supervivencia


    // ─────────────────────────────────────────────────────────────────────────
    // POR QUÉ finalizarAtaque() EN LA RAMA "RESISTE"
    // ─────────────────────────────────────────────────────────────────────────
    // Aunque el niño "resiste" y el Demogorgon no lo captura, el niño SIGUE
    // bloqueado en esperarFinAtaque() → Condition.await().
    // finalizarAtaque() hace signalAll() y setSiendoAtacado(false) → el niño
    // se despierta y ve siendoAtacado==false → sale del while → continúa su ciclo.
    // Sin finalizarAtaque() en esta rama → DEADLOCK aunque el niño "sobrevivió".
    // Regla de oro: finalizarAtaque() se llama SIEMPRE, en los tres casos.


    // ─────────────────────────────────────────────────────────────────────────
    // COMBINACIÓN CON OPCIÓN I (muerte): vidas + muerte
    // ─────────────────────────────────────────────────────────────────────────
    // Si se implementan ambas opciones, el resultado con 0 vidas puede ser:
    //   resultado = ThreadLocalRandom.current().nextInt(2)
    //   resultado == 0 → CAPTURA (va a la Colmena)
    //   resultado == 1 → MUERTE  (hilo del niño termina)
    //
    // Así: con vidas > 0 → resiste. Con 0 vidas → 50% captura / 50% muerte.
    // Un niño ya capturado una vez y rescatado (vidas reseteadas) tiene otra
    // oportunidad de sobrevivir, pero uno que llega a 0 puede morir.
}
