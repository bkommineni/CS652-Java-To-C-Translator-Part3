package cs652.j.codegen.model;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

public class CFile extends OutputModelObject {
	public final String fileName;
	@ModelElement public List<ClassDef> classes = new ArrayList<>();
	@ModelElement public MainMethod main;

	public CFile(String fileName) {
		this.fileName = fileName;
	}

	public void addClass(ClassDef classDef)
	{
		classes.add(classDef);
	}

	public void addMain(MainMethod main)
	{
		this.main = main;
	}
}
