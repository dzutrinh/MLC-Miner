# MLC-Miner
 Multi-level Closed High-Utility Pattern Miner

## Folder structure

`src`: Source code of the algorithm MLC-Miner and CHUI-Miner*

`taxonomy`: synthesized taxonomy of the databases used in the work.

## Requirements
- Java 8.
- An IDE (such as NetBeans or Eclipse) to import the whole project

## How to run
* Within each algorithm, there is a test drive call `TestXXXX.java`. 
* Changing database: replace the value of the `dataset` variable with your desire dataset. The driver will automatically append the `_tax` and `_trans` suffices.
* Use the `minutil` variable to specify the minimum utility threshold. The values should be the exact threshold to be used, no relative (percentages).

## License
The source code is released under GNU GPLv3.