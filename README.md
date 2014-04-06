DirecTree
---------

A Simple DSL for :
- Creating directory trees with text files (with contents).
- Keeping multiple source directories in sync with a target directory.

[![Build Status](https://travis-ci.org/kdabir/directree.svg?branch=master)](https://travis-ci.org/kdabir/directree)

> directree is available in `jcenter` maven repository.

```groovy
@Grab('io.github.kdabir.directree:directree:0.2')
import static directree.DirTree.create

create("my-dir") {
    dir ("todo") {
        file "first.txt", "check out this new library"
    }

    file ("README.md") { """
        Directree
        ${'='*80}

        There are multiple ways in which content can be written to a file.
        after all, its all a valid groovy code.
        """.stripIndent()
    }
}
```

#### Verifying it:

`$ tree my-dir`

    my-dir
    |-- README.md
    `-- todo
        `-- first.txt

    1 directory, 2 files

`$ cat my-dir/README.md`

    Directree
    ================================================================================

    There are multiple ways in which content can be written to a file.
    after all, its all a valid groovy code.

`$ cat my-dir/todo/first.txt`

    check out this new library

#### More realistic example

You can create a project structure:

```groovy
create("my-project") {
    dir "src" , {
        file "hello.groovy", "println 'hello world'"
    }
    dir "test", {
        file ".gitkeep"
    }
    file "build.gradle" , ""
    file ".gitignore", "*.class"
}
```

`create(".")` will create the files and directories in current directory.


