package nl.javadude.gradle.plugins.license

class License {

	List<String> lines

	def License() {
		this.lines = [];
	}

	def add(String line) {
		lines.add(line)
	}

	boolean isLicensed(File file) {
		def fileLines = file.readLines()
		fileLines.subList(0, fileLines.size() > lines.size() ? lines.size() : fileLines.size()).containsAll(lines)
	}
}
