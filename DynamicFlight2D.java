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

public class DynamicFlight2D extends GLCanvas implements GLEventListener, KeyListener {

    private static final String TITULO = "Trabajo Final: Vuelo 2D con Ciclo Atmosférico";
    private static final int FPS = 60;
    private static final int WORLD_SIZE = 100; // Espacio 2D virtual 100x100
    
    // Variables del jugador
    private float playerX;
    private float playerY;
    private final float playerSpeed = 1.0f; // Aumentamos la velocidad para que se sienta más rápido
    
    // Variables del campo de estrellas
    private static final int STAR_COUNT = 300;
    private final float[][] starPositions = new float[STAR_COUNT][3]; // x, y, velocidad
    private final float starMinSpeed = 0.5f;
    private final float starMaxSpeed = 1.5f;
    
    // Variables del ciclo de Amanecer/Anochecer
    private float sunAngle = 270.0f; // Empezamos de noche (Luna alta)
    private final float sunCycleSpeed = 0.05f; // Velocidad de la transición
    
    private GLU glu; 
    private Random rand;

    public DynamicFlight2D(GLCapabilities capabilities) {
        super(capabilities);
        
        this.rand = new Random();
        this.playerX = WORLD_SIZE / 2.0f;
        this.playerY = WORLD_SIZE / 2.0f; // Empezamos en el centro
        
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
            // La velocidad simula la profundidad y la sensación de avance
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
        gl.glEnable(GL2.GL_POINT_SMOOTH); 
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        
        // 1. ACTUALIZAR CICLO DÍA/NOCHE
        sunAngle = (sunAngle + sunCycleSpeed) % 360.0f;
        float dayFactor = updateColors(gl); // Obtener factor para las estrellas
        
        gl.glLoadIdentity();
        
        // 2. ANIMACIÓN Y DIBUJO
        updateStarField();
        dibujarSolLuna(gl);
        dibujarEstrellas(gl, dayFactor);
        dibujarJugador(gl);
    }
    
    /**
     * Actualiza el color de fondo (Cielo) y devuelve el factor de iluminación.
     * @return float Factor de 0.0 (noche profunda) a 1.0 (día brillante).
     */
    private float updateColors(GL2 gl) {
        float angleRad = (float) Math.toRadians(sunAngle);
        // El factor 0.0 (noche) a 1.0 (día) se basa en la altura del sol (coseno)
        float dayFactor = (float) (Math.cos(angleRad) + 1.0) / 2.0f; 
        
        float r, g, b;
        
        // --- Interpolación de Color del Cielo ---
        // Noche Profunda (0.0, 0.0, 0.1) -> Amanecer (0.8, 0.4, 0.1) -> Día (0.3, 0.7, 1.0)
        
        if (dayFactor < 0.5f) { 
            // Noche a Amanecer
            float f = dayFactor * 2.0f; 
            r = 0.0f * (1.0f - f) + 0.8f * f;
            g = 0.0f * (1.0f - f) + 0.4f * f;
            b = 0.1f * (1.0f - f) + 0.1f * f;
        } else { 
            // Amanecer a Día
            float f = (dayFactor - 0.5f) * 2.0f; 
            r = 0.8f * (1.0f - f) + 0.3f * f;
            g = 0.4f * (1.0f - f) + 0.7f * f;
            b = 0.1f * (1.0f - f) + 1.0f * f;
        }

        gl.glClearColor(r, g, b, 1.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        
        return dayFactor;
    }
    
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        
        glu.gluOrtho2D(0.0, WORLD_SIZE, 0.0, WORLD_SIZE);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }
    
    @Override
    public void dispose(GLAutoDrawable drawable) { /* Limpieza */ }


    // ============================= MÉTODOS DE ANIMACIÓN Y DIBUJO =============================

    private void updateStarField() {
        // La velocidad de las estrellas está ligada a su variable [2]
        for (int i = 0; i < STAR_COUNT; i++) {
            // Mover la estrella hacia abajo (simulando avance)
            starPositions[i][1] -= starPositions[i][2]; 
            
            // Si la estrella sale por la parte inferior, resetearla
            if (starPositions[i][1] < 0.0f) {
                starPositions[i][1] = WORLD_SIZE;
                starPositions[i][0] = rand.nextFloat() * WORLD_SIZE;
            }
        }
    }

    private void dibujarSolLuna(GL2 gl) {
        float angleRad = (float) Math.toRadians(sunAngle);
        float radius = 8.0f;
        float orbitRadius = WORLD_SIZE * 0.9f;
        
        // Calcula la posición circular del Sol/Luna alrededor del centro de la pantalla
        float objX = WORLD_SIZE / 2.0f + (float) (orbitRadius * Math.sin(angleRad)); 
        float objY = WORLD_SIZE / 2.0f + (float) (orbitRadius * Math.cos(angleRad)); 
        
        gl.glPushMatrix();
        gl.glTranslatef(objX, objY, 0.0f);
        
        // Determina si es Sol o Luna basado en la altura
        if (objY > WORLD_SIZE / 2.0f) {
             // Sol (Amarillo brillante)
             gl.glColor3f(1.0f, 0.9f, 0.6f); 
        } else {
             // Luna (Blanco/Azulado, simulando el reflejo)
             gl.glColor3f(0.8f, 0.8f, 1.0f); 
        }

        // Dibujar círculo simple (Polígono de muchos lados)
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
            gl.glVertex2f(0.0f, 0.0f); // Centro
            int segments = 50;
            for (int i = 0; i <= segments; i++) {
                float currentAngle = (float) (2.0 * Math.PI * i / segments);
                gl.glVertex2f((float) Math.cos(currentAngle) * radius, 
                              (float) Math.sin(currentAngle) * radius);
            }
        gl.glEnd();
        
        gl.glPopMatrix();
    }

    private void dibujarEstrellas(GL2 gl, float dayFactor) {
        // La opacidad de las estrellas disminuye a medida que el día avanza (dayFactor -> 1.0)
        float starAlpha = 1.0f - dayFactor; 
        
        // Multiplicamos por 4 para acelerar el desvanecimiento y que desaparezcan completamente rápido.
        starAlpha = Math.max(0.0f, starAlpha * 4.0f); 
        
        if (starAlpha < 0.01f) return; 

        gl.glPointSize(2.0f); 
        
        gl.glBegin(GL2.GL_POINTS);
        
        for (int i = 0; i < STAR_COUNT; i++) {
            float speed = starPositions[i][2];
            
            // Brillo base: estrellas más rápidas/cercanas son más brillantes
            float baseBrightness = (speed - starMinSpeed) / (starMaxSpeed - starMinSpeed) * 0.5f + 0.5f;
            
            // Aplicar el desvanecimiento de la noche (starAlpha)
            float finalColor = baseBrightness * starAlpha;
            
            gl.glColor3f(finalColor, finalColor, finalColor);
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

        // Triángulo que simula una nave o puntero
        gl.glBegin(GL2.GL_TRIANGLES);
            gl.glVertex2f(0.0f, size); 
            gl.glVertex2f(-size / 2.0f, 0.0f);
            gl.glVertex2f(size / 2.0f, 0.0f); 
        gl.glEnd();

        gl.glPopMatrix();
    }

    // ============================= Input Implementación =============================
    
    @Override
    public void keyPressed(KeyEvent e) {
        float newX = playerX;
        float newY = playerY;

        // El movimiento ahora es más dinámico (W, S, A, D)
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
        
        // Limitar movimiento dentro de la pantalla (con un pequeño margen)
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

        // Usar la nueva clase DynamicFlight2D
        DynamicFlight2D canvas = new DynamicFlight2D(caps);

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
