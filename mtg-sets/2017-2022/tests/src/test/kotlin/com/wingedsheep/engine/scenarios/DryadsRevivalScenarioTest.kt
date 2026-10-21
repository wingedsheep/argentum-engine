package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dryad's Revival (MID #183) — "Return target card from your graveyard to your hand.
 * Flashback {4}{G}"
 */
class DryadsRevivalScenarioTest : ScenarioTestBase() {

    init {
        context("Dryad's Revival") {

            test("returns any card from your graveyard to your hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dryad's Revival")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Dryad's Revival", 1, "Lightning Bolt").error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Lightning Bolt") shouldBe true
                game.isInGraveyard(1, "Dryad's Revival") shouldBe true
            }

            test("can't target a card in an opponent's graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dryad's Revival")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInGraveyard(2, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Dryad's Revival", 2, "Lightning Bolt").error shouldNotBe null
            }

            test("flashback from the graveyard returns another card and exiles itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Dryad's Revival")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val revival = game.findCardsInGraveyard(1, "Dryad's Revival").single()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.execute(
                    CastSpell(game.player1Id, revival, listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)))
                ).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                withClue("flashback exiles the card on resolution") {
                    game.isInExile(1, "Dryad's Revival") shouldBe true
                }
            }
        }
    }
}
