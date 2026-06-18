package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * Scenario tests for Hurkyl's Recall (ATQ #10).
 *
 * {1}{U} Instant — "Return all artifacts target player owns to their hand."
 *
 * Proves the new [com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.OwnedByTargetPlayer]
 * predicate: the bounce matches on the artifact's *owner* (not controller), can target either
 * player, and returns each artifact to its owner's hand.
 */
class HurkylsRecallScenarioTest : ScenarioTestBase() {

    init {
        context("Hurkyl's Recall") {

            test("targets an opponent: returns all artifacts that opponent owns, leaves others") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hurkyl's Recall")
                    .withLandsOnBattlefield(1, "Island", 2)
                    // Opponent (player 2) owns and controls two artifacts.
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Millstone")
                    // Caster (player 1) also controls an artifact they own — must NOT be returned.
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oppArtifacts = game.findPermanents("Ornithopter")
                    .filter { game.state.getEntity(it)?.get<OwnerComponent>()?.playerId == game.player2Id } +
                    game.findPermanents("Millstone")
                val ownArtifact = game.findPermanents("Ornithopter")
                    .first { game.state.getEntity(it)?.get<OwnerComponent>()?.playerId == game.player1Id }

                game.castSpellTargetingPlayer(1, "Hurkyl's Recall", 2).error shouldBe null
                game.resolveStack()

                val oppHand = game.state.getZone(game.player2Id, Zone.HAND)
                withClue("both of the opponent's artifacts went to their hand") {
                    oppArtifacts.forEach { oppHand shouldContain it }
                }
                val battlefield = game.state.getBattlefield()
                withClue("the caster's own artifact stays on the battlefield") {
                    battlefield shouldContain ownArtifact
                }
                oppArtifacts.forEach { battlefield shouldNotContain it }
            }

            test("targets yourself: returns the artifacts you own") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hurkyl's Recall")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Millstone")
                    .withCardOnBattlefield(2, "Millstone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ownMillstone = game.findPermanents("Millstone")
                    .first { game.state.getEntity(it)?.get<OwnerComponent>()?.playerId == game.player1Id }
                val oppMillstone = game.findPermanents("Millstone")
                    .first { game.state.getEntity(it)?.get<OwnerComponent>()?.playerId == game.player2Id }

                game.castSpellTargetingPlayer(1, "Hurkyl's Recall", 1).error shouldBe null
                game.resolveStack()

                withClue("your artifact returned to your hand") {
                    game.state.getZone(game.player1Id, Zone.HAND) shouldContain ownMillstone
                }
                withClue("the opponent's artifact is untouched") {
                    game.state.getBattlefield() shouldContain oppMillstone
                }
            }

            test("owns but does not control: an artifact the target owns under another's control is returned to the target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hurkyl's Recall")
                    .withLandsOnBattlefield(1, "Island", 2)
                    // Build the artifact under player 1's control...
                    .withCardOnBattlefield(1, "Millstone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // ...but make player 2 its OWNER (control unchanged — it stays controlled by player 1).
                val millstone = game.findPermanent("Millstone")!!
                game.state = game.state.updateEntity(millstone) { container ->
                    val card = container.get<CardComponent>()!!
                    container
                        .with(card.copy(ownerId = game.player2Id))
                        .with(OwnerComponent(game.player2Id))
                }

                // Targeting player 2 (the owner) must return it, even though player 1 controls it.
                game.castSpellTargetingPlayer(1, "Hurkyl's Recall", 2).error shouldBe null
                game.resolveStack()

                withClue("the artifact left the battlefield") {
                    game.state.getBattlefield() shouldNotContain millstone
                }
                withClue("it returned to its OWNER's hand (player 2), not the controller's") {
                    game.state.getZone(game.player2Id, Zone.HAND) shouldContain millstone
                    game.state.getZone(game.player1Id, Zone.HAND) shouldNotContain millstone
                }
            }

            test("targeting the controller (not owner) does NOT return an artifact owned by someone else") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hurkyl's Recall")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Millstone")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 2 owns it; player 1 controls it.
                val millstone = game.findPermanent("Millstone")!!
                game.state = game.state.updateEntity(millstone) { container ->
                    val card = container.get<CardComponent>()!!
                    container
                        .with(card.copy(ownerId = game.player2Id))
                        .with(OwnerComponent(game.player2Id))
                }

                // Targeting player 1 (the controller, NOT the owner) must leave it alone.
                game.castSpellTargetingPlayer(1, "Hurkyl's Recall", 1).error shouldBe null
                game.resolveStack()

                withClue("owner ≠ controller: targeting the controller does not bounce it") {
                    game.state.getBattlefield() shouldContain millstone
                }
            }
        }
    }
}
