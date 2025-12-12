package main;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.glu.GLU;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot; // Necesario para mover el cursor del mouse
import java.awt.AWTException; 
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Random;

public class Main extends GLCanvas implements GLEventListener, KeyListener, MouseMotionListener {

    private static final String TITULO = "Simulación 3D: Ciclo Día y Noche y Controles Mejorados";
    private static final int FPS = 60;
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    
    // Variables de control de cámara
    private float camX = 0.0f, camY = 1.7f, camZ = 0.0f; 
    private float yaw = 0.0f; 
    private float pitch = 0.0f; 
    private final float moveSpeed = 0.05f; 
    private final float mouseSpeed = 0.15f; 
    private final float turnSpeed = 2.0f; // Velocidad de giro para Q/E

    private Robot robot; // Objeto para reposicionar el cursor
    private boolean mouseWarped = false; // Bandera para evitar el loop infinito
    private Point lastMouse = null;
    
    // Generación de Entorno
    private static final int NUM_TREES = 30;
    private static final int APPLES_PER_TREE = 8;
    private static final int FIELD_SIZE = 40;
    
    // VARIABLES DE CICLO DÍA-NOCHE
    private float sunAngle = 0.0f; 
    private final float sunSpeed = 0.1f; 
    
    // ARREGLOS DE POSICIÓN
    private final float[][] treePositions;
    private final float[][][] applePositions; 
    
    private GLU glu; 
    private Random rand;

    public Main(GLCapabilities capabilities) {
        super(capabilities);
        
        // Intentar inicializar el Robot para mover el cursor
        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.err.println("Advertencia: No se pudo inicializar java.awt.Robot. El mouse warping no funcionará.");
        }
        
        this.treePositions = new float[NUM_TREES][2];
        this.applePositions = new float[NUM_TREES][APPLES_PER_TREE][3];
        this.rand = new Random();

        this.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        this.addGLEventListener(this);
        
        this.addKeyListener(this);
        this.addMouseMotionListener(this);
        this.setFocusable(true);
        
