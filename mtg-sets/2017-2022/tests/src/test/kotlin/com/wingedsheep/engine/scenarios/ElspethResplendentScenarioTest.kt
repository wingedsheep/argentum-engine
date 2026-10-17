package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.snc.cards.ElspethResplendent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Elspeth Resplendent (SNC #11). The +1 asks for the keyword-counter kind at resolution and puts it
 * on the target alongside a +1/+1 counter; the −3 lets only a permanent card with mana value 3 or
 * less onto the battlefield, and it arrives carrying a shield counter.
 */
class ElspethResplendentScenarioTest : ScenarioTestBase() {

    private val plusOne = ElspethResplendent.activatedAbilities[0].id
    private val minusThree = ElspethResplendent.activatedAbilities[1].id
    private val minusSeven = ElspethResplendent.activatedAbilities[2].id

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    init {
        test("+1 puts a +1/+1 counter and the chosen keyword counter on the target") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Elspeth Resplendent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elspeth = game.findPermanent("Elspeth Resplendent")!!
            seedLoyalty(game, elspeth, 5)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = elspeth,
                    abilityId = plusOne,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            val choice = game.getPendingDecision() as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(choice.id, choice.options.indexOf("Lifelink counter")))
                .error shouldBe null

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            counters(game, bears, CounterType.LIFELINK) shouldBe 1
            counters(game, bears, CounterType.FLYING) shouldBe 0
            game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            counters(game, elspeth, CounterType.LOYALTY) shouldBe 6
        }

        test("−3 puts only a permanent card with mana value 3 or less onto the battlefield, with a shield counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Elspeth Resplendent")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Lightning Bolt")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elspeth = game.findPermanent("Elspeth Resplendent")!!
            seedLoyalty(game, elspeth, 5)
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            val giant = game.findCardsInLibrary(1, "Hill Giant").single()
            val bolt = game.findCardsInLibrary(1, "Lightning Bolt").single()

            game.execute(ActivateAbility(game.player1Id, elspeth, minusThree)).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as SelectCardsDecision
            decision.options shouldContain bears
            decision.options shouldNotContain giant
            decision.options shouldNotContain bolt
            game.selectCards(listOf(bears)).error shouldBe null

            game.state.getZone(game.player1Id, Zone.BATTLEFIELD) shouldContain bears
            counters(game, bears, CounterType.SHIELD) shouldBe 1
            game.state.getZone(game.player1Id, Zone.LIBRARY).toSet() shouldBe setOf(giant, bolt)
            counters(game, elspeth, CounterType.LOYALTY) shouldBe 2
        }

        test("−7 creates five 3/3 white flying Angels") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Elspeth Resplendent")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elspeth = game.findPermanent("Elspeth Resplendent")!!
            seedLoyalty(game, elspeth, 7)

            game.execute(ActivateAbility(game.player1Id, elspeth, minusSeven)).error shouldBe null
            game.resolveStack()

            val angels = game.findAllPermanents("Angel Token")
            angels.size shouldBe 5
            angels.forEach { game.state.projectedState.hasKeyword(it, Keyword.FLYING) shouldBe true }
        }
    }
}
