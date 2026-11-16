package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Hedonist's Trove (DTK #106).
 *
 * When this enchantment enters, exile target opponent's graveyard.
 * You may play lands from among cards exiled with this enchantment.
 * You may cast spells from among cards exiled with this enchantment. You can't cast more than one
 * spell this way each turn.
 *
 * Pins the three things the script's shape is for: the exile is linked to the Trove (only its own
 * pile is playable), the spell half is capped at one cast per turn, and — per the ruling — the cap
 * is for nonland cards only, so a land play from the pile neither spends nor is blocked by it.
 */
class HedonistsTroveScenarioTest : ScenarioTestBase() {

    init {
        fun opponentExile(game: TestGame): List<EntityId> =
            game.state.getZone(ZoneKey(game.player2Id, Zone.EXILE)).toList()

        fun exiled(game: TestGame, cardName: String): EntityId =
            opponentExile(game).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == cardName
            }

        fun hasLandPlay(game: TestGame, cardName: String): Boolean =
            game.getLegalActions(1).any {
                it.actionType == "PlayLand" && it.description.contains(cardName)
            }

        fun hasCast(game: TestGame, cardName: String): Boolean =
            game.getLegalActions(1).any {
                it.actionType == "CastSpell" && it.description.contains(cardName)
            }

        /**
         * Cast the Trove from hand with an opponent's graveyard of [graveyard] and a card that was
         * already in the opponent's exile (a creature and a land) before the Trove arrived, then resolve the enters trigger.
         */
        fun troveResolved(vararg graveyard: String): TestGame {
            var builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Hedonist's Trove")
                .withLandsOnBattlefield(1, "Swamp", 12)
                .withCardInExile(2, "Muck Rats")
                .withCardInExile(2, "Plains")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
            graveyard.forEach { builder = builder.withCardInGraveyard(2, it) }
            val game = builder
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Hedonist's Trove")
            withClue("casting Hedonist's Trove should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()
            if (game.hasPendingDecision()) game.selectTargets(listOf(game.player2Id))
            game.resolveStack()
            return game
        }

        context("Hedonist's Trove") {

            test("the enters trigger exiles the opponent's whole graveyard, linked to the Trove") {
                val game = troveResolved("Forest", "Scathe Zombies", "Dark Ritual")

                withClue("the opponent's graveyard is empty") { game.graveyardSize(2) shouldBe 0 }
                val trove = game.findPermanent("Hedonist's Trove")!!
                val pile = game.state.getEntity(trove)!!.get<LinkedExileComponent>()!!.exiledIds
                withClue("exactly the graveyard cards are linked — not the card already in exile") {
                    pile shouldContainExactlyInAnyOrder listOf(
                        exiled(game, "Forest"), exiled(game, "Scathe Zombies"), exiled(game, "Dark Ritual")
                    )
                }
            }

            test("lands and spells from the pile are offered, but not other exiled cards") {
                val game = troveResolved("Forest", "Scathe Zombies")

                withClue("you may play lands from among the cards exiled with it") {
                    hasLandPlay(game, "Forest") shouldBe true
                }
                withClue("you may cast spells from among the cards exiled with it") {
                    hasCast(game, "Scathe Zombies") shouldBe true
                }
                withClue("a card the Trove didn't exile is not covered") {
                    hasCast(game, "Muck Rats") shouldBe false
                }
                withClue("a land the Trove didn't exile is not covered") {
                    hasLandPlay(game, "Plains") shouldBe false
                }
            }

            test("only one spell per turn, and a land play is still allowed afterwards") {
                val game = troveResolved("Forest", "Scathe Zombies", "Dark Ritual")

                val cast = game.execute(CastSpell(game.player1Id, exiled(game, "Scathe Zombies"), emptyList()))
                withClue("casting the exiled Scathe Zombies should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()
                withClue("Scathe Zombies resolved under your control") {
                    game.isOnBattlefield("Scathe Zombies") shouldBe true
                }

                withClue("you can't cast more than one spell this way each turn") {
                    hasCast(game, "Dark Ritual") shouldBe false
                }
                withClue("the once-a-turn cap applies only to nonland cards (ruling)") {
                    hasLandPlay(game, "Forest") shouldBe true
                }
                val played = game.execute(PlayLand(game.player1Id, exiled(game, "Forest")))
                withClue("playing the exiled Forest should succeed: ${played.error}") {
                    played.error shouldBe null
                }
                withClue("the Forest is on the battlefield") { game.isOnBattlefield("Forest") shouldBe true }
            }

            test("playing a land from the pile doesn't spend the spell allowance") {
                val game = troveResolved("Forest", "Scathe Zombies")

                val played = game.execute(PlayLand(game.player1Id, exiled(game, "Forest")))
                withClue("playing the exiled Forest should succeed: ${played.error}") {
                    played.error shouldBe null
                }
                withClue("the spell cast is still available this turn") {
                    hasCast(game, "Scathe Zombies") shouldBe true
                }
            }
        }
    }
}
