package com.embeddedProject.jpct_ae_test


import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import java.lang.Exception

import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.EGLConfigChooser
import android.os.Bundle
import android.view.MotionEvent

import android.widget.LinearLayout
import android.widget.TextView


import androidx.core.content.res.ResourcesCompat
import androidx.appcompat.app.AppCompatActivity

import com.threed.jpct.*
import com.threed.jpct.util.BitmapHelper
import com.threed.jpct.util.MemoryHelper


//TODO Girare lo schermo fa crashare
class MainActivity : AppCompatActivity() {

    //Variabili iniziali

    var fb: FrameBuffer? = null
    var world: World? = null        //Mondo in cui sono istanziati gli oggetti
    var back = RGBColor(50, 50, 100)    //Colore dello sfondo

    private var mGLView: GLSurfaceView? = null      //View dell'openGl
    private var renderer: MainActivity.MyRenderer? = null       //Renderer dell'openGL


    private var touchTurn = 0f      //Variabili per i tocchi
    private var touchTurnUp = 0f
    private var xpos = -1f
    private var ypos = -1f

    //Variabili
    var cube: Object3D? = null
    var pyramide: Object3D? = null
    //Fps
    var fps = 0
    var FpsText : TextView? = null
    //Sole
    private var sun: Light? = null



    override fun onCreate(savedInstanceState: Bundle?) {

        //Inizializza la View dell'openGL
        mGLView = GLSurfaceView(this)

        //Impostazioni dell'Engine dell'OpenGl
        mGLView!!.setEGLConfigChooser(EGLConfigChooser { egl, display ->

            // Ensure that we get a 16bit framebuffer. Otherwise, we'll fall
            // back to Pixelflinger on some device (read: Samsung I7500)
            val attributes = intArrayOf(EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE)
            val configs = arrayOfNulls<EGLConfig>(1)
            val result = IntArray(1)
            egl.eglChooseConfig(display, attributes, configs, 1, result)
            configs[0]!!
        })

        //Renderer
        renderer = MyRenderer()
        mGLView!!.setRenderer(renderer)


        super.onCreate(savedInstanceState)

        //Crea le view
        setContentView(R.layout.activity_main)

        //Imposto la view dell'openGL nel primo posto del linearLayout
        val l = findViewById<LinearLayout>(R.id.linearL)
        l.addView(mGLView,0)

        FpsText = findViewById(R.id.FpsTextView)



    }

    override fun onPause() {
        super.onPause()
        mGLView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        mGLView!!.onResume()
    }

    override fun onStop() {
        super.onStop()

    }

    //
    override fun onTouchEvent(me :MotionEvent): Boolean {

        //Viene premuto lo schermo e mi salvo le coordinate del dito sullo schermo
        if (me.action == MotionEvent.ACTION_DOWN) {
            xpos = me.x
            ypos = me.y
            return true
        }

        //Viene sollevato il dito dallo schermo e resetto le coordinate
        if (me.action == MotionEvent.ACTION_UP) {
            xpos = -1f
            ypos = -1f
            touchTurn = 0f
            touchTurnUp = 0f
            return true
        }

        //Viene trascinato il dito sullo schermo e calcolo lo spostamento
        if (me.action == MotionEvent.ACTION_MOVE) {
            val xd = me.x - xpos
            val yd = me.y - ypos
            xpos = me.x
            ypos = me.y
            touchTurn = xd / -100f
            touchTurnUp = yd / -100f
            return true
        }

        //Sinceramente non lo so
         try {
             Thread.sleep(15)
         } catch (e: Exception) {
             // No need for this...
         }

         return super.onTouchEvent(me)

    }

    private fun isFullscreenOpaque(): Boolean {
        return true
    }

    //Classe del renderer cioè colui che si occupa di creare l'ambiente della view dell'openGl
    inner class MyRenderer : GLSurfaceView.Renderer {

        //Variabile del tempo per contare gli fps
        private var time = System.currentTimeMillis()

        //Da mettere per forza perchè GLSurfaceView.Renderer ha 3 funzioni da implementare
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig) {

        }

