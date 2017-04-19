# KconfigReaderExtractor ![Build Status of KconfigReaderExtractor](http://jenkins.sse.uni-hildesheim.de/buildStatus/icon?job=KernelHaven_KconfigReaderExtractor)
[KconfigReader](https://github.com/ckaestne/kconfigreader)
-based Kconfig extractor for KernelHaven

## Capabilities
This extractor reads the Kconfig model. To do that, it has to modify the Linux
source tree by calling `make allyesconfig prepare` on it. Be aware that this
overrides any previously present `.config` file in the Linux source tree.

## Build and Install
### Testing
The rests require a Linux system with installed `build-essential` package. On Ubuntu this package may be installed via:
`sudo apt-get install build-essential`

## License
This extractor is licensed under GPLv3. Another license would be possible with
following restrictions:

The extractor contains KconfigReader which is under GPL-3.0. We do not link 
against KconfigReader, so technically we are not infected by GPL. However a
release under a license other than GPL-3.0 would require the removal of the
contained KconfigReader.
