package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.WarpedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Regression tests for the explicit alternative-cost discriminator
 * ([CastSpell.alternativeCostType]).
 *
 * A card can legally have two alternative costs available at once for the same card+zone — most
 * commonly a battlefield static (Tannuk, Steadfast Second) granting warp to a card in hand that
 * already has evoke. Both legal actions set `useAlternativeCost = true`, so without an explicit
 * discriminator the handler falls back to a fixed priority order (warp > evoke) and silently
 * charges the wrong cost. These tests pin the player's choice down end to end:
 *  - the enumerated "Evoke" and "Cast (Warp)" actions now carry distinct discriminators, and
 *  - executing each charges and behaves as that cost, not the higher-priority one.
 */
class AlternativeCostChoiceScenarioTest : ScenarioTestBase() {

    init {
        // A red creature (so Tannuk's "red creature cards in your hand" grant applies) with a
        // cheap evoke. Defined inline because no real-set evoke creature is registered in the
        // ScenarioTestBase card pool.
        val redEvoker = card("Red Evoker") {
            manaCost = "{4}{R}"
            colorIdentity = "R"
            typeLine = "Creature — Elemental"
            power = 2
            toughness = 2
            oracleText = "Evoke {R}"
            evoke = "{R}"
        }
        cardRegistry.register(redEvoker)

        context("warp grant colliding with evoke") {

            test("the enumerated Evoke and Cast (Warp) actions carry distinct discriminators") {
                val game = warpPlusEvokeBoard()

                val evokerActions = game.getLegalActions(1).filter { info ->
                    (info.action as? CastSpell)?.let { cast ->
                        game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name == "Red Evoker"
                    } == true
                }
                val evokeAction = evokerActions.firstOrNull { it.description.startsWith("Evoke") }
                val warpAction = evokerActions.firstOrNull { it.actionType == "CastWithWarp" }

                withClue("both alternative-cost actions are offered") {
                    evokeAction shouldNotBe null
                    warpAction shouldNotBe null
                }
                withClue("evoke action is tagged EVOKE") {
                    (evokeAction!!.action as CastSpell).alternativeCostType shouldBe AlternativeCostType.EVOKE
                }
                withClue("warp action is tagged WARP") {
                    (warpAction!!.action as CastSpell).alternativeCostType shouldBe AlternativeCostType.WARP
                }
                withClue("the two actions are now distinguishable (the bug was that they were identical)") {
                    (evokeAction!!.action) shouldNotBe (warpAction!!.action)
                }
            }

            test("executing the Evoke action charges the evoke cost ({R}), not warp ({2}{R})") {
                val game = warpPlusEvokeBoard()
                val evokeAction = game.getLegalActions(1).first { info ->
                    info.description.startsWith("Evoke") &&
                        (info.action as? CastSpell)?.let { cast ->
                            game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name == "Red Evoker"
                        } == true
                }.action as CastSpell

                game.execute(evokeAction).error shouldBe null

                val tapped = game.state.getBattlefield(game.player1Id).count { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Mountain" &&
                        game.state.getEntity(id)?.has<TappedComponent>() == true
                }
                withClue("evoke {R} taps exactly one Mountain (warp {2}{R} would tap three)") {
                    tapped shouldBe 1
                }
                withClue("the spell was not treated as warped") {
                    game.state.spellWarpedThisTurn shouldBe false
                }
            }

            test("executing the Cast (Warp) action charges the warp cost ({2}{R}) and marks it warped") {
                val game = warpPlusEvokeBoard()
                val warpAction = game.getLegalActions(1).first { info ->
                    info.actionType == "CastWithWarp" &&
                        (info.action as? CastSpell)?.let { cast ->
                            game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name == "Red Evoker"
                        } == true
                }.action as CastSpell

                game.execute(warpAction).error shouldBe null
                game.resolveStack()

                val tapped = game.state.getBattlefield(game.player1Id).count { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Mountain" &&
                        game.state.getEntity(id)?.has<TappedComponent>() == true
                }
                withClue("warp {2}{R} taps three Mountains") {
                    tapped shouldBe 3
                }
                withClue("the resolved permanent is marked warped and spellWarpedThisTurn is set") {
                    game.state.spellWarpedThisTurn.shouldBeTrue()
                    val evoker = game.findPermanent("Red Evoker")
                    evoker shouldNotBe null
                    game.state.getEntity(evoker!!)?.has<WarpedComponent>() shouldBe true
                }
            }
        }

        // Legacy fallback: an action with no discriminator (older/AI/hand-built callers) still
        // resolves through the priority chain. With only evoke available (no Tannuk), the
        // null-discriminator alt cast is unambiguously evoke.
        test("a null-discriminator alternative cast still resolves (legacy priority fallback)") {
            var builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Red Evoker")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(5) { builder = builder.withCardInLibrary(2, "Mountain") }
            val game = builder.build()

            // castSpellWithAlternativeCost builds CastSpell(useAlternativeCost = true) with no type.
            game.castSpellWithAlternativeCost(1, "Red Evoker").error shouldBe null
            val tapped = game.state.getBattlefield(game.player1Id).count { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Mountain" &&
                    game.state.getEntity(id)?.has<TappedComponent>() == true
            }
            withClue("legacy path falls back to evoke {R} (the only alt cost available)") {
                tapped shouldBe 1
            }
        }
    }

    private fun warpPlusEvokeBoard(): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Tannuk, Steadfast Second", summoningSickness = false)
            .withCardInHand(1, "Red Evoker")
            .withLandsOnBattlefield(1, "Mountain", 3) // enough for warp {2}{R}
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { builder = builder.withCardInLibrary(2, "Mountain") }
        return builder.build()
    }
}