        this.glu = new GLU();
    }
    
    private void generateWorld() {
        for (int i = 0; i < NUM_TREES; i++) {
            treePositions[i][0] = rand.nextFloat() * FIELD_SIZE - (FIELD_SIZE / 2.0f);
            treePositions[i][1] = rand.nextFloat() * FIELD_SIZE - (FIELD_SIZE / 2.0f);
        }
        
        float totalHeight = 3.0f; 
        float baseRadius = 1.5f; 

        for (int i = 0; i < NUM_TREES; i++) {
            for (int j = 0; j < APPLES_PER_TREE; j++) {
                
                float y = rand.nextFloat() * totalHeight * 0.8f; 
                float currentRadiusLimit = baseRadius * (1.0f - y / totalHeight);
                
                float angle = rand.nextFloat() * (float) (2.0 * Math.PI);
                float r = rand.nextFloat() * currentRadiusLimit * 0.8f; 

                float x = (float) (r * Math.cos(angle));
                float z = (float) (r * Math.sin(angle));
                
                applePositions[i][j][0] = x;
                applePositions[i][j][1] = y + 2.0f; 
                applePositions[i][j][2] = z;
            }
        }
    }


    // ============================= GLEventListener Implementación =============================

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        
        generateWorld(); 

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_CULL_FACE); 
        gl.glShadeModel(GL2.GL_SMOOTH); 
        
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_COLOR_MATERIAL); 
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        
        sunAngle = (sunAngle + sunSpeed) % 360.0f;
        updateLightingAndSky(gl);
        
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        
        // Cámara 
        gl.glRotatef(pitch, 1.0f, 0.0f, 0.0f); 
        gl.glRotatef(yaw, 0.0f, 1.0f, 0.0f); 
        gl.glTranslatef(-camX, -camY, -camZ);

        // --- Dibujar la escena ---
        
        dibujarCielo(gl);
        dibujarMontañas(gl); 
        dibujarPlano(gl);
        
        for (int i = 0; i < NUM_TREES; i++) {
            dibujarArbol(gl, treePositions[i][0], treePositions[i][1], applePositions[i]);
        }
    }
    
    private void updateLightingAndSky(GL2 gl) {
        float angleRad = (float) Math.toRadians(sunAngle);
        
        float lightX = 0.0f;
        float lightY = (float) Math.cos(angleRad) * 100.0f;
        float lightZ = (float) Math.sin(angleRad) * 100.0f;
        
        float[] lightPosition = {lightX, lightY, lightZ, 0.0f}; 
        
        float dayFactor = (float) (Math.cos(angleRad) + 1.0) / 2.0f; 
        
        float dr = 0.9f * dayFactor + 0.1f * (1.0f - dayFactor);
        float dg = 0.9f * dayFactor + 0.2f * (1.0f - dayFactor);
        float db = 0.8f * dayFactor + 0.5f * (1.0f - dayFactor);
        float[] diffuseLight = {dr, dg, db, 1.0f};
        
        float ar = 0.5f * dayFactor + 0.1f * (1.0f - dayFactor);
        float ag = 0.5f * dayFactor + 0.1f * (1.0f - dayFactor);
        float ab = 0.5f * dayFactor + 0.3f * (1.0f - dayFactor);
        float[] ambientLight = {ar, ag, ab, 1.0f};

        float skyR = 0.53f * dayFactor + 0.1f * (1.0f - dayFactor);
        float skyG = 0.81f * dayFactor + 0.1f * (1.0f - dayFactor);
        float skyB = 0.92f * dayFactor + 0.3f * (1.0f - dayFactor);
        
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambientLight, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, diffuseLight, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition, 0);
        
        gl.glClearColor(skyR, skyG, skyB, 1.0f);
    }
    
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        if (height == 0) height = 1; 
        
        float aspect = (float)width / height;
        float near = 0.1f;
        float far = 500.0f; 

        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        
        final float tanFov = (float) Math.tan(Math.toRadians(30.0)); 

        float top = near * tanFov;
        float bottom = -top;
        float right = top * aspect;
        float left = -right;
        
        gl.glFrustumf(left, right, bottom, top, near, far);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }
    
    @Override
    public void dispose(GLAutoDrawable drawable) { /* Limpieza */ }


    // ============================= MÉTODOS DE DIBUJO (sin cambios significativos) =============================

    private void dibujarCielo(GL2 gl) {
        gl.glPushMatrix();
        gl.glLoadIdentity(); 
        
        float angleRad = (float) Math.toRadians(sunAngle);
        
        float objY = (float) Math.cos(angleRad) * 150.0f;
        float objZ = (float) Math.sin(angleRad) * 150.0f; 

        gl.glTranslatef(0.0f, objY, objZ); 
        
        gl.glDisable(GL2.GL_LIGHTING);
        
        if (objY > 0) {
             gl.glColor3f(1.0f, 0.8f, 0.0f); // Sol
        } else {
             gl.glColor3f(0.8f, 0.8f, 1.0f); // Luna
        }

        glu.gluSphere(glu.gluNewQuadric(), 10.0, 20, 20); 
        gl.glEnable(GL2.GL_LIGHTING);
        
        gl.glPopMatrix();
    }
    
    private void dibujarMontañas(GL2 gl) {
        gl.glDisable(GL2.GL_LIGHTING);
        
        float[] brown = {0.4f, 0.25f, 0.15f}; 
        float[] snow = {0.9f, 0.9f, 0.95f}; 
        
        float peakHeight = 80.0f;
        float baseLevel = 0.0f; 
        float horizonZ = -300.0f; 

        gl.glBegin(GL2.GL_TRIANGLES);
        
        gl.glColor3fv(brown, 0);
        gl.glVertex3f(-150.0f, baseLevel, horizonZ);
        
        gl.glColor3fv(snow, 0); 
        gl.glVertex3f(-50.0f, peakHeight, horizonZ);
        
        gl.glColor3fv(brown, 0);
        gl.glVertex3f(50.0f, baseLevel, horizonZ);

        gl.glColor3f(brown[0]*0.8f, brown[1]*0.8f, brown[2]*0.8f); 
        gl.glVertex3f(0.0f, baseLevel, horizonZ);
        
        gl.glColor3fv(snow, 0);
        gl.glVertex3f(100.0f, peakHeight*0.75f, horizonZ);
        
        gl.glColor3f(brown[0]*0.8f, brown[1]*0.8f, brown[2]*0.8f);
        gl.glVertex3f(200.0f, baseLevel, horizonZ);

        gl.glEnd();
        
        gl.glEnable(GL2.GL_LIGHTING);
    }

    private void dibujarPlano(GL2 gl) {
        float limit = FIELD_SIZE / 2.0f;
        int patternSize = 4; 

        gl.glNormal3f(0.0f, 1.0f, 0.0f); 
        
        for (int i = (int) -limit; i < limit; i += patternSize) {
            for (int j = (int) -limit; j < limit; j += patternSize) {
                if ((i + j) % (patternSize * 2) == 0) {
                    gl.glColor3f(0.15f, 0.6f, 0.15f); 
                } else {
                    gl.glColor3f(0.1f, 0.5f, 0.1f); 
                }
                
                gl.glBegin(GL2.GL_QUADS);
                    gl.glVertex3f(i, 0.0f, j);
                    gl.glVertex3f(i, 0.0f, j + patternSize);
                    gl.glVertex3f(i + patternSize, 0.0f, j + patternSize);
                    gl.glVertex3f(i + patternSize, 0.0f, j);
                gl.glEnd();
            }
        }
    }
    
    private void dibujarArbol(GL2 gl, float x, float z, float[][] apples) {
        gl.glPushMatrix();
        gl.glTranslatef(x, 0.0f, z); 

        dibujarTronco(gl, 2.0f, 0.2f, 10, 0.45f, 0.2f, 0.0f); 
        
        gl.glTranslatef(0.0f, 2.0f, 0.0f); 
        dibujarFollajeCapas(gl, 3.0f, 1.5f, 10); 
        
        dibujarManzanas(gl, apples);
        
        gl.glPopMatrix();
    }
    
    private void dibujarManzanas(GL2 gl, float[][] apples) {
        gl.glColor3f(1.0f, 0.0f, 0.0f); 
        float appleRadius = 0.15f; 
        
        for (int i = 0; i < apples.length; i++) {
            gl.glPushMatrix();
            
            gl.glTranslatef(apples[i][0], apples[i][1] - 2.0f, apples[i][2]); 
            
            glu.gluSphere(glu.gluNewQuadric(), appleRadius, 10, 10); 
            
            gl.glPopMatrix();
        }
    }

    private void dibujarTronco(GL2 gl, float height, float radius, int segments, float r, float g, float b) {
        gl.glColor3f(r, g, b); 
        float segmentAngle = (float) (2.0 * Math.PI / segments);
        
        for (int i = 0; i < segments; i++) {
            float angle1 = i * segmentAngle;
            float angle2 = (i + 1) * segmentAngle;
            
            float x1 = (float) (radius * Math.cos(angle1));
            float z1 = (float) (radius * Math.sin(angle1));
            float x2 = (float) (radius * Math.cos(angle2));
            float z2 = (float) (radius * Math.sin(angle2));
            
            gl.glBegin(GL2.GL_QUADS);
                float normalX = (x1 + x2) / 2.0f;
                float normalZ = (z1 + z2) / 2.0f;
                float normalLength = (float) Math.sqrt(normalX * normalX + normalZ * normalZ);
                gl.glNormal3f(normalX / normalLength, 0.0f, normalZ / normalLength);

                gl.glVertex3f(x1, 0.0f, z1); 
                gl.glVertex3f(x2, 0.0f, z2); 
                gl.glVertex3f(x2, height, z2); 
                gl.glVertex3f(x1, height, z1); 
            gl.glEnd();
        }
    }
    
    private void dibujarFollajeCapas(GL2 gl, float totalHeight, float baseRadius, int segments) {
        gl.glColor3f(0.1f, 0.5f, 0.2f); 
        
        int numLayers = 3;
        float layerHeight = totalHeight / numLayers;
        
        for (int j = 0; j < numLayers; j++) {
            gl.glPushMatrix();
            
            gl.glTranslatef(0.0f, j * layerHeight * 0.7f, 0.0f); 
            
            float currentRadius = baseRadius * (1.0f - (j * 0.2f)); 
            
            dibujarCono(gl, layerHeight * 1.5f, currentRadius, segments);
            
            gl.glPopMatrix();
        }
    }
    
    private void dibujarCono(GL2 gl, float height, float radius, int segments) {
        float segmentAngle = (float) (2.0 * Math.PI / segments);
        float topY = height;
        
        for (int i = 0; i < segments; i++) {
            float angle1 = i * segmentAngle;
            float angle2 = (i + 1) * segmentAngle;
            
            float x1 = (float) (radius * Math.cos(angle1));
            float z1 = (float) (radius * Math.sin(angle1));
            float x2 = (float) (radius * Math.cos(angle2));
            float z2 = (float) (radius * Math.sin(angle2));
            
            gl.glBegin(GL2.GL_TRIANGLES);
                float normalX = (x1 + x2) / 2.0f;
                float normalZ = (z1 + z2) / 2.0f;
                float normalLength = (float) Math.sqrt(normalX * normalX + normalZ * normalZ);
                gl.glNormal3f(normalX / normalLength, 0.5f, normalZ / normalLength);

                gl.glVertex3f(0.0f, topY, 0.0f); 
                gl.glVertex3f(x1, 0.0f, z1); 
                gl.glVertex3f(x2, 0.0f, z2); 
            gl.glEnd();
            
            gl.glBegin(GL2.GL_TRIANGLES);
                gl.glNormal3f(0.0f, -1.0f, 0.0f); 

                gl.glVertex3f(0.0f, 0.0f, 0.0f); 
                gl.glVertex3f(x1, 0.0f, z1); 
                gl.glVertex3f(x2, 0.0f, z2); 
            gl.glEnd();
        }
    }

    // ============================= Input Implementación MEJORADA =============================
    
    @Override
    public void keyPressed(KeyEvent e) {
        float dx = 0.0f;
        float dz = 0.0f;
        
        float yawRad = (float) Math.toRadians(yaw);

        // Movimiento (W, S, A, D)
        if (e.getKeyCode() == KeyEvent.VK_W) {
            dx = (float) (moveSpeed * Math.sin(yawRad));
            dz = (float) (-moveSpeed * Math.cos(yawRad));
        } 
        else if (e.getKeyCode() == KeyEvent.VK_S) {
            dx = (float) (-moveSpeed * Math.sin(yawRad));
            dz = (float) (moveSpeed * Math.cos(yawRad));
        }
        else if (e.getKeyCode() == KeyEvent.VK_A) { 
            dx = (float) (-moveSpeed * Math.cos(yawRad));
            dz = (float) (-moveSpeed * Math.sin(yawRad));
        }
        else if (e.getKeyCode() == KeyEvent.VK_D) { 
            dx = (float) (moveSpeed * Math.cos(yawRad));
            dz = (float) (moveSpeed * Math.sin(yawRad));
        }
        // Giro con teclado (Q y E)
        else if (e.getKeyCode() == KeyEvent.VK_Q) {
            yaw -= turnSpeed; // Girar a la izquierda
        }
        else if (e.getKeyCode() == KeyEvent.VK_E) {
            yaw += turnSpeed; // Girar a la derecha
        }
        // Elevación/Descenso
        else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            camY += moveSpeed;
        }
        else if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            camY -= moveSpeed;
            if (camY < 0.1f) camY = 0.1f;
        }

        camX += dx;
        camZ += dz;
    }

    @Override
    public void keyReleased(KeyEvent e) { /* No usado */ }
    
    @Override
    public void keyTyped(KeyEvent e) { /* No usado */ }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (robot == null) return; // Si Robot no se pudo inicializar, salimos

        // 1. Evitar procesamiento de eventos de mouse que nosotros mismos generamos
        if (mouseWarped) {
            mouseWarped = false;
            return;
        }
        
        if (lastMouse == null) {
            lastMouse = e.getPoint();
            return;
        }

        // 2. Calcular diferencia de movimiento
        float deltaX = (float) (e.getX() - lastMouse.getX());
        float deltaY = (float) (e.getY() - lastMouse.getY());

        // 3. Aplicar rotación
        yaw += deltaX * mouseSpeed; 
        pitch += deltaY * mouseSpeed; 

        // 4. Limitar Pitch (vertical)
        if (pitch > 90.0f) pitch = 90.0f;
        if (pitch < -90.0f) pitch = -90.0f;

        // 5. Reposicionar el cursor (Mouse Warping)
        
        // Coordenadas absolutas del centro de la ventana en la pantalla
        Point centerScreen = getLocationOnScreen();
        centerScreen.translate(getWidth() / 2, getHeight() / 2);

        // Si el cursor se ha movido significativamente, lo recentramos
        if (Math.abs(deltaX) > 1 || Math.abs(deltaY) > 1) {
             mouseWarped = true; // Establecer la bandera para el siguiente evento
             robot.mouseMove(centerScreen.x, centerScreen.y);
             
             // Actualizar lastMouse al centro de la ventana
             lastMouse = new Point(getWidth() / 2, getHeight() / 2);
        } else {
             // Si el movimiento es pequeño, simplemente actualizamos lastMouse
             lastMouse = e.getPoint();
        }
    }


    // ============================= Main (Punto de Entrada) =============================

    public static void main(String[] args) {
        GLProfile glp = null;
        try {
            glp = GLProfile.get(GLProfile.GL2);
        } catch (com.jogamp.opengl.GLException e) {
            try {
                 glp = GLProfile.get(GLProfile.GLES1);
            } catch (com.jogamp.opengl.GLException e2) {
                System.err.println("Error fatal: No se pudo obtener ningún perfil OpenGL compatible.");
                System.exit(1);
            }
        }
        
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setDepthBits(16);
        caps.setSampleBuffers(false); 

        Main canvas = new Main(caps);

        JFrame frame = new JFrame(TITULO);
        frame.getContentPane().add(canvas);
        
        final FPSAnimator animator = new FPSAnimator(canvas, FPS, true);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        frame.pack(); 
        frame.setLocationRelativeTo(null); 
        frame.setVisible(true);
        frame.setResizable(true); 

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (animator.isStarted())
                    animator.stop();
                System.exit(0);
            }
        });

        animator.start();
    }
}
