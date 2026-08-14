package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Parasitic Grasp (VOW #123).
 *
 * {1}{B} Instant — Cleave {1}{B}{B}
 * "Parasitic Grasp deals 3 damage to target [Human] creature. You gain 3 life."
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * (cheaper) cast can only hit a Human creature; the cleaved cast broadens the target to any
 * creature. Both modes deal 3 damage and gain 3 life unconditionally.
 *
 * Target-only difference: the base [target] carries the "Human" subtype restriction and
 * [cleaveTarget] drops it. These tests pin both modes:
 *  - printed cast kills a Human (a 2/2) and gains 3 life, but is illegal against a non-Human, and
 *  - the cleaved cast kills a non-Human creature and still gains 3 life.
 */
class ParasiticGraspScenarioTest : ScenarioTestBase() {

    init {
        context("Parasitic Grasp — printed cast (brackets present)") {

            test("deals 3 damage to a Human creature and gains 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Parasitic Grasp")
                    .withLandsOnBattlefield(1, "Swamp", 2) // {1}{B}
                    .withCardOnBattlefield(2, "Glory Seeker", summoningSickness = false) // 2/2 Human
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val human = game.findPermanent("Glory Seeker")!!

                val cast = game.castSpell(1, "Parasitic Grasp", targetId = human)
                withClue("A Human creature is a legal target for the printed cast: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("3 damage kills the 2/2 Human") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                    game.isInGraveyard(2, "Glory Seeker") shouldBe true
                }
                withClue("The caster gains 3 life") {
                    game.getLifeTotal(1) shouldBe 23
                }
            }

            test("rejects a non-Human creature as an illegal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Parasitic Grasp")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) // Bear, not Human
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpell(1, "Parasitic Grasp", targetId = bears)
                withClue("A non-Human creature is not a legal target for the printed cast") {
                    cast.error shouldNotBe null
                }
                withClue("The illegally-targeted creature survives and no life is gained") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }

        context("Parasitic Grasp — cleaved cast (brackets removed)") {

            test("deals 3 damage to any creature and gains 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Parasitic Grasp")
                    .withLandsOnBattlefield(1, "Swamp", 3) // Cleave {1}{B}{B}
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) // 2/2 non-Human
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpellWithCleave(1, "Parasitic Grasp", targetId = bears)
                withClue("Paying the cleave cost broadens the target to any creature: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("3 damage kills the non-Human 2/2") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("The caster gains 3 life") {
                    game.getLifeTotal(1) shouldBe 23
                }
            }
        }
    }
}
