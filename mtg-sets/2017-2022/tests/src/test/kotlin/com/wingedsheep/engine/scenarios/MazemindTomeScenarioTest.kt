package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mazemind Tome — the "{T}, Put a page counter on this artifact:" cost
 * ([com.wingedsheep.sdk.dsl.Costs.PutCounterOnSelf]) plus the state-triggered
 * "four or more page counters → exile it, gain 4 life".
 *
 * The cost is the novel piece: unlike every other cost it *adds* something, so it never runs
 * out, and it is paid whether or not the ability resolves.
 */
class MazemindTomeScenarioTest : ScenarioTestBase() {

    private val scryAbilityId by lazy {
        cardRegistry.requireCard("Mazemind Tome").activatedAbilities[0].id
    }
    private val drawAbilityId by lazy {
        cardRegistry.requireCard("Mazemind Tome").activatedAbilities[1].id
    }

    private fun TestGame.pageCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PAGE) ?: 0

    private fun TestGame.seedPageCounters(id: EntityId, count: Int) {
        state = state.updateEntity(id) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.PAGE, count))
        }
    }

    init {
        test("the scry ability taps the Tome and puts a page counter on it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mazemind Tome")
                .withCardInLibrary(1, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tome = game.findPermanent("Mazemind Tome")!!
            game.pageCounters(tome) shouldBe 0

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = tome, abilityId = scryAbilityId)
            ).error shouldBe null

            // The counter is part of the cost, so it lands immediately — before resolution.
            game.pageCounters(tome) shouldBe 1
            game.state.getEntity(tome)!!.has<TappedComponent>() shouldBe true
        }

        test("the draw ability costs {2} and also accrues a page counter") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mazemind Tome")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tome = game.findPermanent("Mazemind Tome")!!
            val handBefore = game.handSize(1)

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = tome, abilityId = drawAbilityId)
            ).error shouldBe null
            game.resolveStack()

            game.pageCounters(tome) shouldBe 1
            game.handSize(1) shouldBe handBefore + 1
        }

        test("the draw ability is unaffordable without the {2}") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mazemind Tome")
                .withCardInLibrary(1, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tome = game.findPermanent("Mazemind Tome")!!
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = tome, abilityId = drawAbilityId)
            ).error shouldNotBe null
            game.pageCounters(tome) shouldBe 0
        }

        test("the fourth page counter exiles the Tome and gains 4 life") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mazemind Tome")
                .withCardInLibrary(1, "Mountain")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLifeTotal(1, 20)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tome = game.findPermanent("Mazemind Tome")!!
            // Three already there; the activation's own cost counter is the fourth. The draw
            // ability is used rather than scry so resolution doesn't pause for a scry decision.
            game.seedPageCounters(tome, 3)

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = tome, abilityId = drawAbilityId)
            ).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Mazemind Tome") shouldBe true
            game.isOnBattlefield("Mazemind Tome") shouldBe false
            game.getLifeTotal(1) shouldBe 24
        }

        test("the state trigger stays quiet below four counters") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mazemind Tome")
                .withCardInLibrary(1, "Mountain")
                .withLifeTotal(1, 20)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tome = game.findPermanent("Mazemind Tome")!!
            game.seedPageCounters(tome, 2)

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = tome, abilityId = scryAbilityId)
            ).error shouldBe null
            game.resolveStack()

            game.pageCounters(tome) shouldBe 3
            game.isOnBattlefield("Mazemind Tome") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
