dependencies {
    paperweight.paperDevBundle("1.20.1-R0.1-SNAPSHOT") {
        exclude("net.fabricmc")
    }
    remapper("net.fabricmc:tiny-remapper:0.10.2:fat")
}