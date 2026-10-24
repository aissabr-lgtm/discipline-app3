{ pkgs }: {
    deps = [
        pkgs.jdk17
        pkgs.gradle
        pkgs.git
        pkgs.zip
        pkgs.unzip
    ];
}
