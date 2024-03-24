# MLC-Miner
 Multi-level Closed High-Utility Pattern Miner

## Folder structure

- `src`: Source code of the algorithm `MLC-Miner` and `CHUI-Miner*`

- `taxonomy`: synthesized taxonomy of the databases used in the work. Databases with real taxonomies can be obtained from the SPMF website.

## Requirements
- Java 8.
- An IDE (such as NetBeans or Eclipse) to import the whole project

## How to run
* Within each algorithm, there is a test drive call `TestXXXX.java`. 
* Changing database: replace the value of the `dataset` variable with your desire dataset. The driver will automatically append the `_tax` and `_trans` suffixes. 
* The databases must comes in two separated file: one containing the transactions, which name ends with `_trans`. The other contains the related taxonomy of items, which name ends with `_tax`. These two files should be place on top level of the package folder structure, outside the `src` folder. The format of both transaction database and the taxonomy strictly follow the SPMF format.
* Use the `minutil` variable to specify the minimum utility threshold. The values should be the exact threshold to be used, no relative (percentages).

## License
The source code is released under GNU GPLv3.