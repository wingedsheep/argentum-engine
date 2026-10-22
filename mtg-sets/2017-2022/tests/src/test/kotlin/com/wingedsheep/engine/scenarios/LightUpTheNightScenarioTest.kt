package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Light Up the Night (MID) — X damage to any target, X plus 1 to a creature or planeswalker. Its
 * flashback {3}{R} removes X loyalty counters from among your planeswalkers, and that X (never 0)
 * is the spell's X.
 */
class LightUpTheNightScenarioTest : ScenarioTestBase() {

    private fun TestGame.loyalty(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun builder(inGraveyard: Boolean) = scenario()
        .withPlayers("Player", "Opponent")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .let { if (inGraveyard) it.withCardInGraveyard(1, "Light Up the Night") else it.withCardInHand(1, "Light Up the Night") }
        .let { b -> var r = b; repeat(5) { r = r.withCardInLibrary(1, "Mountain").withCardInLibrary(2, "Mountain") }; r }

    init {
        context("Light Up the Night") {

            test("from hand it deals X to a player and X plus 1 to a creature") {
                val game = builder(inGraveyard = false).build()
                val spell = game.state.getHand(game.player1Id).single()
                game.execute(CastSpell(game.player1Id, spell, xValue = 2,
                    targets = listOf(ChosenTarget.Player(game.player2Id)))).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 18
            }

            test("from hand X plus 1 damage kills a creature X alone would not") {
                val game = builder(inGraveyard = false).build()
                val spell = game.state.getHand(game.player1Id).single()
                val giant = game.findPermanent("Hill Giant")!!
                game.execute(CastSpell(game.player1Id, spell, xValue = 2,
                    targets = listOf(ChosenTarget.Permanent(giant)))).error shouldBe null
                game.resolveStack()
                withClue("2 + 1 damage kills the 3/3 Hill Giant") { game.findPermanent("Hill Giant") shouldBe null }
            }

            test("flashback removes X loyalty counters from among your planeswalkers and deals that X") {
                val game = builder(inGraveyard = true)
                    .withCardOnBattlefield(1, "Teferi, Who Slows the Sunset")
                    .withCardOnBattlefield(1, "Wrenn and Seven")
                    .build()
                val spell = game.findCardsInGraveyard(1, "Light Up the Night").single()
                val teferi = game.findPermanent("Teferi, Who Slows the Sunset")!!
                val wrenn = game.findPermanent("Wrenn and Seven")!!

                val offer = game.getLegalActions(1).single {
                    it.actionType == "CastWithFlashback" && (it.action as CastSpell).cardId == spell
                }
                withClue("X ranges 1..9 over the 4 + 5 loyalty you control") {
                    offer.isAffordable shouldBe true
                    offer.hasXCost shouldBe true
                    offer.minX shouldBe 1
                    offer.maxAffordableX shouldBe 9
                }

                val result = game.execute(CastSpell(
                    game.player1Id, spell, xValue = 3,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK,
                    additionalCostPayment = AdditionalCostPayment(distributedCounterRemovals = listOf(
                        DistributedCounterRemoval(teferi, "LOYALTY", 2),
                        DistributedCounterRemoval(wrenn, "LOYALTY", 1),
                    )),
                ))
                withClue("error=${result.error}") { result.error shouldBe null }
                game.loyalty(teferi) shouldBe 2
                game.loyalty(wrenn) shouldBe 4

                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
                game.isInExile(1, "Light Up the Night") shouldBe true
            }

            test("flashback can't be cast with X = 0, so it needs a planeswalker with loyalty") {
                val game = builder(inGraveyard = true).build()
                val spell = game.findCardsInGraveyard(1, "Light Up the Night").single()
                game.getLegalActions(1).filter {
                    it.actionType == "CastWithFlashback" && (it.action as CastSpell).cardId == spell
                }.none { it.isAffordable } shouldBe true
                game.execute(CastSpell(
                    game.player1Id, spell, xValue = 0,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK,
                )).error shouldNotBe null
                game.isInGraveyard(1, "Light Up the Night") shouldBe true
            }
        }
    }
}
