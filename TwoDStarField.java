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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

public class TwoDStarField extends GLCanvas implements GLEventListener, KeyListener {

    private static final String TITULO = "Trabajo Final de Gráficos: Campo Estelar 2D";
    private static final int FPS = 60;
    private static final int WORLD_SIZE = 100; // Espacio 2D virtual 100x100
    
    // Variables del jugador
    private float playerX;
    private float playerY;
    private final float playerSpeed = 0.5f; 
    
    // Variables del campo de estrellas
    private static final int STAR_COUNT = 300;
    private final float[][] starPositions = new float[STAR_COUNT][3]; // x, y, velocidad
    private final float starMinSpeed = 0.1f;
    private final float starMaxSpeed = 0.8f;
    
    private GLU glu; 
    private Random rand;

    public TwoDStarField(GLCapabilities capabilities) {
        super(capabilities);
        
        this.rand = new Random();
        this.playerX = WORLD_SIZE / 2.0f;
        this.playerY = 10.0f; 
        
        this.setPreferredSize(new Dimension(800, 600));
        this.addGLEventListener(this);
        
        this.addKeyListener(this);
        this.setFocusable(true);
        
        this.glu = new GLU();
    }
    
    private void generateStarField() {
        for (int i = 0; i < STAR_COUNT; i++) {
            starPositions[i][0] = rand.nextFloat() * WORLD_SIZE; 
            starPositions[i][1] = rand.nextFloat() * WORLD_SIZE; 
            starPositions[i][2] = rand.nextFloat() * (starMaxSpeed - starMinSpeed) + starMinSpeed;
        }
    }


    // ============================= GLEventListener Implementación =============================

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        
        generateStarField();

        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glDisable(GL2.GL_LIGHTING);
        
        gl.glClearColor(0.0f, 0.0f, 0.1f, 1.0f); // Fondo Azul Oscuro (Espacio)
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        // --- CORRECCIÓN CRÍTICA: Usar getGL2() para obtener el contexto ---
        GL2 gl = drawable.getGL().getGL2();
        
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        gl.glLoadIdentity();
        
        // --- Animación y Dibujo ---
        updateStarField();
        dibujarEstrellas(gl);
        dibujarJugador(gl);
    }
    
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        
        // --- PROYECCIÓN ORTOGRÁFICA 2D ---
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        
        // Mapea la ventana a un sistema de coordenadas de 0 a 100
        glu.gluOrtho2D(0.0, WORLD_SIZE, 0.0, WORLD_SIZE);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }
    
    @Override
    public void dispose(GLAutoDrawable drawable) { /* Limpieza */ }


    // ============================= MÉTODOS DE ANIMACIÓN Y DIBUJO =============================

    private void updateStarField() {
        for (int i = 0; i < STAR_COUNT; i++) {
            // Mover la estrella hacia abajo (simulando que la nave avanza)
            starPositions[i][1] -= starPositions[i][2]; 
            
            // Si la estrella sale por la parte inferior, resetearla en la parte superior
            if (starPositions[i][1] < 0.0f) {
                starPositions[i][1] = WORLD_SIZE;
                starPositions[i][0] = rand.nextFloat() * WORLD_SIZE;
            }
        }
    }

    private void dibujarEstrellas(GL2 gl) {
        gl.glPointSize(2.0f); 

        gl.glBegin(GL2.GL_POINTS);
        
        for (int i = 0; i < STAR_COUNT; i++) {
            float speed = starPositions[i][2];
            
            // Color más brillante para estrellas más rápidas (simula cercanía/profundidad)
            float brightness = (speed - starMinSpeed) / (starMaxSpeed - starMinSpeed); 
            float color = 0.5f + 0.5f * brightness; 
            
            gl.glColor3f(color, color, color);
            gl.glVertex2f(starPositions[i][0], starPositions[i][1]);
        }
        
        gl.glEnd();
    }
    
    private void dibujarJugador(GL2 gl) {
        gl.glPushMatrix();
        gl.glTranslatef(playerX, playerY, 0.0f); 

        // Color del jugador (Azul/Cian brillante)
        gl.glColor3f(0.0f, 0.7f, 1.0f); 
        
        float size = 4.0f;

        gl.glBegin(GL2.GL_TRIANGLES);
            // Pico superior
            gl.glVertex2f(0.0f, size / 2.0f); 
            // Base izquierda
            gl.glVertex2f(-size / 2.0f, -size / 2.0f);
            // Base derecha
            gl.glVertex2f(size / 2.0f, -size / 2.0f); 
        gl.glEnd();

        gl.glPopMatrix();
    }

    // ============================= Input Implementación =============================
    
    @Override
    public void keyPressed(KeyEvent e) {
        float newX = playerX;
        float newY = playerY;

        if (e.getKeyCode() == KeyEvent.VK_W) {
            newY += playerSpeed;
        } 
        else if (e.getKeyCode() == KeyEvent.VK_S) {
            newY -= playerSpeed;
        }
        else if (e.getKeyCode() == KeyEvent.VK_A) { 
            newX -= playerSpeed;
        }
        else if (e.getKeyCode() == KeyEvent.VK_D) { 
            newX += playerSpeed;
        }
        
        // Limitar movimiento dentro del WORLD_SIZE (100x100)
        float margin = 2.0f;
        playerX = Math.max(margin, Math.min(newX, WORLD_SIZE - margin));
        playerY = Math.max(margin, Math.min(newY, WORLD_SIZE - margin));
    }

    @Override
    public void keyReleased(KeyEvent e) { /* No usado */ }
    
    @Override
    public void keyTyped(KeyEvent e) { /* No usado */ }

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

        TwoDStarField canvas = new TwoDStarField(caps);

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
