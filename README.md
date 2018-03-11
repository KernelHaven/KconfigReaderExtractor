# KconfigReaderExtractor

![Build Status](http://jenkins.sse.uni-hildesheim.de/buildStatus/icon?job=KernelHaven_KconfigReaderExtractor)

A variability-model extractor for [KernelHaven](https://github.com/KernelHaven/KernelHaven).

This extractor uses [KconfigReader](https://github.com/ckaestne/kconfigreader) to analyze the Linux Kernel.

## Capabilities

This extractor reads the Kconfig model of the Linux Kernel. To do that, it has to modify the Linux source tree by calling `make allyesconfig prepare` on it. Be aware that this overrides any previously present `.config` file in the Linux source tree.

## Usage

Place [`KconfigReaderExtractor.jar`](https://jenkins.sse.uni-hildesheim.de/view/KernelHaven/job/KernelHaven_KconfigReaderExtractor/lastSuccessfulBuild/artifact/build/jar/KconfigReaderExtractor.jar) in the plugins folder of KernelHaven.

To use this extractor, set `variability.extractor.class` to `net.ssehub.kernel_haven.kconfigreader.KconfigReaderExtractor` in the KernelHaven properties.

## Dependencies

In addition to KernelHaven, this plugin has the following dependencies:
* Only runs on a Linux operating system
* C compiler and `make`. On Ubuntu just install the `build-essential` package via: `sudo apt install build-essential`

## License

This plugin is licensed under GPLv3. Another license would be possible with following restrictions:

The plugin contains KconfigReader which is under GPL-3.0. We do not link against KconfigReader, so technically we are not infected by GPL. However a release under a license other than GPL-3.0 would require the removal of the contained KconfigReader.