        //Funzione in cui si preparano gli oggetti3d, il mondo e la luce...
        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {

            fb?.dispose()
            fb = FrameBuffer(gl,width,height)


            //Inizializzo il mondo(genera lo spazio in 3d)
            world = World()

            //TODO usare i dati di ArCore per la luce
            //Imposto la luce base del mondo(cioè una luce di base che non genera ombre)
            world!!.setAmbientLight(20, 20, 20)

            //Creo una nuova luce e la aggiungo nel mondo
            sun = Light(world)

            //TODO usare i dati di ArCore per il sole Forse
            //Intensità e colore della luce
            sun!!.setIntensity(250f, 250f, 250f)


            // Creo una texture partendo da un bottone :). Si può usare anche un immagine
            //La texture è l'immagine che viene appiccicata sopra il cubo dandogli colore
            //TODO privare come texture un immagine
            val texture = Texture(
                BitmapHelper.rescale(
                    BitmapHelper.convert(ResourcesCompat.getDrawable(resources, android.R.drawable.btn_default,null)),
                    64,
                    64
                )
            )
            //Aggiungo la texture appena creata tra quelle disponibili e gli do un nome
            TextureManager.getInstance().addTexture("texture", texture)

            //Creo un cubo ed una piramide e gli scalo di 10
            cube = Primitives.getCube(10f)
            pyramide = Primitives.getPyramide(10f,0.5f)

            //Genero il cubo aggiungendo la texture e faccio la build
            if(cube != null){
                cube!!.calcTextureWrapSpherical()
                cube!!.setTexture("texture")
                cube!!.strip()
                cube!!.build()
            }

            //Stessa cosa per la piramide
            if(pyramide != null){
                pyramide!!.calcTextureWrapSpherical()
                pyramide!!.setTexture("texture")
                pyramide!!.strip()
                pyramide!!.build()
                //La sposto in alto
                pyramide!!.translate(0f,-15f,0f)
                //Imposto come padre della piramide il cubo così saranno copiati tutti i movimenti
                pyramide!!.addParent(cube)

            }

            //Aggiungo i due oggetti al modo
            world!!.addObject(cube)
            world!!.addObject(pyramide)

            //TODO La telecamera sarà mossa da ARCore
            //Mi seleziono la telecamera
            val cam: Camera = world!!.camera
            //La muovo fuori dal cubo ad una velocità 50
            cam.moveCamera(Camera.CAMERA_MOVEOUT, 50f)
            //Imposto la telecamera che guardi il cubo
            cam.lookAt(cube!!.transformedCenter)

            //Qui si ricava un vettore di dove è posizionato il cubo...
            val sv = SimpleVector()
            sv.set(cube!!.transformedCenter)
            //e si muove 100 unità lungo l'asse y e z
            sv.y -= 100f
            sv.z -= 100f
            //e posiziona il sole in quel punto
            sun!!.position = sv

            //Comando utile per risparmiare la memoria
            MemoryHelper.compact()

        }

        //Funzione che gestisce i cambiamenti degli oggetti come rotazione e traslazione ecc.
        override fun onDrawFrame(gl: GL10?) {

            //Ruoto in orizzontale il cubo e la piramide in base agli input che ho raccolto
            if (touchTurn != 0f) {
                cube!!.rotateY(touchTurn)
                //pyramide!!.rotateY(touchTurn)
                touchTurn = 0f
            }

            //Ruoto in verticale il cubo e la piramide in base agli input che ho raccolto
            if (touchTurnUp != 0f) {
                cube!!.rotateX(touchTurnUp)
                //pyramide!!.rotateX(touchTurnUp)
                touchTurnUp = 0f
            }

            //Pulisco il buffer con il colore dello sfondo
            fb!!.clear(back)
            //Aggiorno il mondo con i nuovi cambiamenti
            world!!.renderScene(fb)
            world!!.draw(fb)
            fb!!.display()

            //Calcolo gli Fps
            if (System.currentTimeMillis() - time >= 1000) {
                Logger.log(fps.toString() + "fps")
                FpsText?.text = fps.toString()

                fps = 0
                time = System.currentTimeMillis()
            }
            //Boh...
            fps++
        }

    }

}
