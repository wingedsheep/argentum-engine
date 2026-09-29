package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Crystal Carapace (MOM #183) — {3}{G} Enchantment — Aura.
 * "Enchant creature. Enchanted creature gets +3/+3 and has ward {2}. Cycling {2}"
 */
class CrystalCarapaceScenarioTest : ScenarioTestBase() {

    init {
        context("Crystal Carapace") {

            test("enchanted creature gets +3/+3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Crystal Carapace", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val id = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(id) shouldBe 5
                game.state.projectedState.getToughness(id) shouldBe 5
            }

            test("ward {2} counters an opponent's removal spell they can't pay for") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(2, "Crystal Carapace", "Grizzly Bears")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Doom Blade", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                withClue("the ward trigger goes on the stack above Doom Blade") {
                    game.state.stack.size shouldBe 2
                }
                game.resolveStack()

                withClue("Doom Blade was countered, so the Bears survive") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                    game.isInGraveyard(1, "Doom Blade") shouldBe true
                }
            }

            test("without the Aura the same removal spell resolves (control)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Doom Blade", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.state.stack.size shouldBe 1
                game.resolveStack()
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("cycling {2} discards it and draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Crystal Carapace")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Crystal Carapace").error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Crystal Carapace") shouldBe true
                game.isInHand(1, "Forest") shouldBe true
            }
        }
    }
}
