package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Can't Stay Away (MID #213) — "Return target creature card with mana value 3 or less from your
 * graveyard to the battlefield. It gains 'If this creature would die, exile it instead.'
 * Flashback {3}{W}{B}"
 */
class CantStayAwayScenarioTest : ScenarioTestBase() {

    private fun TestGame.castOnGraveyardCard(fromGraveyard: Boolean, creature: EntityId) =
        execute(
            CastSpell(
                player1Id,
                (if (fromGraveyard) state.getGraveyard(player1Id) else state.getHand(player1Id))
                    .first { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Can't Stay Away" },
                listOf(ChosenTarget.Card(creature, player1Id, Zone.GRAVEYARD))
            )
        )

    private fun TestGame.graveyardCard(name: String): EntityId =
        state.getGraveyard(player1Id).first {
            state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == name
        }

    init {
        context("Can't Stay Away") {

            test("returns the creature, which is exiled instead of dying — even on a later turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Can't Stay Away")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.graveyardCard("Grizzly Bears")
                game.castOnGraveyardCard(fromGraveyard = false, creature = bears).error shouldBe null
                game.resolveStack()
                game.findPermanent("Grizzly Bears") shouldNotBe null

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.castSpell(2, "Lightning Bolt", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }

            test("a sacrificed returned creature is exiled too") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Can't Stay Away")
                    .withCardInHand(1, "Sacrifice")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castOnGraveyardCard(fromGraveyard = false, creature = game.graveyardCard("Grizzly Bears")).error shouldBe null
                game.resolveStack()
                val bears = game.findPermanent("Grizzly Bears")!!

                val sacrifice = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Sacrifice"
                }
                game.execute(
                    CastSpell(game.player1Id, sacrifice, emptyList(),
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)))
                ).error shouldBe null

                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }

            test("flashback from the graveyard returns a creature and exiles Can't Stay Away") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Can't Stay Away")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castOnGraveyardCard(fromGraveyard = true, creature = game.graveyardCard("Grizzly Bears")).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldNotBe null
                game.isInExile(1, "Can't Stay Away") shouldBe true
            }

            test("can't target a creature card with mana value 4 or more") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Can't Stay Away")
                    .withCardInGraveyard(1, "Gurmag Angler")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castOnGraveyardCard(fromGraveyard = false, creature = game.graveyardCard("Gurmag Angler")).error shouldNotBe null
            }
        }
    }
}
