package bpm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Switch {
	String longOpt();
	String shortOpt();
	String description();
}
