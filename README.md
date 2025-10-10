# MLC-Miner
Multi-level Closed High-Utility Pattern Miner

## Folder structure

- `build`: The executable of the algorithm `MLC-Miner`.
- `taxonomy`: synthesized taxonomy of the databases used in the work. Databases with real taxonomies can be obtained from the SPMF website.

## Requirements
- Java 8.
- An IDE (such as NetBeans or Eclipse) to import the whole project

## How to run

`java -jar build/mlcminer.jar --dataset sample --minutil 50 --eucp true --output result.txt`

or

`java -jar build/mlcminer.jar --trans sample_trans.txt --tax sample_tax.txt --minutil 30`

## Parameters
- `--dataset <name> (produces <name>_trans.txt and <name>_tax.txt)`
- `--trans <file>`
- `--tax <file>`
- `--minutil <value>`
- `--eucp <true|false>`
- `--output <file>`
- `--maxtrans <number>`
- `-h / --help`

## License
GNU GPLv3.
