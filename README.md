DirecTree
---------

A Simple DSL to create Directory Tree and Text Files with contents

```groovy
@GrabResolver(name="directree", root='http://dl.bintray.com/kdabir/maven') @Grab('directree:directree:0.1')
import static directree.DirTree.create

create("crazystuff") {
    dir "temp" , {
        file "todo.txt", "check out this new library"
    }
}
```

#### Verifying it:

`$ tree crazystuff`

>       crazystuff
>       `-- temp
>            `-- todo.txt

`$ cat crazystuff/temp/todo.txt`

>       check out this new library


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