{ pkgs ? import <nixpkgs> {} }:

with pkgs;

mkShell {
  buildInputs = [
    clojure
    # koreader
    maven
    openjdk11
    # pandoc
    python3
    python38Packages.weasyprint
    # texlive.combined.scheme-full
  ];
}
