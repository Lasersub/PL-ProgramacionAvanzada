package clases;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.stream.*;
import java.util.Comparator;

public class SimulacionRemota extends UnicastRemoteObject implements ISimulacionRemota {

    private SimulacionBackend backend;

    public SimulacionRemota(SimulacionBackend backend) throws RemoteException {
        super();
        this.backend = backend;
    }

    @Override
    public int getNinosEnHawkins() throws RemoteException {
        return backend.getHawkins().getCallePrincipal().getListaNinos().size()
             + backend.getHawkins().getSotanoByers().getListaNinos().size()
             + backend.getHawkins().getRadioWSQK().getListaNinos().size();
    }

    @Override
    public int getNinosPortalBosque() throws RemoteException {
        Portal p = backend.getHawkins().getPortalBosque();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosPortalLaboratorio() throws RemoteException {
        Portal p = backend.getHawkins().getPortalLaboratorio();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosPortalCentroComercial() throws RemoteException {
        Portal p = backend.getHawkins().getPortalCentroComercial();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosPortalAlcantarillado() throws RemoteException {
        Portal p = backend.getHawkins().getPortalAlcantarillado();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosLaboratorio() throws RemoteException {
        return backend.getUpsideDown().getLaboratorio().getListaNinos().size();
    }

    @Override
    public int getNinosCentroComercial() throws RemoteException {
        return backend.getUpsideDown().getCentroComercial().getListaNinos().size();
    }

    @Override
    public int getNinosBosque() throws RemoteException {
        return backend.getUpsideDown().getBosque().getListaNinos().size();
    }

    @Override
    public int getNinosAlcantarillado() throws RemoteException {
        return backend.getUpsideDown().getAlcantarillado().getListaNinos().size();
    }

    @Override
    public int getNinosColmena() throws RemoteException {
        return backend.getUpsideDown().getColmena().getListaNinos().size();
    }

    @Override
    public int getDemogorgonesLaboratorio() throws RemoteException {
        return backend.getUpsideDown().getLaboratorio().getListaDemogorgons().size();
    }

    @Override
    public int getDemogorgonesCentroComercial() throws RemoteException {
        return backend.getUpsideDown().getCentroComercial().getListaDemogorgons().size();
    }

    @Override
    public int getDemogorgonesBosque() throws RemoteException {
        return backend.getUpsideDown().getBosque().getListaDemogorgons().size();
    }

    @Override
    public int getDemogorgonesAlcantarillado() throws RemoteException {
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
}
