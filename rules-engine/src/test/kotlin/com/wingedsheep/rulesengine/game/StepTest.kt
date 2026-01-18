package com.wingedsheep.rulesengine.game

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContainExactly

class StepTest : FunSpec({

    context("phase association") {
        test("beginning phase steps") {
            Step.UNTAP.phase shouldBe Phase.BEGINNING
            Step.UPKEEP.phase shouldBe Phase.BEGINNING
            Step.DRAW.phase shouldBe Phase.BEGINNING
        }

        test("main phase steps") {
            Step.PRECOMBAT_MAIN.phase shouldBe Phase.PRECOMBAT_MAIN
            Step.POSTCOMBAT_MAIN.phase shouldBe Phase.POSTCOMBAT_MAIN
        }

        test("combat phase steps") {
            Step.BEGIN_COMBAT.phase shouldBe Phase.COMBAT
            Step.DECLARE_ATTACKERS.phase shouldBe Phase.COMBAT
            Step.DECLARE_BLOCKERS.phase shouldBe Phase.COMBAT
            Step.FIRST_STRIKE_COMBAT_DAMAGE.phase shouldBe Phase.COMBAT
            Step.COMBAT_DAMAGE.phase shouldBe Phase.COMBAT
            Step.END_COMBAT.phase shouldBe Phase.COMBAT
        }

        test("ending phase steps") {
            Step.END.phase shouldBe Phase.ENDING
            Step.CLEANUP.phase shouldBe Phase.ENDING
        }
    }

    context("isMainPhase") {
        test("main phase steps return true") {
            Step.PRECOMBAT_MAIN.isMainPhase.shouldBeTrue()
            Step.POSTCOMBAT_MAIN.isMainPhase.shouldBeTrue()
        }

        test("non-main phase steps return false") {
            Step.UNTAP.isMainPhase.shouldBeFalse()
            Step.DECLARE_ATTACKERS.isMainPhase.shouldBeFalse()
            Step.CLEANUP.isMainPhase.shouldBeFalse()
        }
    }

    context("allowsSorcerySpeed") {
        test("main phase steps allow sorcery speed") {
            Step.PRECOMBAT_MAIN.allowsSorcerySpeed.shouldBeTrue()
            Step.POSTCOMBAT_MAIN.allowsSorcerySpeed.shouldBeTrue()
        }

        test("non-main phase steps don't allow sorcery speed") {
            Step.UNTAP.allowsSorcerySpeed.shouldBeFalse()
            Step.UPKEEP.allowsSorcerySpeed.shouldBeFalse()
            Step.DECLARE_ATTACKERS.allowsSorcerySpeed.shouldBeFalse()
        }
    }

    context("hasPriority") {
        test("untap step has no priority") {
            Step.UNTAP.hasPriority.shouldBeFalse()
        }

        test("cleanup step has no priority") {
            Step.CLEANUP.hasPriority.shouldBeFalse()
        }

        test("other steps have priority") {
            Step.UPKEEP.hasPriority.shouldBeTrue()
            Step.DRAW.hasPriority.shouldBeTrue()
            Step.PRECOMBAT_MAIN.hasPriority.shouldBeTrue()
            Step.DECLARE_ATTACKERS.hasPriority.shouldBeTrue()
            Step.END.hasPriority.shouldBeTrue()
        }
    }

    context("next") {
        test("steps progress in correct order") {
            Step.UNTAP.next() shouldBe Step.UPKEEP
            Step.UPKEEP.next() shouldBe Step.DRAW
            Step.DRAW.next() shouldBe Step.PRECOMBAT_MAIN
            Step.PRECOMBAT_MAIN.next() shouldBe Step.BEGIN_COMBAT
            Step.BEGIN_COMBAT.next() shouldBe Step.DECLARE_ATTACKERS
            Step.DECLARE_ATTACKERS.next() shouldBe Step.DECLARE_BLOCKERS
            Step.DECLARE_BLOCKERS.next() shouldBe Step.FIRST_STRIKE_COMBAT_DAMAGE
            Step.FIRST_STRIKE_COMBAT_DAMAGE.next() shouldBe Step.COMBAT_DAMAGE
            Step.COMBAT_DAMAGE.next() shouldBe Step.END_COMBAT
            Step.END_COMBAT.next() shouldBe Step.POSTCOMBAT_MAIN
            Step.POSTCOMBAT_MAIN.next() shouldBe Step.END
            Step.END.next() shouldBe Step.CLEANUP
            Step.CLEANUP.next() shouldBe Step.UNTAP
        }
    }

    context("stepsInPhase") {
        test("returns steps for beginning phase") {
            Step.stepsInPhase(Phase.BEGINNING) shouldContainExactly listOf(
                Step.UNTAP, Step.UPKEEP, Step.DRAW
            )
        }

        test("returns steps for combat phase") {
            Step.stepsInPhase(Phase.COMBAT) shouldContainExactly listOf(
                Step.BEGIN_COMBAT, Step.DECLARE_ATTACKERS, Step.DECLARE_BLOCKERS,
                Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT
            )
        }

        test("returns single step for main phases") {
            Step.stepsInPhase(Phase.PRECOMBAT_MAIN) shouldContainExactly listOf(Step.PRECOMBAT_MAIN)
            Step.stepsInPhase(Phase.POSTCOMBAT_MAIN) shouldContainExactly listOf(Step.POSTCOMBAT_MAIN)
        }
    }

    context("firstStepOf") {
        test("returns first step of each phase") {
            Step.firstStepOf(Phase.BEGINNING) shouldBe Step.UNTAP
            Step.firstStepOf(Phase.PRECOMBAT_MAIN) shouldBe Step.PRECOMBAT_MAIN
            Step.firstStepOf(Phase.COMBAT) shouldBe Step.BEGIN_COMBAT
            Step.firstStepOf(Phase.POSTCOMBAT_MAIN) shouldBe Step.POSTCOMBAT_MAIN
            Step.firstStepOf(Phase.ENDING) shouldBe Step.END
        }
    }

    context("lastStepOf") {
        test("returns last step of each phase") {
            Step.lastStepOf(Phase.BEGINNING) shouldBe Step.DRAW
            Step.lastStepOf(Phase.PRECOMBAT_MAIN) shouldBe Step.PRECOMBAT_MAIN
            Step.lastStepOf(Phase.COMBAT) shouldBe Step.END_COMBAT
            Step.lastStepOf(Phase.POSTCOMBAT_MAIN) shouldBe Step.POSTCOMBAT_MAIN
            Step.lastStepOf(Phase.ENDING) shouldBe Step.CLEANUP
        }
    }
})
