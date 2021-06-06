# ast-kotlin-VorPAL-Internship

A simple Kotlin code analyzer.

During the work, a tree is built, which displays the structure of the program required to build the following metrics:

1. Maximum inheritance depth
2. Average depth of inheritance
3. Average number of overridden methods
4. Average number of fields in a class
5. ABC metric

## Usage

Just enter the path to the folder with the project on Kotlin or to the separate file (s).

 Command Line: `KOTLIN-PROJECT-OR-FILE... [-x XML] [-j JSON] [-y YAML]`

- `-h, --help`               show this help message and exit
- `-x XML, --xml XML`        Path to xml report file
- `-j JSON, --json JSON`     Path to json report file
- `-y YAML, --yaml YAML`     Path to yaml report file


positional arguments:
  KOTLIN-PROJECT-OR-FILE   Paths to kotlin projects or files
