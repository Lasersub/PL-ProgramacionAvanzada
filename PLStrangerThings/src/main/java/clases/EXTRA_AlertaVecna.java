package clases;

// ============================================================
// IMPLEMENTACIÓN EXTRA — OPCIÓN A: Evento ALERTA_VECNA
// ARCHIVO DE REFERENCIA PARA LA DEFENSA — NO ES FUNCIONAL SOLO
// CONTIENE LOS CAMBIOS QUE HAY QUE HACER EN GestorEventos.java
// ============================================================
//
// RESUMEN DE CAMBIOS (todos en GestorEventos.java, ningún otro archivo):
//   1. Añadir ALERTA_VECNA al enum TipoEvento
//   2. Añadir flag volatile alertaVecnaActiva
//   3. elegirEvento(): cambiar nextInt(4) → nextInt(5)
//   4. activarEvento(): añadir case ALERTA_VECNA
//   5. desactivarEvento(): añadir case ALERTA_VECNA
//   6. Añadir método privado capturarNinosEnUpsideDown()
//   7. Añadir getter isAlertaVecnaActiva()
//
// POR QUÉ NO HAY QUE TOCAR NINO NI DEMOGORGON:
//   El hilo Niño ya reacciona a isCapturado()==true en su run().
//   Solo hay que poner al niño en ese estado desde aquí.
//   El Demogorgon no se ve afectado por este evento.
// ============================================================

public class EXTRA_AlertaVecna {

    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO 1: En el enum TipoEvento, añadir ALERTA_VECNA al final
    // ─────────────────────────────────────────────────────────────────────────
    // ORIGINAL:
    //   public enum TipoEvento {
    //       NINGUNO, APAGON_LABORATORIO, TORMENTA_UPSIDE_DOWN, INTERVENCION_ELEVEN, RED_MENTAL
    //   }
    //
    // MODIFICADO:
    //   public enum TipoEvento {
    //       NINGUNO, APAGON_LABORATORIO, TORMENTA_UPSIDE_DOWN, INTERVENCION_ELEVEN, RED_MENTAL,
    //       ALERTA_VECNA   // ← nuevo valor
    //   }


    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO 2: Añadir flag volatile junto a los otros flags de evento
    // ─────────────────────────────────────────────────────────────────────────
    // Poner junto a los otros flags (apagonActivo, tormentaActiva, etc.):
    //
    //   private volatile boolean alertaVecnaActiva = false;
    //
    // Es volatile porque es escrito por GestorEventos y puede ser leído
    // por cualquier otro hilo que quiera consultar si el evento está activo.


    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO 3: En elegirEvento(), cambiar nextInt(4) por nextInt(5)
    // ─────────────────────────────────────────────────────────────────────────
    // ORIGINAL:
    //   int r = ThreadLocalRandom.current().nextInt(4);
    //   return TipoEvento.values()[r + 1];
    //
    // MODIFICADO:
    //   int r = ThreadLocalRandom.current().nextInt(5);  // ahora hay 5 eventos posibles
    //   return TipoEvento.values()[r + 1];
    //
    // values() = [NINGUNO(0), APAGON(1), TORMENTA(2), ELEVEN(3), RED_MENTAL(4), ALERTA_VECNA(5)]
    // r+1 garantiza que nunca se elige NINGUNO


    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO 4: En activarEvento(), añadir case ALERTA_VECNA
    // ─────────────────────────────────────────────────────────────────────────
    // Añadir dentro del switch de activarEvento(), después del case RED_MENTAL:
    //
    //   case ALERTA_VECNA:
    //       alertaVecnaActiva = true;
    //       // Bloquear todos los portales (reutilizamos activarApagon que ya existe)
    //       hawkins.getPortalBosque().activarApagon();
    //       hawkins.getPortalLaboratorio().activarApagon();
    //       hawkins.getPortalCentroComercial().activarApagon();
    //       hawkins.getPortalAlcantarillado().activarApagon();
    //       // Capturar todos los niños que estén ahora mismo en el Upside Down
    //       capturarNinosEnUpsideDown();
    //       break;


    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO 5: En desactivarEvento(), añadir case ALERTA_VECNA
    // ─────────────────────────────────────────────────────────────────────────
    // Añadir dentro del switch de desactivarEvento(), después del case RED_MENTAL:
    //
    //   case ALERTA_VECNA:
    //       alertaVecnaActiva = false;
    //       // Reactivar portales al terminar el evento
    //       hawkins.getPortalBosque().desactivarApagon();
    //       hawkins.getPortalLaboratorio().desactivarApagon();
    //       hawkins.getPortalCentroComercial().desactivarApagon();
    //       hawkins.getPortalAlcantarillado().desactivarApagon();
    //       break;


    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO 6: Añadir método privado capturarNinosEnUpsideDown()
    // ─────────────────────────────────────────────────────────────────────────
    // Añadir como método privado en GestorEventos, junto a liberarNinosConEleven():
    //
    // private void capturarNinosEnUpsideDown() {
    //     Zona[] zonas = {
    //         upsideDown.getBosque(),
    //         upsideDown.getLaboratorio(),
    //         upsideDown.getCentroComercial(),
    //         upsideDown.getAlcantarillado()
    //     };
    //     for (Zona zona : zonas) {
    //         // Snapshot de la lista para no modificar mientras iteramos
    //         List<Object> copia = new ArrayList<>(zona.getListaNinos());
    //         for (Object obj : copia) {
    //             Nino nino = (Nino) obj;
    //             nino.setCapturado(true);        // El niño sabrá que fue capturado
    //             zona.salirNino(nino);            // Sacarlo de la zona
    //             upsideDown.getColmena().entrarNino(nino); // Moverlo a la Colmena
    //             nino.getMiHilo().interrupt();    // Sacarlo de su sleep de recolección
    //             zona.finalizarAtaque(nino);      // CRÍTICO: si estaba en esperarFinAtaque()
    //                                              // hay que desbloquearlo o quedará en deadlock
    //             log.registrarEvento("ALERTA VECNA: niño " + nino.getId()
    //                 + " enviado a la Colmena por Vecna");
    //         }
    //     }
    // }
    //
    // POR QUÉ finalizarAtaque() ES CRÍTICO AQUÍ:
    //   Si el niño estaba siendo atacado por un Demogorgon, está bloqueado en
    //   Zona.esperarFinAtaque() → Condition.await(). Si no llamamos finalizarAtaque(),
    //   ese niño nunca se despierta → DEADLOCK. Siempre hay que garantizar que
    //   el niño se desbloquea, independientemente de quién lo capture.


    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO 7: Añadir getter isAlertaVecnaActiva()
    // ─────────────────────────────────────────────────────────────────────────
    // Añadir junto a los otros getters (isApagonActivo, isTormentaActiva, etc.):
    //
    //   public boolean isAlertaVecnaActiva() { return alertaVecnaActiva; }

}
