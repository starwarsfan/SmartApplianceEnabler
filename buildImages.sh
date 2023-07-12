#!/usr/bin/env bash
#
# Copyright (C) 2023 Axel Müller <axel.mueller@avanux.de>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along
# with this program; if not, write to the Free Software Foundation, Inc.,
# 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
#

# ===========================================================================
#
# Created: 2023-07-12 Y. Schumann
#
# Helper script to build and push SmartApplianceEnabler baseimage
#
# ===========================================================================

# Store path from where script was called, determine own location
# and source helper content from there
callDir=$(pwd)
ownLocation="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "${ownLocation}" || exit 1


helpMe() {
    echo "
    Helper script to build SmartApplianceEnabler Docker image
    for AMD64 aka x86_64 architecture.

    Usage:
    ${0} [options]

    Optional parameters:
    -7 .. Also build ARMv7 image
    -8 .. Also build ARMv8 image
    -p .. Push image to DockerHub
    -t <docker-tag>
       .. Docker tag to set. Default: ${DOCKER_TAG}
    -v <version>
       .. Version of SAE. Currently used only as an env var on
          created Docker image. Default: ${SAE_VERSION}
    -h  Show this help
    "
}

_init() {
    if [[ -n "${TERM}" && "${TERM}" != "dumb" ]]; then
#        GREEN=$(tput setaf 2) RED=$(tput setaf 1) BLUE="$(tput setaf 4)"
#        LTGREYBG="$(tput setab 7)"
#        NORMAL=$(tput sgr0) BLINK=$(tput blink)
        GREEN='\e[0;32m' RED='\e[0;31m' BLUE='\e[0;34m' NORMAL='\e[0m'
    else
        GREEN="" RED="" BLUE="" LTGREYBG="" NORMAL="" BLINK=""
    fi
}

die() {
    error=${1:-1}
    shift
    error "$*" >&2
    exit ${error}
}

info() {
    printf "${GREEN}%-7s: %s${NORMAL}\n" "Info" "$*"
}

error() {
    printf "${RED}%-7s: %s${NORMAL}\n" "Error" "$*"
}

warning() {
    printf "${BLUE}%-7s: %s${NORMAL}\n" "Warning" "$*"
}

_init

PUSH_IMAGE=''
BUILD_ARM_V7=false
BUILD_ARM_V8=false
PLATFORM="linux/amd64"
DOCKER_TAG="avanux/smartapplianceenabler:ci"
SAE_VERSION="2.3.0"

while getopts 78hpt:v: option; do
    case ${option} in
        7) BUILD_ARM_V7=true;;
        8) BUILD_ARM_V8=true;;
        p) PUSH_IMAGE=--push;;
        t) DOCKER_TAG="${OPTARG}" ;;
        v) SAE_VERSION="${OPTARG}" ;;
        h) helpMe && exit 0;;
        *) die 90 "invalid option \"${OPTARG}\"";;
    esac
done

info "Building for these architectures:"
if ${BUILD_ARM_V7} ; then
    PLATFORM=${PLATFORM},linux/arm/v8
    info " -> ARM64 (ARMv8)"
fi
if ${BUILD_ARM_V8} ; then
    PLATFORM=${PLATFORM},linux/arm/v7
    info " -> ARM32 (ARMv/"
fi
info " -> AMD64 (x86_64)"

info "Building SmartApplianceEnabler image"
docker buildx \
    build \
    -f docker/sae-ci/Dockerfile \
    --platform=${PLATFORM} \
    --build-arg SAE_VERSION=${SAE_VERSION} \
    --tag=${DOCKER_TAG} \
    ${PUSH_IMAGE} \
    .
info " -> Done"
