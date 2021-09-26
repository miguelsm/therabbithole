Start image resizing proxy:

``` sh
nix-shell
go get willnorris.com/go/imageproxy/cmd/imageproxy
$GOPATH/bin/imageproxy
```

Generate `html` and save it into `/out` using the REPL and code in `comment` block at the end of `core.clj`.

Execute generate book script:

``` sh
nix-shell
./script/generate-book
```
