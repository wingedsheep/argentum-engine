package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Death's Caress (DKA #59) — "Destroy target creature. If that creature was a Human, you gain life
 * equal to its toughness."
 *
 * Whether it was a Human and how tough it was are read as it last existed on the battlefield: a
 * pumped Human gains its pumped toughness, a non-Human gains nothing.
 */
class DeathsCaressScenarioTest : ScenarioTestBase() {

    init {
        fun board(victim: String) = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, "Death's Caress")
            .withCardInHand(1, "Giant Growth")
            .withLandsOnBattlefield(1, "Swamp", 5)
            .withLandsOnBattlefield(1, "Forest", 1)
            .withCardOnBattlefield(2, victim)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Death's Caress") {

            test("destroying a Human gains life equal to its toughness") {
                val game = board("Village Bell-Ringer")
                val ringer = game.findPermanent("Village Bell-Ringer")!!

                game.castSpell(1, "Death's Caress", ringer).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Village Bell-Ringer") shouldBe true
                withClue("Village Bell-Ringer is a 1/4 Human") {
                    game.getLifeTotal(1) shouldBe 24
                }
            }

            test("uses the Human's toughness as it last existed, including a pump") {
                val game = board("Village Bell-Ringer")
                val ringer = game.findPermanent("Village Bell-Ringer")!!

                game.castSpell(1, "Giant Growth", ringer).error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Death's Caress", ringer).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Village Bell-Ringer") shouldBe true
                withClue("1/4 pumped to 4/7") {
                    game.getLifeTotal(1) shouldBe 27
                }
            }

            test("destroying a non-Human gains no life") {
                val game = board("Hill Giant")
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Death's Caress", giant).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
