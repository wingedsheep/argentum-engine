package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Callaphe, Beloved of the Sea (THB #45; {1}{U}{U}, * /3).
 *
 *   Callaphe's power is equal to your devotion to blue.
 *   Creatures and enchantments you control have "Spells your opponents cast that target this
 *   permanent cost {1} more to cast."
 *
 * Covers the devotion CDA and the enchantment half of the grant, which Jubilant Skybonder's
 * creature-only filter never exercises.
 */
class CallapheBelovedOfTheSeaScenarioTest : ScenarioTestBase() {

    init {
        context("Callaphe, Beloved of the Sea") {

            test("power equals devotion to blue") {
                // {U}{U} devotion, plus Glorious Anthem's +1/+1
                val game = board(lands = 0)
                game.state.projectedState.getPower(game.findPermanent("Callaphe, Beloved of the Sea")!!) shouldBe 3
            }

            test("an opponent's spell targeting a noncreature enchantment you control costs {1} more") {
                val short = board(lands = 2)
                withClue("{1}{W} + {1} tax can't be paid with two Plains") {
                    short.castSpell(1, "Disenchant", short.findPermanent("Glorious Anthem")!!).error shouldNotBe null
                }

                val paid = board(lands = 3)
                paid.castSpell(1, "Disenchant", paid.findPermanent("Glorious Anthem")!!).error shouldBe null
                paid.resolveStack()
                paid.isInGraveyard(2, "Glorious Anthem") shouldBe true
            }

            test("Callaphe taxes spells targeting itself, once though it is both creature and enchantment") {
                val short = board(lands = 2)
                withClue("{1}{W} + {1} tax can't be paid with two Plains") {
                    short.castSpell(1, "Disenchant", short.findPermanent("Callaphe, Beloved of the Sea")!!).error shouldNotBe null
                }

                val paid = board(lands = 3)
                withClue("a creature enchantment holds one copy of the grant, so the tax is only {1}") {
                    paid.castSpell(1, "Disenchant", paid.findPermanent("Callaphe, Beloved of the Sea")!!).error shouldBe null
                }
            }
        }
    }

    private fun board(lands: Int): TestGame =
        scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(2, "Callaphe, Beloved of the Sea")
            .withCardOnBattlefield(2, "Glorious Anthem")
            .withCardInHand(1, "Disenchant")
            .withLandsOnBattlefield(1, "Plains", lands)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
}
