package minusk.photon.image;

import minusk.photon.control.MainController;
import minusk.photon.tools.Pencil;
import minusk.photon.tools.Tool;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.system.jemalloc.JEmalloc.*;

/**
 * Created by MinusKelvin on 17/09/15.
 */
public class Document {
	private static int tex1, tex2, changetex, chgstenciltex, changeMultisample, displaytex, finalTex,
			alphashader, imageshader, positionsbuffer, fbo, lastWidth, lastHeight, offsetLoc;
	private static FloatBuffer positions, clearcolor;
	
	private int width, height;
	public float x, y, zoom=1;
	private int currentLayer = 0;
	private boolean flush = true;
	/** Layer list, stored bottom to top */
	private ArrayList<Layer> layers = new ArrayList<>();
	private ByteBuffer change, changeStencil;
	private BlendMode changeBlendMode;
	private int changing = 0;
	public Tool currentTool = new Pencil();
	
	public Document(int width, int height) {
		this.width = width;
		this.height = height;
		x = width/2f;
		y = height/2f;
		layers.add(new Layer(this));
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public Layer getLayer(int i) {
		return layers.get(i);
	}
	
	public Layer getWorkingLayer() {
		return layers.get(currentLayer);
	}
	
	/**
	 * Tells this Document that it should rebuild it's cached copy of the final image next time it's rendered.
	 * Should be called if any part of the image has been changed.
	 */
	public void flush() {
		flush = true;
	}
	
	/**
	 * Start a change. Possible <code>type</code>s:
	 * <table>
	 *     <tr><td>1</td><td>Software buffer</td></tr>
	 *     <tr><td>2</td><td>GPU-side texture</td></tr>
	 *     <tr><td>3</td><td>GPU-side multisample texture</td></tr>
	 * </table>
	 */
	public void initializeChange(BlendMode blend, int type) {
		if (changing != 0)
			throw new IllegalStateException("initialize change while changing");
		if (type < 1 || type > 3)
			throw new IllegalArgumentException("type must be 1, 2, or 3");
		
		changing = type;
		changeBlendMode = blend;
		
		switch (type) {
		case 1:
			change = je_calloc(width * height, 4);
			changeStencil = je_calloc(width * height, 1);
			break;
		case 2:
			break;
		case 3:
			break;
		}
	}
	
	public ByteBuffer getChangeBuffer() {
		return change;
	}
	
	public ByteBuffer getChangeStencil() {
		return changeStencil;
	}
	
	/** Sets the current change's blend mode and calls {@link #flush}. */
	public void setChangeBlendMode(BlendMode blend) {
		changeBlendMode = blend;
		flush();
	}
	
	/** Renders the change and copies the result onto the current layer. */
	public void finalizeChange() {
		if (changing == 0)
			throw new IllegalStateException("can't finalize change when not changing");
		
		glBindTexture(GL_TEXTURE_2D, changetex);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, change);
		glBindTexture(GL_TEXTURE_2D, tex1);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, getWorkingLayer().colorData);
		glBindTexture(GL_TEXTURE_2D, tex2);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
		changeBlendMode.blend(width, height, changetex, tex1, tex2);
		glBindTexture(GL_TEXTURE_2D, tex2);
		glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, getWorkingLayer().colorData);
		
		je_free(change);
		changing = 0;
		
		flush();
	}
	
	public void render() {
		if (flush) {
			boolean usingDisplay = layers.size() % 2 == 1;
			if (currentLayer == 0 && changing != 0)
				usingDisplay = false;
			
			if (usingDisplay) {
				glBindTexture(GL_TEXTURE_2D, displaytex);
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, layers.get(0).colorData);
				glBindTexture(GL_TEXTURE_2D, tex2);
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
			} else {
				glBindTexture(GL_TEXTURE_2D, displaytex);
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
				glBindTexture(GL_TEXTURE_2D, tex2);
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, layers.get(0).colorData);
			}
			glBindTexture(GL_TEXTURE_2D, tex1);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
			
			if (changing != 0) {
				glBindTexture(GL_TEXTURE_2D, changetex);
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, change);
				if (currentLayer == 0) {
					if (usingDisplay)
						changeBlendMode.blend(width, height, changetex, displaytex, tex2);
					else
						changeBlendMode.blend(width, height, changetex, tex2, displaytex);
					usingDisplay ^= true;
				}
			}
			
			for (int i = 1; i < layers.size(); i++) {
				if (currentLayer == i && changing != 0) {
					if (usingDisplay)
						glBindTexture(GL_TEXTURE_2D, tex2);
					else
						glBindTexture(GL_TEXTURE_2D, displaytex);
					glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, layers.get(i).colorData);
					if (usingDisplay)
						changeBlendMode.blend(width, height, changetex, tex2, tex1);
					else
						changeBlendMode.blend(width, height, changetex, displaytex, tex1);
				} else {
					glBindTexture(GL_TEXTURE_2D, tex1);
					glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, layers.get(i).colorData);
				}
				
				if (usingDisplay)
					layers.get(i).getBlendMode().blend(width, height, tex1, displaytex, tex2);
				else
					layers.get(i).getBlendMode().blend(width, height, tex1, tex2, displaytex);
				
				usingDisplay ^= true;
			}
			
			flush = false;
		}
		
		int documentAreaHeight = MainController.get().getDisplayHeight();
		int documentAreaWidth = MainController.get().getDisplayWidth();
		
		glBindTexture(GL_TEXTURE_2D, finalTex);
		if (documentAreaWidth != lastWidth || documentAreaHeight != lastHeight)
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, documentAreaWidth, documentAreaHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
		lastHeight = documentAreaHeight;
		lastWidth = documentAreaWidth;
		glBindFramebuffer(GL_FRAMEBUFFER, fbo);
		glViewport(0,0,documentAreaWidth,documentAreaHeight);
		glClearBufferfv(GL_COLOR, 0, clearcolor);
		
		glBindBuffer(GL_ARRAY_BUFFER, positionsbuffer);
		
		x = Math.max(Math.min(x,width),0);
		y = Math.max(Math.min(y,height),0);
		zoom = Math.max(Math.min(zoom,64),0.01f);
		
		positions.put(-x*zoom / documentAreaWidth * 2);
		positions.put(-y*zoom / documentAreaHeight * 2);
		
		positions.put((width-x)*zoom / documentAreaWidth * 2);
		positions.put(-y*zoom / documentAreaHeight * 2);
		
		positions.put(-x*zoom / documentAreaWidth * 2);
		positions.put((height-y)*zoom / documentAreaHeight * 2);
		
		positions.put((width - x)*zoom / documentAreaWidth * 2);
		positions.put((height - y)*zoom / documentAreaHeight * 2);
		
		positions.position(0);
		glBufferSubData(GL_ARRAY_BUFFER, 0, positions);
		
		glUseProgram(alphashader);
		glUniform2f(offsetLoc, x*zoom/12, y*zoom/12);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(0);
		
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		
		glUseProgram(imageshader);
		glBindTexture(GL_TEXTURE_2D, displaytex);
		
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		
		glDisable(GL_BLEND);
	}
	
	public double toImageX(double docAreaX) {
		return (docAreaX - MainController.get().getDisplayWidth()/2) / zoom + x;
	}
	
	public double toImageY(double docAreaY) {
		return (MainController.get().getDisplayHeight()/2 - docAreaY) / zoom + y;
	}
	
	public static void initialize() {
		tex1 = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, tex1);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		
		tex2 = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, tex2);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		
		displaytex = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, displaytex);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		
		changetex = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, changetex);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		
		// Alpha background shader
		int vert = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vert, "#version 330 core\n" +
				"layout(location = 0) in vec2 pos;" +
				"void main() {" +
				"gl_Position = vec4(pos, 0.0, 1.0);" +
				"}");
		glCompileShader(vert);
		if (glGetShaderi(vert, GL_COMPILE_STATUS) != GL_TRUE)
			throw new IllegalStateException(glGetShaderInfoLog(vert));
		
		int frag = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(frag, "#version 330 core\n" +
				"uniform vec2 offset;" +
				"out vec4 color;" +
				"void main() {" +
				"vec2 v = mod(vec2(ivec2(gl_FragCoord.xy / 8.0 + offset)), 2.0);" +
				"color = vec4(vec3(1.0/3.0 * mod(v.x+v.y, 2.0) + 1.0/3.0),1.0);" +
				"}");
		glCompileShader(frag);
		if (glGetShaderi(frag, GL_COMPILE_STATUS) != GL_TRUE)
			throw new IllegalStateException(glGetShaderInfoLog(frag));
		
		alphashader = glCreateProgram();
		glAttachShader(alphashader, vert);
		glAttachShader(alphashader, frag);
		glLinkProgram(alphashader);
		if (glGetProgrami(alphashader, GL_LINK_STATUS) != GL_TRUE)
			throw new IllegalStateException(glGetProgramInfoLog(alphashader));
		glDeleteShader(frag);
		glDeleteShader(vert);
		
		offsetLoc = glGetUniformLocation(alphashader, "offset");
		
		// Image drawing shader
		vert = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vert, "#version 330 core\n" +
				"layout(location = 0) in vec2 pos;" +
				"const vec2[] texcoords = vec2[] (vec2(0,0), vec2(1, 0), vec2(0, 1), vec2(1, 1));" +
				"out vec2 tex;" +
				"void main() {" +
				"gl_Position = vec4(pos, 0.0, 1.0);" +
				"tex = texcoords[gl_VertexID];" +
				"}");
		glCompileShader(vert);
		if (glGetShaderi(vert, GL_COMPILE_STATUS) != GL_TRUE)
			throw new IllegalStateException(glGetShaderInfoLog(vert));
		
		frag = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(frag, "#version 330 core\n" +
				"in vec2 tex;" +
				"uniform sampler2D image;" +
				"out vec4 color;" +
				"void main() {" +
				"color = texture(image, tex);" +
				"}");
		glCompileShader(frag);
		if (glGetShaderi(frag, GL_COMPILE_STATUS) != GL_TRUE)
			throw new IllegalStateException(glGetShaderInfoLog(frag));
		
		imageshader = glCreateProgram();
		glAttachShader(imageshader, vert);
		glAttachShader(imageshader, frag);
		glLinkProgram(imageshader);
		if (glGetProgrami(imageshader, GL_LINK_STATUS) != GL_TRUE)
			throw new IllegalStateException(glGetProgramInfoLog(imageshader));
		glDeleteShader(frag);
		glDeleteShader(vert);
		
		positionsbuffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, positionsbuffer);
		glBufferData(GL_ARRAY_BUFFER, 32, GL_STATIC_DRAW);
		
		positions = je_malloc(32).asFloatBuffer();
		clearcolor = je_malloc(16).asFloatBuffer();
		clearcolor.put(0, 0.25f);
		clearcolor.put(1, 0.25f);
		clearcolor.put(2, 0.25f);
		clearcolor.put(3, 1);
		
		finalTex = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, finalTex);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
		
		fbo = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, fbo);
		glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, finalTex, 0);
		glDrawBuffers(GL_COLOR_ATTACHMENT0);
	}
	
	public static int getFinalTex() {
		return finalTex;
	}
}
