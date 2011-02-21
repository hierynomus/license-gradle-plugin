package nl.javadude.gradle.plugins.license.tasks

import org.gradle.api.GradleException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction
import nl.javadude.gradle.plugins.license.types.HashFormat
import nl.javadude.gradle.plugins.license.types.SlashStarFormat

class LicenseTask extends ConventionTask {
	Properties filetypes
	Map licenses = [:]
	static Map formatters = [:]

	static {
		formatters[HashFormat.class.name] = new HashFormat()
		formatters[SlashStarFormat.class.name] = new SlashStarFormat()
	}

	def LicenseTask() {
		filetypes = new Properties()
		filetypes.load this.getClass().getClassLoader().getResourceAsStream("filetypes.properties")
	}

	def convertAllLicenseTypes(List<java.lang.String> license) {
		filetypes.each { k, v ->
			def format = LicenseTask.formatters[v]
			licenses[v] = format.transform(license)
		}
	}

	List<java.lang.String> loadLicense() {
		def license = project.convention.plugins.license.license
		if (!license.exists()) {
			throw new GradleException("The license file [" + license + "] does not exist.")
		}
		license.readLines()
	}

	@TaskAction
	protected void process() {
		init()

		toBeLicensed = scanForFiles()
		toBeLicensed.findAll({ shouldBeLicensed it }).each { file ->
			handleFile(file)
		}
	}

	def init() {
		List<String> license = loadLicense()
		convertAllLicenseTypes(license)
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
		println "Adding license on " + file
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
