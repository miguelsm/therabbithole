WIP

Start the image resizing proxy:

``` sh
nix-shell
go get willnorris.com/go/imageproxy/cmd/imageproxy
$GOPATH/bin/imageproxy -cache /tmp/imageproxy
```

Generate the HTML and save it into `./out` using a REPL and the code in the `comment` block at the end of `./src/core.clj`.

Execute the script to generate the book:

``` sh
nix-shell
./script/generate-book
```
