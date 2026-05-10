package clases;

/**
 * Interfaz remota RMI expuesta al cliente. Define todos los métodos que el
 * panel de control y el panel de información pueden invocar sobre el servidor
 * para consultar el estado de la simulación o interactuar con ella.
 */
public interface ISimulacionRemota extends java.rmi.Remote {

    // --- PanelControl ---

    /** @return número total de niños presentes actualmente en Hawkins. */
    int getNumNinosEnHawkins() throws java.rmi.RemoteException;

    /** @return niños en el portal del Bosque (cola de ida + esperando vuelta). */
    int getNumNinosPortalBosque() throws java.rmi.RemoteException;
    /** @return niños en el portal del Laboratorio (cola de ida + esperando vuelta). */
    int getNumNinosPortalLaboratorio() throws java.rmi.RemoteException;
    /** @return niños en el portal del Centro Comercial (cola de ida + esperando vuelta). */
    int getNumNinosPortalCentroComercial() throws java.rmi.RemoteException;
    /** @return niños en el portal del Alcantarillado (cola de ida + esperando vuelta). */
    int getNumNinosPortalAlcantarillado() throws java.rmi.RemoteException;

    /** @return número de niños en la zona Laboratorio del Upside Down. */
    int getNumNinosLaboratorio() throws java.rmi.RemoteException;
    /** @return número de niños en la zona Centro Comercial del Upside Down. */
    int getNumNinosCentroComercial() throws java.rmi.RemoteException;
    /** @return número de niños en la zona Bosque del Upside Down. */
    int getNumNinosBosque() throws java.rmi.RemoteException;
    /** @return número de niños en la zona Alcantarillado del Upside Down. */
    int getNumNinosAlcantarillado() throws java.rmi.RemoteException;
    /** @return número de niños capturados actualmente en la Colmena. */
    int getNumNinosColmena() throws java.rmi.RemoteException;

    /** @return número de demogorgons en la zona Laboratorio. */
    int getNumDemogorgonesLaboratorio() throws java.rmi.RemoteException;
    /** @return número de demogorgons en la zona Centro Comercial. */
    int getNumDemogorgonesCentroComercial() throws java.rmi.RemoteException;
    /** @return número de demogorgons en la zona Bosque. */
    int getNumDemogorgonesBosque() throws java.rmi.RemoteException;
    /** @return número de demogorgons en la zona Alcantarillado. */
    int getNumDemogorgonesAlcantarillado() throws java.rmi.RemoteException;

    /** @return lista de hasta 3 cadenas con los demogorgons más capturas (formato "DxxXX: N capturas"). */
    java.util.List<String> getRankingTop3() throws java.rmi.RemoteException;

    /** @return nombre del evento global activo, o {@code "Sin evento activo"} si no hay ninguno. */
    String getEventoActivo() throws java.rmi.RemoteException;
    /** @return segundos restantes del evento activo, o {@code 0} si no hay evento. */
    long getSegundosRestantesEvento() throws java.rmi.RemoteException;

    /** Alterna la simulación entre pausada y en ejecución. */
    void pausarReanudar() throws java.rmi.RemoteException;
    /** @return {@code true} si la simulación está actualmente pausada. */
    boolean isPausado() throws java.rmi.RemoteException;



    // --- PanelInfo ---

    /** @return IDs de los niños en la Calle Principal de Hawkins. */
    java.util.List<String> getNinosCallePrincipal() throws java.rmi.RemoteException;
    /** @return IDs de los niños en el Sótano Byers. */
    java.util.List<String> getNinosSotanoByers() throws java.rmi.RemoteException;
    /** @return IDs de los niños en la Radio WSQK. */
    java.util.List<String> getNinosRadioWSQK() throws java.rmi.RemoteException;

    /** @return IDs de los niños en la zona Laboratorio del Upside Down. */
    java.util.List<String> getNinosLaboratorio() throws java.rmi.RemoteException;
    /** @return IDs de los niños en la zona Centro Comercial del Upside Down. */
    java.util.List<String> getNinosCentroComercial() throws java.rmi.RemoteException;
    /** @return IDs de los niños en la zona Bosque del Upside Down. */
    java.util.List<String> getNinosBosque() throws java.rmi.RemoteException;
    /** @return IDs de los niños en la zona Alcantarillado del Upside Down. */
    java.util.List<String> getNinosAlcantarillado() throws java.rmi.RemoteException;

    /** @return niños en la cola de ida del portal del Bosque (esperando completar grupo). */
    int getNinosEnColaIdaPortalBosque() throws java.rmi.RemoteException;
    /** @return niños en la cola de ida del portal del Laboratorio. */
    int getNinosEnColaIdaPortalLaboratorio() throws java.rmi.RemoteException;
    /** @return niños en la cola de ida del portal del Centro Comercial. */
    int getNinosEnColaIdaPortalCentroComercial() throws java.rmi.RemoteException;
    /** @return niños en la cola de ida del portal del Alcantarillado. */
    int getNinosEnColaIdaPortalAlcantarillado() throws java.rmi.RemoteException;

    /** @return niños esperando cruzar de vuelta a Hawkins por el portal del Bosque. */
    int getNinosEsperandoVueltaPortalBosque() throws java.rmi.RemoteException;
    /** @return niños esperando cruzar de vuelta a Hawkins por el portal del Laboratorio. */
    int getNinosEsperandoVueltaPortalLaboratorio() throws java.rmi.RemoteException;
    /** @return niños esperando cruzar de vuelta a Hawkins por el portal del Centro Comercial. */
    int getNinosEsperandoVueltaPortalCentroComercial() throws java.rmi.RemoteException;
    /** @return niños esperando cruzar de vuelta a Hawkins por el portal del Alcantarillado. */
    int getNinosEsperandoVueltaPortalAlcantarillado() throws java.rmi.RemoteException;

    /** @return capacidad máxima de grupo del portal del Bosque. */
    int getCapacidadPortalBosque() throws java.rmi.RemoteException;
    /** @return capacidad máxima de grupo del portal del Laboratorio. */
    int getCapacidadPortalLaboratorio() throws java.rmi.RemoteException;
    /** @return capacidad máxima de grupo del portal del Centro Comercial. */
    int getCapacidadPortalCentroComercial() throws java.rmi.RemoteException;
    /** @return capacidad máxima de grupo del portal del Alcantarillado. */
    int getCapacidadPortalAlcantarillado() throws java.rmi.RemoteException;

    /** @return IDs de los demogorgons en la zona Laboratorio. */
    java.util.List<String> getDemogorgonesLaboratorio() throws java.rmi.RemoteException;
    /** @return IDs de los demogorgons en la zona Centro Comercial. */
    java.util.List<String> getDemogorgonesCentroComercial() throws java.rmi.RemoteException;
    /** @return IDs de los demogorgons en la zona Bosque. */
    java.util.List<String> getDemogorgonesBosque() throws java.rmi.RemoteException;
    /** @return IDs de los demogorgons en la zona Alcantarillado. */
    java.util.List<String> getDemogorgonesAlcantarillado() throws java.rmi.RemoteException;

    /** @return total acumulado de gotas de sangre recolectadas por los niños. */
    int getNumGotasSangre() throws java.rmi.RemoteException;
    /** @return número de niños actualmente en la Colmena (capturados sin rescatar). */
    int getNumCapturasColmena() throws java.rmi.RemoteException;

}
