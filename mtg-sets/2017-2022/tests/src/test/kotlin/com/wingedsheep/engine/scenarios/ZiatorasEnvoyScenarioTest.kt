package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ziatora's Envoy (SNC #232) — {1}{B}{R}{G} Creature — Lizard Warrior 5/4.
 *
 *   Trample
 *   Whenever this creature deals combat damage to a player, look at the top card of your library.
 *   You may play a land from the top of your library or cast a spell with mana value less than or
 *   equal to the damage dealt from the top of your library without paying its mana cost. If you
 *   don't, put that card into your hand.
 *   Blitz {2}{B}{R}{G}
 *
 * Covers the damage-bound mana-value cap (read off the trigger's damage amount inside the
 * pipeline's selection), the land half of "play" during resolution, and "If you don't" sending
 * a declined or ineligible card to hand.
 */
class ZiatorasEnvoyScenarioTest : ScenarioTestBase() {

    private fun attackWithEnvoy(top: String): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Ziatora's Envoy", summoningSickness = false)
            .withCardInLibrary(1, top)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Ziatora's Envoy" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareNoBlockers().error shouldBe null
        repeat(20) {
            if (game.getPendingDecision() != null || game.state.step == Step.END_COMBAT) return game
            game.passPriority()
        }
        return game
    }

    private fun TestGame.permanentsNamed(name: String): Int =
        state.getBattlefield(player1Id).count { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        context("Ziatora's Envoy combat-damage trigger") {

            test("a spell with mana value at most the damage dealt is cast for free") {
                val game = attackWithEnvoy(top = "Hill Giant")
                game.getLifeTotal(2) shouldBe 15
                val giant = game.findCardsInLibrary(1, "Hill Giant").single()
                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                choice.playerId shouldBe game.player1Id
                choice.options shouldBe listOf(giant)
                game.selectCards(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant (MV 4 <= 5 damage) was cast without paying its mana cost") {
                    game.findPermanent("Hill Giant") shouldNotBe null
                    game.isInHand(1, "Hill Giant") shouldBe false
                    game.librarySize(1) shouldBe 1
                }
            }

            test("a spell with mana value above the damage dealt can't be cast and goes to hand") {
                val game = attackWithEnvoy(top = "Craw Wurm")
                if (game.getPendingDecision() is SelectCardsDecision) {
                    val wurm = game.findCardsInLibrary(1, "Craw Wurm").single()
                    withClue("Craw Wurm (MV 6 > 5 damage) is not selectable") {
                        game.selectCards(listOf(wurm)).error shouldNotBe null
                    }
                    game.selectCards(emptyList()).error shouldBe null
                }
                game.resolveStack()

                game.findPermanent("Craw Wurm") shouldBe null
                game.isInHand(1, "Craw Wurm") shouldBe true
                game.librarySize(1) shouldBe 1
            }

            test("a land on top is played from the library") {
                val game = attackWithEnvoy(top = "Forest")
                val forest = game.findCardsInLibrary(1, "Forest").single()
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(forest)).error shouldBe null
                game.resolveStack()

                game.permanentsNamed("Forest") shouldBe 1
                game.isInHand(1, "Forest") shouldBe false
                game.librarySize(1) shouldBe 1
            }

            test("declining puts the card into hand") {
                val game = attackWithEnvoy(top = "Hill Giant")
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.findPermanent("Hill Giant") shouldBe null
                game.isInHand(1, "Hill Giant") shouldBe true
                game.librarySize(1) shouldBe 1
            }
        }
    }
}
