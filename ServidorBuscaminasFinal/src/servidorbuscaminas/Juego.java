package servidorbuscaminas;

import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author DARKCEUS
 */
public class Juego {

    public static final int FILAS = 20;
    public static final int COLUMNAS = 20;
    public static final int TAM_ALTO = 20;
    public static final int TAM_ANCHO = 20;
    public static final int NUMERO_MINAS = 20;
    private int limpiarTablero;
    private final Campo[][] TABLERO;
    private final ArrayList<Campo> listaMinas;
    private int camposVisibles;
    private boolean inicio;
    private final int[] NUM_X = {-1, 0, 1, -1, 1, -1, 0, 1};
    private final int[] NUM_Y = {-1, -1, -1, 0, 0, 1, 1, 1};
    private final int[] CRUZ_X = {0, -1, 1, 0};
    private final int[] CRUZ_Y = {-1, 0, 0, 1};
    private final Sala sala;
    private int minutos;
    private int segundos;
    private ArrayList<Campo> listaDisponibles;
    private Thread t;
    
    public Juego() {
        this.TABLERO = null;
        this.listaMinas = null;
        this.sala = null;
    }

    public Juego(Sala sala) {
        this.inicio = true;
        this.camposVisibles = 0;
        this.listaDisponibles = new ArrayList<>();
        this.segundos = 0;
        this.minutos = 0;
        this.sala = sala;
        this.TABLERO = new Campo[FILAS][COLUMNAS];
        this.listaMinas = new ArrayList<>();
        crearTablero();
        colocarMinas();
        checarPerimetro();
        imprimirInfo();
        iniciarTiempo();
    }
    
