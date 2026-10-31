#!/usr/bin/env bash
# Keeps the persisted Gradle build cache down to the entries a CI job actually used.
#
#   gradle-build-cache.sh mark    — after the cache is restored, before Gradle runs
#   gradle-build-cache.sh prune   — after Gradle runs, before the cache is saved
#
# Why: the repo's Actions cache is capped at 10 GB, and without pruning every saved entry carries
# along everything it was restored from, so each group's cache only ever grows (PR entries reached
# 1.1 GB per group). Pruning keeps exactly the entries this run read or wrote.
#
# "Read" is tracked by access time, not modification time. Gradle does NOT bump an entry's mtime
# when it loads it from the local cache, so an mtime-only prune deleted every cache *hit*: a fully
# cached run saved a ~44 KB cache, the next run recompiled the whole card corpus from scratch, and
# runs alternated between warm and cold. `mark` resets every restored entry's atime to the epoch;
# with the default `relatime` mount option the kernel then updates it on the first read (atime is
# older than mtime), so `prune` can tell a hit from an entry nobody touched.
#
# If the filesystem doesn't record access times at all (noatime), `mark` detects it with a canary
# file and `prune` keeps everything instead of throwing the hits away.
#
# Env: GRADLE_BUILD_CACHE_DIR (the cache directory), RUNNER_TEMP, GITHUB_ENV (set by Actions).
set -euo pipefail

cache_dir="${GRADLE_BUILD_CACHE_DIR:?GRADLE_BUILD_CACHE_DIR must be set}"
marker="${RUNNER_TEMP:?}/gradle-cache-marker"

case "${1:-}" in
  mark)
    mkdir -p "${cache_dir}"
    find "${cache_dir}" -type f -exec touch -a -d @0 {} +

    # Canary on the same filesystem as the cache: does a read update atime here?
    canary="$(dirname "${cache_dir}")/.atime-canary"
    echo canary > "${canary}"
    touch -a -d @0 "${canary}"
    cat "${canary}" > /dev/null
    if [ "$(stat -c %X "${canary}")" -gt 0 ]; then
      echo "GRADLE_CACHE_ATIME_TRACKING=true" >> "${GITHUB_ENV}"
      echo "Access-time tracking works; unused cache entries will be pruned."
    else
      echo "GRADLE_CACHE_ATIME_TRACKING=false" >> "${GITHUB_ENV}"
      echo "::warning::Filesystem does not update access times; the Gradle build cache will not be pruned this run."
    fi
    rm -f "${canary}"

    # Last, so every atime reset above is older than the marker.
    sleep 1
    touch "${marker}"
    ;;

  prune)
    du -sh "${cache_dir}" || true
    if [ "${GRADLE_CACHE_ATIME_TRACKING:-false}" != "true" ]; then
      echo "Skipping prune: access times are not tracked on this runner."
      exit 0
    fi
    # Delete entries neither read (atime) nor written (mtime) since the marker.
    find "${cache_dir}" -type f ! -anewer "${marker}" ! -newer "${marker}" -delete
    du -sh "${cache_dir}" || true
    ;;

  *)
    echo "usage: $0 mark|prune" >&2
    exit 2
    ;;
esac
