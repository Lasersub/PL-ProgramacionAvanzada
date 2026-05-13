package clases;

// ============================================================
// IMPLEMENTACIÓN EXTRA — OPCIÓN D: ZonaConAforo
//
// ARCHIVO 2 de 2: Cambios en SimulacionBackend.java
//
// RESUMEN DEL CAMBIO:
//   Un único cambio de una línea: cambiar el tipo de sotanoByers de Zona
//   a ZonaConAforo. El resto del sistema no se entera gracias al polimorfismo.
//
// POR QUÉ SOLO UNA LÍNEA:
//   Nino.java llama hawkins.getSotanoByers().entrarNino(this) y .salirNino(this).
//   getSotanoByers() devuelve tipo Zona. ZonaConAforo ES una Zona (herencia).
//   Java resuelve en tiempo de ejecución qué entrarNino() ejecutar
//   (el de ZonaConAforo, con semáforo) → polimorfismo en acción.
//   Nino.java no necesita saber nada de semáforos ni de ZonaConAforo.
//
// ARCHIVOS QUE NO SE MODIFICAN:
//   Nino.java       → usa entrarNino/salirNino sin saber la implementación
//   Hawkins.java    → getSotanoByers() devuelve Zona; ZonaConAforo es Zona
//   Zona.java       → no cambia
//   Demogorgon.java → no usa el Sótano Byers
// ============================================================

public class EXTRA_D_SimulacionBackend_Cambios {

    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO ÚNICO: En SimulacionBackend.java, cambiar la declaración de sotanoByers
    // ─────────────────────────────────────────────────────────────────────────
    //
    // ORIGINAL (línea ~25 de SimulacionBackend.java):
    //   private final Zona sotanoByers = new Zona();
    //
    // MODIFICADO:
    //   private final Zona sotanoByers = new ZonaConAforo(10);
    //                                    ^^^^^^^^^^^^^^^^
    //                                    Aforo máximo: 10 niños simultáneos
    //                                    (ajustable según lo que pida el enunciado)
    //
    // El tipo declarado sigue siendo Zona (no ZonaConAforo) porque:
    //   - Hawkins.java tiene getSotanoByers() devolviendo Zona
    //   - Si cambiáramos el tipo a ZonaConAforo habría que cambiar también Hawkins
    //   - Con el tipo Zona aprovechamos el polimorfismo: la JVM ejecuta
    //     ZonaConAforo.entrarNino() automáticamente en tiempo de ejecución


    // ─────────────────────────────────────────────────────────────────────────
    // CÓMO QUEDARÍA EL BLOQUE DE DECLARACIONES (visión completa)
    // ─────────────────────────────────────────────────────────────────────────
    //
    //   // Zonas Hawkins
    //   private final Zona callePrincipal = new Zona();
    //   private final Zona sotanoByers    = new ZonaConAforo(10);  // ← CAMBIADO
    //   private final Zona radioWSQK      = new Zona();
    //
    // Todo lo demás en SimulacionBackend permanece exactamente igual.


    // ─────────────────────────────────────────────────────────────────────────
    // SI EL ENUNCIADO PIDE TAMBIÉN LIMITAR OTRAS ZONAS
    // ─────────────────────────────────────────────────────────────────────────
    // El mismo cambio aplica a cualquier zona, por ejemplo Radio WSQK:
    //
    //   private final Zona radioWSQK = new ZonaConAforo(5);  // máx 5 niños
    //
    // O limitar zonas del Upside Down (aunque estas son más peligrosas
    // de limitar porque los Demogorgons también entran en ellas y usan
    // entrarDemogorgon, no entrarNino, por lo que el semáforo no las afecta):
    //
    //   private final Zona bosque = new ZonaConAforo(20);


    // ─────────────────────────────────────────────────────────────────────────
    // DEMOSTRACIÓN DE POLIMORFISMO EN TIEMPO DE EJECUCIÓN
    // ─────────────────────────────────────────────────────────────────────────
    // Nino.java tiene este código (sin cambios):
    //
    //   hawkins.getSotanoByers().entrarNino(this);  // llama a Zona.entrarNino()
    //   Thread.sleep(...);
    //   hawkins.getSotanoByers().salirNino(this);   // llama a Zona.salirNino()
    //
    // Antes del cambio: getSotanoByers() devuelve una Zona → se ejecuta Zona.entrarNino()
    // Después del cambio: getSotanoByers() devuelve una ZonaConAforo (que ES una Zona)
    //   → la JVM busca el método entrarNino() en ZonaConAforo (existe, está @Override)
    //   → se ejecuta ZonaConAforo.entrarNino() con el semáforo
    //   → Nino.java no cambió ni una línea, pero ahora el Sótano tiene aforo
    //
    // Esto es el principio Open/Closed: abierto para extensión (ZonaConAforo),
    // cerrado para modificación (Nino, Hawkins, SimulacionBackend no cambian).
}