    private void iniciarTiempo() {
        t = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        sleep(1000);
                        segundos++;
                        if (segundos == 30){
                            aumentarMinas();
                        }
                        if (segundos == 60) {
                            aumentarMinas();
                            segundos = 0;
                            minutos++;
                        }
                    } catch (InterruptedException e) {
                    }
                }
            }
        };
        t.start();
    }

    public void setInicio(boolean inicio) {
        this.inicio = inicio;
    }

    public boolean getInicio() {
        return this.inicio;
    }

    private void imprimirInfo() {
        for (int y = 0; y < COLUMNAS; y++) {
            for (int x = 0; x < FILAS; x++) {
                System.out.print(TABLERO[x][y].getValor());
            }
            System.out.println("");
        }
    }

    private void crearTablero() {
        for (int y = 0; y < COLUMNAS; y++) {
            for (int x = 0; x < FILAS; x++) {
                TABLERO[x][y] = new Campo(x, y);
            }
        }
    }
    
    private int obtenerRandom(int num){
        return (int) (Math.random() * num);
    }
    
    private boolean comprobarLimites(int x2, int y2){
        return (x2 > NUM_X[0] && x2 < Juego.FILAS) && (y2 > NUM_Y[0] && y2 < Juego.COLUMNAS);
    }

    private void colocarMinas() {
        boolean minasC;
        int fila;
        int columna;
        Campo campo;
        int contarMinas = 0;
        do {
            minasC = false;
            for (int i = 0; i < NUMERO_MINAS; i++) {
                fila = obtenerRandom(FILAS);
                columna = obtenerRandom(COLUMNAS);
                campo = TABLERO[fila][columna];
                if (campo.comprobarVacio()) {
                    campo.colocarMina();
                    listaMinas.add(campo);
                    contarMinas++;
                } else {
                    i--;
                }
            }
            if (contarMinas == NUMERO_MINAS) {
                minasC = true;
            }
        } while (minasC == false);
        System.out.println(contarMinas);
    }
    
    private void checarPerimetro() {
        int x2;
        int y2;
        Campo campo;
        for (Campo campo2 : this.listaMinas) {
            for (int i = 0; i < 8; i++) {
                x2 = NUM_X[i] + campo2.getX();
                y2 = NUM_Y[i] + campo2.getY();
                if (comprobarLimites(x2, y2)) {
                    campo = TABLERO[x2][y2];
                    if (!campo.comprobarMina()) {
                        campo.aumentarValor();
                    }
                }
            }
        }
    }
    
    private void checarPerimetro(Campo campo2) {
        int x2;
        int y2;
        Campo campo;
        System.out.println("Se agregó mina X:" + campo2.getX() + ", Y:" + campo2.getY());
        for (int i = 0; i < 8; i++) {
            x2 = NUM_X[i] + campo2.getX();
            y2 = NUM_Y[i] + campo2.getY();
            if (comprobarLimites(x2, y2)) {
                campo = TABLERO[x2][y2];
                if (!campo.comprobarMina()) {
                    campo.aumentarValor();
                    if (campo.comprobarVisible()) {
                        sala.enviarInfo("2ACTUALIZAR " + campo.getX() + "," + campo.getY() + "," + campo.getValor());
                    }
                }
            }
            if (i == 7) {
                sala.enviarInfo("AUMENTARBANDERAS");
            }
        }
    }
    
    private void aumentarMinas() {
        this.listaDisponibles = new ArrayList<>();
        int num = checarEspacio();
        if (num >= 0) {
            checarPerimetro(listaDisponibles.get(num));
            imprimirInfo();
        }
    }
    
    private int checarEspacio() {
        Campo campo;
        for (int j = 0; j < Juego.COLUMNAS; j++) {
            for (int i = 0; i < Juego.FILAS; i++) {
                campo = TABLERO[i][j];
                if (campo.comprobarOculto() && campo.comprobarValorValido()) {
                    listaDisponibles.add(campo);
                }
            }
        }
        if (listaDisponibles.isEmpty()) {
            return -1;
        }
        int random = obtenerRandom(listaDisponibles.size());
        listaDisponibles.get(random).colocarMina();
        listaMinas.add(listaDisponibles.get(random));
        return random;
    }
    
    private boolean checarClic(Jugador jugador, Campo campo) {
        if (jugador.checarClic()) {
            return true;
        }
        boolean esquina = checarEsquina(jugador, campo);
        if (esquina) {
            jugador.darClic();
            return true;
        }
        if (jugador.getID() == 1 && !esquina) {
            jugador.getPW().println("INFOMESSAGE Debes iniciar por la Izquierda");
        } else if (jugador.getID() == 2 && !esquina) {
            jugador.getPW().println("INFOMESSAGE Debes iniciar por la Derecha");
        } else if (jugador.getID() == 3 && !esquina) {
            jugador.getPW().println("INFOMESSAGE Debes iniciar por la Arriba");
        } else if (jugador.getID() == 4 && !esquina) {
            jugador.getPW().println("INFOMESSAGE Debes iniciar por la Abajo");
        }
        return false;
    }
    
    private boolean checar(Jugador jugador) {
        int tam;
        switch (jugador.getID()) {
            case 1: {
                tam = COLUMNAS;
                for (int i = 0; i < tam; i++) {
                    if (TABLERO[0][i].getEstado() == Campo.ESTADO_INICIAL) {
                        return true;
                    }
                }
                break;
            }
            case 2: {
                tam = COLUMNAS;
                int info = (Juego.FILAS - 1);
                for (int i = 0; i < tam; i++) {
                    if (TABLERO[info][i].getEstado() == Campo.ESTADO_INICIAL) {
                        return true;
                    }
                }
                break;
            }
            case 3: {
                tam = FILAS;
                for (int i = 0; i < tam; i++) {
                    if (TABLERO[i][0].getEstado() == Campo.ESTADO_INICIAL) {
                        return true;
                    }
                }
                break;
            }
            case 4: {
                tam = FILAS;
                int info = (Juego.COLUMNAS - 1);
                for (int i = 0; i < tam; i++) {
                    if (TABLERO[i][info].getEstado() == Campo.ESTADO_INICIAL) {
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }
    
    private boolean checarEsquina(Jugador jugador, Campo campo) {
        if (jugador.getID() == 1 && (campo.getX() == 0 || !checar(jugador))) {
            return true;
        } else if (jugador.getID() == 2 && (campo.getX() == (Juego.FILAS - 1) || !checar(jugador))) {
            return true;
        } else if (jugador.getID() == 3 && (campo.getY() == 0 || !checar(jugador))) {
            return true;
        } else if (jugador.getID() == 4 && (campo.getY() == (Juego.COLUMNAS - 1) || !checar(jugador))) {
            return true;
        }
        return false;
    }
    
    public void descubrirCampo(Jugador jugador, int x, int y) {
        if (sala.getIniciado() && jugador.continuarJugando()) {
            Campo campo = TABLERO[x][y];
            if (checarClic(jugador, campo)) {
                if (campo.comprobarOculto()/* || (!campo.getAdmin().equals(jugador) && campo.getEstado() == Campo.ESTADO_BANDERA)*/) {
                    campo.hacerVisible();
                    campo.setAdmin(jugador);
                    if (!campo.comprobarMina()) {
                        camposVisibles++;
                    }
                    sala.enviarInfo("DESCUBRIRCAMPO " + x + "," + y + "," + campo.getValor());
                    if (campo.comprobarMina()) {
                        jugador.perder();
                        campo.hacerVisible();
                        sala.agregarPerdedor(jugador);
                        sala.enviarInfo("HAYMINA " + x + "," + y + "," + jugador.getID());
                        sala.enviarInfo("MESSAGE " + jugador.getNombre() + " ha perdido");
                    } else if (campo.comprobarVacio()) {
                        revelarPerimetro(jugador, campo);
                    }
                }
                if (inicio) {
                    checarMinas();
                }
            }
        } else {
            jugador.getPW().println("INFOMESSAGE Ya perdiste, no puedes jugar");
        }
    }

    private void revelarPerimetro(Jugador jugador, Campo campo) {
        int x2;
        int y2;
        Campo campo2;
        for (int i = 0; i < 8; i++) {
            x2 = NUM_X[i] + campo.getX();
            y2 = NUM_Y[i] + campo.getY();
            if (comprobarLimites(x2, y2)) {
                campo2 = TABLERO[x2][y2];
                if (campo2.comprobarOculto()) {
                    campo2.setAdmin(jugador);
                    campo2.hacerVisible();
                    camposVisibles++;
                    sala.enviarInfo("DESCUBRIRCAMPO " + x2 + "," + y2 + "," + campo2.getValor());
                    if (campo2.comprobarVacio()) {
                        revelarPerimetro(jugador, campo2);
                    }
                }
            }
        }
    }
    
    public void gestionarBandera(Jugador jugador, int x, int y, int minasRes) {
        if (sala.getIniciado() && jugador.continuarJugando()) {
            Campo campo = TABLERO[x][y];
            if (campo.comprobarOculto() && minasRes > 0) {
                if (checarClic(jugador, campo)) {
                    campo.colocarBandera();
                    campo.setAdmin(jugador);
                    jugador.agregarBandera(campo);
                    sala.enviarInfo("PONERBANDERA " + x + "," + y + "," + jugador.getID());
                    if (inicio) {
                        checarMinas();
                    }
                }
            } else if (campo.comprobarBandera() && campo.getAdmin().equals(jugador)) {
                campo.ocultar();
                campo.setAdmin(jugador);
                jugador.quitarBandera(campo);
                sala.enviarInfo("QUITARBANDERA " + x + "," + y + "," + jugador.getID());
            }
        } else {
            jugador.getPW().println("INFOMESSAGE Ya perdiste, no puedes jugar");
        }
    }
    
    public void checarMinas() {
        boolean checarMinas = true;
        for (Campo minas : listaMinas) {
            if (!(minas.comprobarVisible() || minas.comprobarBandera())) {
                checarMinas = false;
                break;
            }
        }
        limpiarTablero = (FILAS * COLUMNAS) - listaMinas.size();
        if (checarMinas && camposVisibles == limpiarTablero) {
            sala.enviarInfo("INFOMESSAGE Han puesto banderas sobre todas las minas y descubierto todos los campos.");
            this.mostrarPuntos();
        }
    }
    
    private void sacarPuntos() {
        for (Jugador jugador : sala.getLista()) {
            for (Campo banderas : jugador.getBanderas()) {
                if (banderas.comprobarMina()) {
                    jugador.aumentarPuntos();
                } else {
                    jugador.quitarPuntos();
                }
            }
        }
    }
    
    private void sacarPuntaje(){
       Campo campo;
        for (int y = 0; y < COLUMNAS; y++) {
            for (int x = 0; x < FILAS; x++) {
                campo = TABLERO[x][y];
                if (!campo.comprobarMina()) {
                    if (campo.comprobarBandera()) {
                        campo.ponerBanderaIncorrecta();
                        sala.enviarInfo("NOMINA " + x + "," + y);
                        campo.getAdmin().quitarPuntos();
                    } else if (campo.comprobarOculto() || campo.comprobarVisible()) {
                        campo.hacerVisible();
                        sala.enviarInfo("2DESCUBRIRCAMPO " + x + "," + y);
                    }
                } else {
                    if (campo.comprobarBandera()) {
                        campo.getAdmin().aumentarPuntos();
                    } else if (campo.comprobarOculto()) {
                        campo.hacerVisible();
                        sala.enviarInfo("2HAYMINA " + x + "," + y);
                    }
                }
            }
        }
    }

    public void mostrarPuntos() {
        sacarPuntaje();
        inicio = false;
        t.stop();
        Collections.sort(sala.getLista(), (Jugador j1, Jugador j2) -> new Integer(j2.getPuntos()).compareTo(j1.getPuntos()));
        sala.enviarInfo("MESSAGE [Servidor] El juego ha terminado");
        String algo = "";
        for (Jugador jugador : sala.getLista()) {
            algo += "Jugador " + jugador.getID() + ", Nombre: " + jugador.getNombre() + ", Puntos: " + jugador.getPuntos() + ".";
        }
        sala.enviarInfo("PUNTOS " + algo);
        sala.reiniciarDatos();
    }
}