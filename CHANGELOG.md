# Changelog

## 2.0.1 (27/10/2022)
### Changed
- Fix the java version check
- Remove deprecated usage of getStackTraceElement in LoggingUtils.java

## 2.0.0 (25/10/2022)
### Added
- Print execution time
- Create classes for each SIM file type
- Map SIM files to their name based on the standard
- Get file type and size for 2G mode
- Handle files based on AID
- Detect file linking for MF -> AID
- Export data to CSV for the file scan module

### Changed
- Improve functions to select file path
- Improve file attribute parsing
- Fix AID listing
- Improve file discovery
- Add more TARs to be scanned

## 1.9.0 (27/09/2019)
### Added
- **Test for S@T/WIB vulnerabilities:** These SIM applications could be abused to retrieve user location, send SMS or start a call if the security settings are not properly set
- **New SPI fuzzer settings:** A special test case with all security fields set to zero has been defined
- **New OTA fuzzer settings:** A special test case for no UDH has been added to the OTA fuzzer

## 1.8.1
### Added
- OTA-Passthrough-Fuzzer 
- OTA-APDU scanner
- File Scanner
- Fuzzing with constants
- 2G fallback

## v1.5
### Added
- PIN disable/verify/skip
- Fuzzer
- APDU scanner
- TAR scanner
