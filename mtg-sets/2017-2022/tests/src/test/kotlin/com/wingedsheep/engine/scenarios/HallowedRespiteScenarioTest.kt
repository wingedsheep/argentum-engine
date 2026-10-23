package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hallowed Respite (MID) — exile target nonlegendary creature, return it under its owner's
 * control; a +1/+1 counter if it entered under your control, otherwise it's tapped. Flashback.
 */
class HallowedRespiteScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.isTapped(id: EntityId): Boolean = state.getEntity(id)?.has<TappedComponent>() == true

    private fun setup(extra: ScenarioBuilder.() -> ScenarioBuilder): TestGame {
        var b = scenario()
            .withPlayers("Player", "Opponent")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withLandsOnBattlefield(1, "Island", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .extra()
        repeat(5) { b = b.withCardInLibrary(1, "Island") }
        repeat(5) { b = b.withCardInLibrary(2, "Island") }
        return b.build()
    }

    init {
        context("Hallowed Respite") {

            test("your own creature comes back untapped with a +1/+1 counter") {
                val game = setup {
                    withCardInHand(1, "Hallowed Respite").withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                }
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Hallowed Respite", bears).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getController(returned) shouldBe game.player1Id
                game.plusCounters(returned) shouldBe 1
                game.isTapped(returned) shouldBe false
            }

            test("an opponent's creature comes back under its owner's control, tapped, without a counter") {
                val game = setup {
                    withCardInHand(1, "Hallowed Respite").withCardOnBattlefield(2, "Hill Giant")
                }
                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Hallowed Respite", giant).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Hill Giant")!!
                game.state.projectedState.getController(returned) shouldBe game.player2Id
                game.isTapped(returned) shouldBe true
                game.plusCounters(returned) shouldBe 0
            }

            test("flashback casts it from the graveyard and exiles it afterwards") {
                val game = setup {
                    withCardInGraveyard(1, "Hallowed Respite").withCardOnBattlefield(1, "Grizzly Bears")
                }
                val respite = game.findCardsInGraveyard(1, "Hallowed Respite").single()
                val bears = game.findPermanent("Grizzly Bears")!!
                val result = game.execute(
                    CastSpell(
                        game.player1Id, respite, listOf(ChosenTarget.Permanent(bears)),
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.FLASHBACK,
                    )
                )
                withClue("error=${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.plusCounters(game.findPermanent("Grizzly Bears")!!) shouldBe 1
                game.isInExile(1, "Hallowed Respite") shouldBe true
            }
        }
    }
}
