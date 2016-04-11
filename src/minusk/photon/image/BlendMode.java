package minusk.photon.image;

import java.util.ArrayList;
import java.util.Scanner;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

/**
 * Created by MinusKelvin on 28/09/15.
 */
public class BlendMode {
	private static final String vertexShader = "#version 330 core\n" +
			"const vec2[] positions = vec2[] (vec2(-1,-1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));" +
			"void main() {" +
			"   gl_Position = vec4(positions[gl_VertexID], 0.0, 1.0);" +
			"}";
	
	private static final String fragmentHeader = "#version 330 core\n" +
			"out vec4 output;" +
			"uniform sampler2D src;" +
			"uniform sampler2D dst;" +
			"void main() {" +
			"   vec4 source = texelFetch(src, ivec2(gl_FragCoord.xy), 0);" +
			"   vec4 destination = texelFetch(dst, ivec2(gl_FragCoord.xy), 0);";
	
	private static final String fragmentFooter = "}";
	private static int vertex, fbo;
	
	private static ArrayList<BlendMode> blendModes = new ArrayList<>();
	private final int shaderprogram, srcloc, dstloc;
	
	private BlendMode(int program) {
		shaderprogram = program;
		srcloc = glGetUniformLocation(program, "src");
		dstloc = glGetUniformLocation(program, "dst");
	}
	
	public void blend(int width, int height, int src, int dst, int out) {
		glBindFramebuffer(GL_FRAMEBUFFER, fbo);
		glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, out, 0);
		
		glUseProgram(shaderprogram);
		
		glActiveTexture(GL_TEXTURE1);
		glUniform1i(srcloc, 1);
		glBindTexture(GL_TEXTURE_2D, src);
		
		glActiveTexture(GL_TEXTURE0);
		glUniform1i(dstloc, 0);
		glBindTexture(GL_TEXTURE_2D, dst);
		
		glViewport(0,0,width,height);
		
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	}
	
	public static void initialize() {
		vertex = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertex, vertexShader);
		glCompileShader(vertex);
		if (glGetShaderi(vertex, GL_COMPILE_STATUS) != GL_TRUE)
			throw new IllegalStateException(glGetShaderInfoLog(vertex));
		
		String s;
		
		s = createBlendMode(readClasspath("/minusk/photon/res/blendmodes/overwrite.blend"));
		if (s != null) System.err.println(s);
		s = createBlendMode(readClasspath("/minusk/photon/res/blendmodes/alpha.blend"));
		if (s != null) System.err.println(s);
		
		fbo = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, fbo);
		glDrawBuffers(GL_COLOR_ATTACHMENT0);
	}
	
	public static BlendMode getDefault() {
		return blendModes.get(0);
	}
	
	public static BlendMode getOverwrite() {
		return blendModes.get(0);
	}
	
	public static BlendMode getAlpha() {
		return blendModes.get(1);
	}
	
	public static String readClasspath(String path) {
		Scanner scanner = new Scanner(BlendMode.class.getResourceAsStream(path)).useDelimiter("\\Z");
		String v = scanner.next();
		scanner.close();
		return v;
	}
	
	public static String createBlendMode(String shader) {
		String fragment = fragmentHeader + shader + fragmentFooter;
		
		int frag = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(frag, fragment);
		glCompileShader(frag);
		if (glGetShaderi(frag, GL_COMPILE_STATUS) != GL_TRUE)
			return glGetShaderInfoLog(frag);
		
		int program = glCreateProgram();
		glAttachShader(program, vertex);
		glAttachShader(program, frag);
		glLinkProgram(program);
		if (glGetProgrami(program, GL_LINK_STATUS) != GL_TRUE)
			return glGetProgramInfoLog(program);
		glDeleteShader(frag);
		
		blendModes.add(new BlendMode(program));
		return null;
	}
}
