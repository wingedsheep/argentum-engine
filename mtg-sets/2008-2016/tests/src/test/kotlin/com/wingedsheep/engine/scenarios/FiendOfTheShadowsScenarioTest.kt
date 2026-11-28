package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Fiend of the Shadows (DKA #62) — {3}{B}{B} Creature — Vampire Wizard 3/3.
 *
 *   Flying
 *   Whenever this creature deals combat damage to a player, that player exiles a card from their
 *   hand. You may play that card for as long as it remains exiled.
 *   Sacrifice a Human: Regenerate this creature.
 *
 * The two halves of the trigger act for different players: the *damaged* player chooses which card
 * leaves their hand, and the Fiend's *controller* gets the permanent play permission on it.
 */
class FiendOfTheShadowsScenarioTest : ScenarioTestBase() {

    init {
        context("Fiend of the Shadows") {

            test("the damaged player picks the card to exile and the Fiend's controller may cast it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Fiend of the Shadows")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Mountain")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Fiend of the Shadows" to 2)).error shouldBe null

                var guard = 0
                while (game.state.pendingDecision == null && guard++ < 30) {
                    if (game.state.step == Step.DECLARE_BLOCKERS && game.state.priorityPlayerId == game.player2Id) {
                        game.declareNoBlockers()
                    }
                    game.passPriority()
                }

                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("'that player exiles a card from their hand' — the damaged player chooses") {
                    decision.playerId shouldBe game.player2Id
                }
                game.getLifeTotal(2) shouldBe 17

                val bears = game.findCardsInHand(2, "Grizzly Bears").single()
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the chosen card is exiled; the other stays in hand") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInHand(2, "Mountain") shouldBe true
                }

                val permission = game.state.mayPlayPermissions.single { bears in it.cardIds }
                withClue("the Fiend's controller gets the play permission, for as long as the card stays exiled") {
                    permission.controllerId shouldBe game.player1Id
                    permission.permanent shouldBe true
                }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.execute(CastSpell(game.player1Id, bears, emptyList())).error shouldBe null
                game.resolveStack()

                withClue("the stolen Grizzly Bears enters under Alice's control") {
                    val onBattlefield = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                    game.state.projectedState.getController(onBattlefield) shouldBe game.player1Id
                }
            }
        }
    }
}
