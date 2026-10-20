package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for All Hallow's Eve (LEG #88).
 *
 * {2}{B}{B} Sorcery
 * "Exile All Hallow's Eve with two scream counters on it.
 *  At the beginning of your upkeep, if this card is exiled with a scream counter on it, remove a
 *  scream counter from it. If there are no more scream counters on it, put it into your graveyard
 *  and each player returns all creature cards from their graveyard to the battlefield."
 *
 * Three things carry the card: the spell lands in exile (not the graveyard) with two counters,
 * the upkeep trigger functions from exile and counts down over two of the caster's upkeeps, and
 * the intervening "if" keeps a counter-less exiled copy inert.
 */
class AllHallowsEveScenarioTest : ScenarioTestBase() {

    init {
        context("All Hallow's Eve") {

            fun TestGame.screamCounters(): Int {
                val id = state.getExile(player1Id).single { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.name == "All Hallow's Eve"
                }
                return state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.SCREAM) ?: 0
            }

            /** Round the table from the caster's turn to the caster's next upkeep. */
            fun TestGame.toMyNextUpkeep() {
                passUntilPhase(Phase.ENDING, Step.END)
                passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                state.activePlayerId shouldBe player2Id
                passUntilPhase(Phase.ENDING, Step.END)
                passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                state.activePlayerId shouldBe player1Id
                resolveStack()
            }

            test("exiles itself with two scream counters, then returns every graveyard creature two upkeeps later") {
                val game = scenario()
                    .withPlayers("Necromancer", "Opponent")
                    .withCardInHand(1, "All Hallow's Eve")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInGraveyard(2, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "All Hallow's Eve").error shouldBe null
                game.resolveStack()

                withClue("resolving only exiles it with two scream counters") {
                    game.isInExile(1, "All Hallow's Eve") shouldBe true
                    game.isInGraveyard(1, "All Hallow's Eve") shouldBe false
                    game.screamCounters() shouldBe 2
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }

                game.toMyNextUpkeep()
                withClue("first upkeep: one counter removed, nothing returns yet") {
                    game.screamCounters() shouldBe 1
                    game.isInExile(1, "All Hallow's Eve") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }

                game.toMyNextUpkeep()
                withClue("last counter removed: the card goes to its owner's graveyard") {
                    game.isInExile(1, "All Hallow's Eve") shouldBe false
                    game.isInGraveyard(1, "All Hallow's Eve") shouldBe true
                }
                withClue("each player's creature cards return under their owner's control") {
                    val bears = game.findPermanent("Grizzly Bears")!!
                    val giant = game.findPermanent("Hill Giant")!!
                    game.state.getEntity(bears)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                    game.state.getEntity(giant)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
                }
                withClue("non-creature cards stay in the graveyard") {
                    game.isInGraveyard(2, "Swamp") shouldBe true
                }
            }

            test("an exiled copy with no scream counters never triggers") {
                val game = scenario()
                    .withPlayers("Necromancer", "Opponent")
                    .withCardInExile(1, "All Hallow's Eve")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id

                withClue("the intervening 'if' fails, so nothing goes on the stack") {
                    game.state.stack.isEmpty() shouldBe true
                }
                game.resolveStack()
                game.isInExile(1, "All Hallow's Eve") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
        }
    }
}
