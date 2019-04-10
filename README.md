# KconfigReaderExtractor

![Build Status](https://jenkins-2.sse.uni-hildesheim.de/buildStatus/icon?job=KH_KconfigReaderExtractor)

A variability-model extractor for [KernelHaven](https://github.com/KernelHaven/KernelHaven).

This extractor uses [KconfigReader](https://github.com/ckaestne/kconfigreader) to analyze the Linux Kernel.

## Capabilities

This extractor reads the Kconfig model of the Linux Kernel. To do that, it has to modify the Linux source tree by calling `make allyesconfig prepare` on it. Be aware that this overrides any previously present `.config` file in the Linux source tree.

## Usage

Place [`KconfigReaderExtractor.jar`](https://jenkins-2.sse.uni-hildesheim.de/view/KernelHaven/job/KH_KconfigReaderExtractor/lastSuccessfulBuild/artifact/build/jar/KconfigReaderExtractor.jar) in the plugins folder of KernelHaven.

To use this extractor, set `variability.extractor.class` to `net.ssehub.kernel_haven.kconfigreader.KconfigReaderExtractor` in the KernelHaven properties.

## Dependencies

In addition to KernelHaven, this plugin has the following dependencies:
* Only runs on a Linux operating system
* C compiler and `make` to run `make allyesconfig prepare` in the Linux source tree. On Ubuntu just install the `build-essential` and `libelf-dev` packages via: `sudo apt install build-essential libelf-dev`

## License

This plugin is licensed under [GPLv3](https://www.gnu.org/licenses/gpl-3.0.html).

Another license would be possible with the following restriction:
* The plugin contains KconfigReader which is under GPLv3. We do not link against KconfigReader, so technically we are not infected by GPL. However a release under a license other than GPLv3 would require the removal of the contained KconfigReader.

## Used Tools

The following tools are used (and bundled in `res/`) by this plugin:

| Tool | Version | License |
|------|---------|---------|
| [kconfigreader](https://github.com/ckaestne/kconfigreader.git) | [2016-07-01 (913bf31)](https://github.com/ckaestne/kconfigreader/commit/913bf3178af5a8ac8bedc5e8733561ed38280cf9) | [GPLv3](https://www.gnu.org/licenses/gpl.html) |
