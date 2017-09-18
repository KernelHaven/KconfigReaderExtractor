# KconfigReaderExtractor

A variability-model extractor for [KernelHaven](https://github.com/KernelHaven/KernelHaven).
This extractor uses [KconfigReader](https://github.com/ckaestne/kconfigreader) to analyze the Linux Kernel.

## Capabilities

This extractor reads the Kconfig model of the Linux Kernel. To do that, it has to modify the Linux source tree by calling `make allyesconfig prepare` on it. Be aware that this overrides any previously present `.config` file in the Linux source tree.

## Usage

To use this extractor, set `variability.extractor.class` to `net.ssehub.kernel_haven.kconfigreader.KconfigReaderExtractor` in the KernelHaven properties.

### Dependencies

In addition to KernelHaven, this extractor has the following dependencies:
* Only runs on a Linux operating system
* C compiler and `make`. On Ubuntu just install the `build-essential` package via: `sudo apt-get install build-essential`

### Configuration

In addition to the default ones, this extractor has the following configuration options in the KernelHaven properties:

| Key | Mandatory | Default | Example | Description |
|-----|-----------|---------|---------|-------------|
| `variability.extractor.find_locations` | No | `false` | `true` | If set to true, the extractor will store source locations for each variable. Those locations represent occurences of the variable in the files that kconfigreader used for generating the VariabilityMode. |


## License
This extractor is licensed under GPLv3. Another license would be possible with following restrictions:

The extractor contains KconfigReader which is under GPL-3.0. We do not link against KconfigReader, so technically we are not infected by GPL. However a release under a license other than GPL-3.0 would require the removal of the contained KconfigReader.
