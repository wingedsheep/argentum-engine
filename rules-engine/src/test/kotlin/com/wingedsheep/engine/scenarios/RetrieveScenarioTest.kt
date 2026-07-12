package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Retrieve (VOW #215) — {2}{G} Sorcery.
 *
 *   Return up to one target creature card and up to one target noncreature permanent card from
 *   your graveyard to your hand. Exile Retrieve.
 *
 * Exercises the two independent "up to one target" graveyard slots (creature card at index 0,
 * noncreature permanent card at index 1): targeting both returns both to hand and Retrieve itself
 * ends up exiled (not in the graveyard) as it finishes resolving.
 */
class RetrieveScenarioTest : ScenarioTestBase() {

    init {
        context("Retrieve") {

            test("returns a targeted creature card and a targeted noncreature permanent card to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Retrieve")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Test Enchantment")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val enchantment = game.findCardsInGraveyard(1, "Test Enchantment").single()

                val card = game.findCardsInHand(1, "Retrieve").first()
                game.execute(
                    CastSpell(
                        game.player1Id,
                        card,
                        listOf(
                            ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                            ChosenTarget.Card(enchantment, game.player1Id, Zone.GRAVEYARD),
                        ),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears returned to hand") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("Test Enchantment returned to hand") {
                    game.isInGraveyard(1, "Test Enchantment") shouldBe false
                    game.isInHand(1, "Test Enchantment") shouldBe true
                }
                withClue("Retrieve is exiled rather than going to the graveyard") {
                    game.isInGraveyard(1, "Retrieve") shouldBe false
                    game.isInExile(1, "Retrieve") shouldBe true
                }
            }

            test("skipping both optional targets still exiles Retrieve with nothing returned") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Retrieve")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val card = game.findCardsInHand(1, "Retrieve").first()
                game.execute(CastSpell(game.player1Id, card, emptyList())).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears stays in the graveyard when not targeted") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Retrieve still ends up in exile") {
                    game.isInExile(1, "Retrieve") shouldBe true
                }
            }
        }
    }
}
