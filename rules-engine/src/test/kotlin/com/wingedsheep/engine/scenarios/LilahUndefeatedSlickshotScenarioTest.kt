package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Lilah, Undefeated Slickshot (OTJ) — {1}{U}{R} Human Rogue, 3/3.
 *
 * "Prowess.
 * Whenever you cast a multicolored instant or sorcery spell from your hand, exile that spell
 * instead of putting it into your graveyard as it resolves. If you do, it becomes plotted."
 *
 * The second ability re-routes the triggering spell's post-resolution destination to exile and
 * makes it plotted (the plot sibling of Goliath Daydreamer's dream-counter ability). Per the
 * engine's `onlyIfResolved` semantics, the spell resolves fully first; only if it actually
 * resolves does it become plotted.
 */
class LilahUndefeatedSlickshotScenarioTest : ScenarioTestBase() {

    init {
        test("a multicolored instant cast from hand resolves, then is exiled and becomes plotted instead of going to the graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lilah, Undefeated Slickshot")
                .withCardInHand(1, "Lightning Helix")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val result = game.castSpellTargetingPlayer(1, "Lightning Helix", targetPlayerNumber = 2)
            withClue("Casting Lightning Helix should succeed: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()

            withClue("Lightning Helix resolved normally first (3 damage to Player2)") {
                game.getLifeTotal(2) shouldBe 17
            }
            withClue("As it resolves the spell is exiled, not put into the graveyard") {
                namesInExile(game, 1) shouldBe setOf("Lightning Helix")
                namesInGraveyard(game, 1) shouldBe emptySet()
            }
            withClue("The exiled card became plotted") {
                val helix = game.state.getExile(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Helix"
                }
                game.state.getEntity(helix)?.get<PlottedComponent>() shouldNotBe null
            }
        }

        test("prowess pumps Lilah when you cast a noncreature spell") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lilah, Undefeated Slickshot", summoningSickness = false)
                .withCardInHand(1, "Lightning Helix")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lilah = game.findPermanent("Lilah, Undefeated Slickshot")!!
            game.castSpellTargetingPlayer(1, "Lightning Helix", targetPlayerNumber = 2).error shouldBe null
            // Resolve the prowess trigger and the spell; the +1/+1 lasts until end of turn.
            game.resolveStack()

            withClue("Prowess: Lilah is 4/4 until end of turn after a noncreature cast") {
                game.state.projectedState.getPower(lilah) shouldBe 4
                game.state.projectedState.getToughness(lilah) shouldBe 4
            }
        }

        test("a monocolored instant cast from hand is NOT exiled — it goes to the graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lilah, Undefeated Slickshot")
                .withCardInHand(1, "Volcanic Hammer")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Volcanic Hammer", targetPlayerNumber = 2).error shouldBe null
            game.resolveStack()

            withClue("Monocolored spell doesn't trigger Lilah — it goes to the graveyard normally") {
                namesInGraveyard(game, 1) shouldBe setOf("Volcanic Hammer")
                namesInExile(game, 1) shouldBe emptySet()
            }
        }
    }

    private fun namesInExile(game: TestGame, playerNumber: Int): Set<String> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId).mapNotNull {
            game.state.getEntity(it)?.get<CardComponent>()?.name
        }.toSet()
    }

    private fun namesInGraveyard(game: TestGame, playerNumber: Int): Set<String> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getGraveyard(playerId).mapNotNull {
            game.state.getEntity(it)?.get<CardComponent>()?.name
        }.toSet()
    }
}
