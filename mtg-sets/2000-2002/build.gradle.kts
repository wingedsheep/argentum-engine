// Card definitions for sets released 2000–2002.
//
// Era modules are independent: each depends only on the SDK and :mtg-sets:core, never on another
// era, so they compile in parallel and a change to one era recompiles only that era. A set must
// therefore not reference another set's objects or cards at compile time — refer to another set
// by code instead (see MtgSet.basicLandsFallbackCode). A cross-era import is a compile error.
//
// Boundaries are FIXED. A new release year gets a new module; sets already placed never move, so
// this file's contents only ever grow.
plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    api(project(":mtg-sdk"))
    api(project(":mtg-sets:core"))
}
