package com.wingedsheep.rulesengine.zone

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse

class ZoneTypeTest : FunSpec({

    context("isPublic") {
        test("battlefield is public") {
            ZoneType.BATTLEFIELD.isPublic.shouldBeTrue()
        }

        test("graveyard is public") {
            ZoneType.GRAVEYARD.isPublic.shouldBeTrue()
        }

        test("stack is public") {
            ZoneType.STACK.isPublic.shouldBeTrue()
        }

        test("exile is public") {
            ZoneType.EXILE.isPublic.shouldBeTrue()
        }

        test("library is not public") {
            ZoneType.LIBRARY.isPublic.shouldBeFalse()
        }

        test("hand is not public") {
            ZoneType.HAND.isPublic.shouldBeFalse()
        }
    }

    context("isHidden") {
        test("library is hidden") {
            ZoneType.LIBRARY.isHidden.shouldBeTrue()
        }

        test("hand is hidden") {
            ZoneType.HAND.isHidden.shouldBeTrue()
        }

        test("battlefield is not hidden") {
            ZoneType.BATTLEFIELD.isHidden.shouldBeFalse()
        }
    }

    context("isShared") {
        test("battlefield is shared") {
            ZoneType.BATTLEFIELD.isShared.shouldBeTrue()
        }

        test("stack is shared") {
            ZoneType.STACK.isShared.shouldBeTrue()
        }

        test("exile is shared") {
            ZoneType.EXILE.isShared.shouldBeTrue()
        }

        test("library is not shared") {
            ZoneType.LIBRARY.isShared.shouldBeFalse()
        }

        test("hand is not shared") {
            ZoneType.HAND.isShared.shouldBeFalse()
        }

        test("graveyard is not shared") {
            ZoneType.GRAVEYARD.isShared.shouldBeFalse()
        }
    }
})
