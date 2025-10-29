#!/bin/bash

#
# Prepare GPG Key is expected to be in base64
#
# Linux/Mac:
#   gpg -a --export-secret-keys "your@email" | base64 > gpg.base64
#
# Windows:
#   1、gpg -a --export-secret-keys "your@email" > gpg.tmp
#   2、certutil -encode gpg.tmp gpg.base64
#
echo "$GPG_KEY_BASE64" | base64 --decode > gpg.asc
echo ${GPG_PASSPHRASE} | gpg --batch --yes --passphrase-fd 0 --import gpg.asc
gpg -k