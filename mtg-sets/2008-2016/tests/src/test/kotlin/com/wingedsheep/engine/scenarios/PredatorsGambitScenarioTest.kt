package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Predator's Gambit (AVR #117) — {B} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +2/+1.
 *   Enchanted creature has intimidate as long as its controller controls no other creatures.
 *
 * "Its controller" is the enchanted creature's controller, which need not be the Aura's.
 */
class PredatorsGambitScenarioTest : ScenarioTestBase() {
    init {
        test("cast on your only creature: +2/+1 and intimidate") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Predator's Gambit")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Predator's Gambit", bears).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears gets +2/+1") {
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 3
            }
            withClue("its controller controls no other creatures, so it has intimidate") {
                game.state.projectedState.hasKeyword(bears, Keyword.INTIMIDATE) shouldBe true
            }
        }

        test("no intimidate while its controller controls another creature") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardAttachedTo(1, "Predator's Gambit", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("the stat bonus still applies") {
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 3
            }
            withClue("Hill Giant is another creature, so no intimidate") {
                game.state.projectedState.hasKeyword(bears, Keyword.INTIMIDATE) shouldBe false
            }
        }

        test("on an opponent's lone creature it checks that creature's controller, not the Aura's") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardAttachedTo(1, "Predator's Gambit", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("P2 controls no other creatures, so P1's Hill Giant doesn't matter") {
                game.state.projectedState.hasKeyword(bears, Keyword.INTIMIDATE) shouldBe true
            }
        }

        test("on an opponent's creature alongside another of theirs: no intimidate") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardAttachedTo(1, "Predator's Gambit", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("P2 controls Hill Giant as well, so no intimidate even though P1 controls none") {
                game.state.projectedState.hasKeyword(bears, Keyword.INTIMIDATE) shouldBe false
            }
        }
    }
}
