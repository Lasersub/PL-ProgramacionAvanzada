package clases;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.Comparator;

public class SimulacionRemota extends UnicastRemoteObject implements ISimulacionRemota {

    private SimulacionBackend backend;

    public SimulacionRemota(SimulacionBackend backend) throws RemoteException {
        super();
        this.backend = backend;
    }

    @Override
    public int getNumNinosEnHawkins() throws RemoteException {
        return backend.getHawkins().getCallePrincipal().getListaNinos().size()
             + backend.getHawkins().getSotanoByers().getListaNinos().size()
             + backend.getHawkins().getRadioWSQK().getListaNinos().size();
    }

    @Override
    public int getNumNinosPortalBosque() throws RemoteException {
        Portal p = backend.getHawkins().getPortalBosque();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNumNinosPortalLaboratorio() throws RemoteException {
        Portal p = backend.getHawkins().getPortalLaboratorio();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNumNinosPortalCentroComercial() throws RemoteException {
        Portal p = backend.getHawkins().getPortalCentroComercial();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNumNinosPortalAlcantarillado() throws RemoteException {
        Portal p = backend.getHawkins().getPortalAlcantarillado();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNumNinosLaboratorio() throws RemoteException {
        return backend.getUpsideDown().getLaboratorio().getListaNinos().size();
    }

    @Override
    public int getNumNinosCentroComercial() throws RemoteException {
        return backend.getUpsideDown().getCentroComercial().getListaNinos().size();
    }

    @Override
    public int getNumNinosBosque() throws RemoteException {
        return backend.getUpsideDown().getBosque().getListaNinos().size();
    }

    @Override
    public int getNumNinosAlcantarillado() throws RemoteException {
        return backend.getUpsideDown().getAlcantarillado().getListaNinos().size();
    }

    @Override
    public int getNumNinosColmena() throws RemoteException {
        return backend.getUpsideDown().getColmena().getListaNinos().size();
    }

    @Override
    public int getNumDemogorgonesLaboratorio() throws RemoteException {
        return backend.getUpsideDown().getLaboratorio().getListaDemogorgons().size();
    }

    @Override
    public int getNumDemogorgonesCentroComercial() throws RemoteException {
        return backend.getUpsideDown().getCentroComercial().getListaDemogorgons().size();
    }

    @Override
    public int getNumDemogorgonesBosque() throws RemoteException {
        return backend.getUpsideDown().getBosque().getListaDemogorgons().size();
    }

    @Override
    public int getNumDemogorgonesAlcantarillado() throws RemoteException {
        return backend.getUpsideDown().getAlcantarillado().getListaDemogorgons().size();
    }

    @Override
    public List<String> getRankingTop3() throws RemoteException {
        List<Demogorgon> copia = new ArrayList<>(backend.getDemogorgons());
        copia.sort(Comparator.comparingInt(Demogorgon::getCapturas).reversed());
        List<String> resultado = new ArrayList<>();
        for (int i = 0; i < Math.min(3, copia.size()); i++) {
            Demogorgon d = copia.get(i);
            resultado.add(d.getId() + ": " + d.getCapturas() + " capturas");
        }
        return resultado;
    }

    @Override
    public String getEventoActivo() throws RemoteException {
        GestorEventos gestor = backend.getGestorEventos();
        if (gestor == null || gestor.getEventoActual() == GestorEventos.TipoEvento.NINGUNO) {
            return "Sin evento activo";
        }
        return gestor.getEventoActual().name().replace('_', ' ');
    }

    @Override
    public long getSegundosRestantesEvento() throws RemoteException {
        GestorEventos gestor = backend.getGestorEventos();
        if (gestor == null) return 0;
        return gestor.getSegundosRestantes();
    }

    @Override
    public void pausarReanudar() throws RemoteException {
        if (backend.isPausado()) {
            backend.reanudarSimulacion();
        } else {
            backend.pausarSimulacion();
        }
    }

    @Override
    public boolean isPausado() throws RemoteException {
        return backend.isPausado();
    }



    @Override
    public List<String> getNinosLaboratorio() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getLaboratorio().getListaNinos()) ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosCentroComercial() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getCentroComercial().getListaNinos()) ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosBosque() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getBosque().getListaNinos()) ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosAlcantarillado() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getAlcantarillado().getListaNinos()) ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getDemogorgonesLaboratorio() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getLaboratorio().getListaDemogorgons()) ids.add(((Demogorgon) obj).getId());
        return ids;
    }

    @Override
    public List<String> getDemogorgonesCentroComercial() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getCentroComercial().getListaDemogorgons()) ids.add(((Demogorgon) obj).getId());
        return ids;
    }

    @Override
    public List<String> getDemogorgonesBosque() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getBosque().getListaDemogorgons()) ids.add(((Demogorgon) obj).getId());
        return ids;
    }

    @Override
    public List<String> getDemogorgonesAlcantarillado() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getAlcantarillado().getListaDemogorgons()) ids.add(((Demogorgon) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosCallePrincipal() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getHawkins().getCallePrincipal().getListaNinos()) ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosSotanoByers() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getHawkins().getSotanoByers().getListaNinos()) ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosRadioWSQK() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getHawkins().getRadioWSQK().getListaNinos()) ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public int getNinosEnColaIdaPortalBosque() throws RemoteException {
        return backend.getHawkins().getPortalBosque().getNinosEnColaIda().size();
    }

    @Override
    public int getNinosEnColaIdaPortalLaboratorio() throws RemoteException {
        return backend.getHawkins().getPortalLaboratorio().getNinosEnColaIda().size();
    }

    @Override
    public int getNinosEnColaIdaPortalCentroComercial() throws RemoteException {
        return backend.getHawkins().getPortalCentroComercial().getNinosEnColaIda().size();
    }

    @Override
    public int getNinosEnColaIdaPortalAlcantarillado() throws RemoteException {
        return backend.getHawkins().getPortalAlcantarillado().getNinosEnColaIda().size();
    }

    @Override
    public int getNinosEsperandoVueltaPortalBosque() throws RemoteException {
        return backend.getHawkins().getPortalBosque().getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosEsperandoVueltaPortalLaboratorio() throws RemoteException {
        return backend.getHawkins().getPortalLaboratorio().getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosEsperandoVueltaPortalCentroComercial() throws RemoteException {
        return backend.getHawkins().getPortalCentroComercial().getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosEsperandoVueltaPortalAlcantarillado() throws RemoteException {
        return backend.getHawkins().getPortalAlcantarillado().getNinosEsperandoVuelta();
    }

    @Override
    public int getCapacidadPortalBosque() throws RemoteException {
        return backend.getHawkins().getPortalBosque().getCapacidad();
    }

    @Override
    public int getCapacidadPortalLaboratorio() throws RemoteException {
        return backend.getHawkins().getPortalLaboratorio().getCapacidad();
    }

    @Override
    public int getCapacidadPortalCentroComercial() throws RemoteException {
        return backend.getHawkins().getPortalCentroComercial().getCapacidad();
    }

    @Override
    public int getCapacidadPortalAlcantarillado() throws RemoteException {
        return backend.getHawkins().getPortalAlcantarillado().getCapacidad();
    }

    @Override
    public int getNumGotasSangre() throws RemoteException {
        return backend.getGestorEventos().getSangreTotal();
    }

    @Override
    public int getNumCapturasColmena() throws RemoteException {
        return backend.getUpsideDown().getColmena().getListaNinos().size();
    }
}
