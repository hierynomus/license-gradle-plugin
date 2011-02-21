package nl.javadude.gradle.plugins.license.tasks

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction

class LicenseTask extends ConventionTask {
	Properties filetypes
	Map licenses = [:]

	def LicenseTask() {
		filetypes = new Properties()
		filetypes.load this.getClass().getClassLoader().getResourceAsStream("filetypes.properties")

		List<String> license = loadLicense()
		convertAllLicenseTypes(license)
	}

	def convertAllLicenseTypes(List<java.lang.String> license) {
		filetypes.each { k, v ->
			def format = Class.forName((String) v).newInstance()
			licenses[v] = format.transform(license)
		}
	}

	List<java.lang.String> loadLicense() {
		project.convention.plugins.license.license.readLines()
	}

	@TaskAction
	protected void process() {
		toBeLicensed = scanForFiles()
		toBeLicensed.findAll({ shouldBeLicensed it }).each { file ->
			handleFile(file)
		}
	}

	def scanForFiles() {
		List<File> toBeLicensed = []
		project.sourceSets.each { set ->
			set.allSource.each { file ->
				def ext = file.name.substring(file.name.indexOf('.') + 1)
				if (filetypes.containsKey(ext)) {
					toBeLicensed.add file
				}
			}
		}
		toBeLicensed
	}

	boolean shouldBeLicensed(File file) {
		def license = getLicenseForFile(file)
		!license.isLicensed(file)
	}

	private def getLicenseForFile(File file) {
		def ext = file.name.substring(file.name.indexOf('.') + 1)
		licenses.get(filetypes.get(ext))
	}

	def handleFile(File file) {
		def license = getLicenseForFile(file)
		def lines = file.readLines()
		file.delete()
		file.createNewFile()
		file.withWriter { w ->
			license.lines.each { line ->
				w.writeLine(line)
			}
			lines.each { line ->
				w.writeLine(line)
			}
			w.newLine()
		}
	}
}
