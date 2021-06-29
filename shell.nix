{ pkgs ? import <nixpkgs> {} }:

with pkgs;

mkShell {
  buildInputs = [
    clojure
    # koreader
    maven
    openjdk11
    # pandoc
    poppler # for pdfunite
    python3
    python38Packages.weasyprint
    # texlive.combined.scheme-full
  ];
}
