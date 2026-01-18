package com.wingedsheep.rulesengine.game

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse

class PhaseTest : FunSpec({

    context("isMainPhase") {
        test("PRECOMBAT_MAIN is main phase") {
            Phase.PRECOMBAT_MAIN.isMainPhase.shouldBeTrue()
        }

        test("POSTCOMBAT_MAIN is main phase") {
            Phase.POSTCOMBAT_MAIN.isMainPhase.shouldBeTrue()
        }

        test("BEGINNING is not main phase") {
            Phase.BEGINNING.isMainPhase.shouldBeFalse()
        }

        test("COMBAT is not main phase") {
            Phase.COMBAT.isMainPhase.shouldBeFalse()
        }

        test("ENDING is not main phase") {
            Phase.ENDING.isMainPhase.shouldBeFalse()
        }
    }

    context("next") {
        test("phases progress in correct order") {
            Phase.BEGINNING.next() shouldBe Phase.PRECOMBAT_MAIN
            Phase.PRECOMBAT_MAIN.next() shouldBe Phase.COMBAT
            Phase.COMBAT.next() shouldBe Phase.POSTCOMBAT_MAIN
            Phase.POSTCOMBAT_MAIN.next() shouldBe Phase.ENDING
            Phase.ENDING.next() shouldBe Phase.BEGINNING
        }
    }

    context("FIRST") {
        test("first phase is BEGINNING") {
            Phase.FIRST shouldBe Phase.BEGINNING
        }
    }

    context("displayName") {
        test("phases have correct display names") {
            Phase.BEGINNING.displayName shouldBe "Beginning Phase"
            Phase.PRECOMBAT_MAIN.displayName shouldBe "Precombat Main Phase"
            Phase.COMBAT.displayName shouldBe "Combat Phase"
            Phase.POSTCOMBAT_MAIN.displayName shouldBe "Postcombat Main Phase"
            Phase.ENDING.displayName shouldBe "Ending Phase"
        }
    }
})
