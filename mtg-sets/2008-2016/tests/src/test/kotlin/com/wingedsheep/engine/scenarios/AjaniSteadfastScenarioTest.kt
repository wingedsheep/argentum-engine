package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ajani Steadfast (M15 #1, {3}{W}, loyalty 4).
 *
 * The −7 emblem holds a "prevent all but 1" damage prevention of its own, covering its owner and the
 * planeswalkers they control but not their creatures, and it keeps working after Ajani is gone. The
 * +1 may target nothing; the −2's loyalty counters skip Ajani itself.
 */
class AjaniSteadfastScenarioTest : ScenarioTestBase() {

    private val name = "Ajani Steadfast"

    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("+1 gives the target creature +1/+1, first strike, vigilance and lifelink") {
            val game = board().withCardOnBattlefield(1, "Grizzly Bears").build()
            val ajani = game.findPermanent(name)!!
            val bears = game.findPermanent("Grizzly Bears")!!
            setLoyalty(game, ajani, 4)

            activate(game, ajani, index = 0, targets = listOf(ChosenTarget.Permanent(bears)))
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
            projected.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
            projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            loyalty(game, ajani) shouldBe 5
        }

        test("+1 can be activated with no target") {
            val game = board().build()
            val ajani = game.findPermanent(name)!!
            setLoyalty(game, ajani, 4)

            activate(game, ajani, index = 0)
            game.resolveStack()

            loyalty(game, ajani) shouldBe 5
        }

        test("−2 puts a +1/+1 counter on each creature and a loyalty counter on each other planeswalker") {
            val game = board()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Ajani Goldmane")
                .build()
            val ajani = game.findPermanent(name)!!
            val goldmane = game.findPermanent("Ajani Goldmane")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            setLoyalty(game, ajani, 4)
            setLoyalty(game, goldmane, 4)

            activate(game, ajani, index = 1)
            game.resolveStack()

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            loyalty(game, goldmane) shouldBe 5
            withClue("\"each other planeswalker\" — Ajani only pays its −2") { loyalty(game, ajani) shouldBe 2 }
        }

        test("−7 emblem leaves you only 1 of a Lightning Bolt, after Ajani is gone") {
            val game = withEmblem(board())

            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 19
        }

        test("−7 emblem leaves a planeswalker you control only 1 of a Lightning Bolt") {
            val game = withEmblem(board().withCardOnBattlefield(1, "Ajani Goldmane")) { g ->
                setLoyalty(g, g.findPermanent("Ajani Goldmane")!!, 4)
            }
            val goldmane = game.findPermanent("Ajani Goldmane")!!

            game.castSpell(2, "Lightning Bolt", goldmane).error shouldBe null
            game.resolveStack()

            loyalty(game, goldmane) shouldBe 3
        }

        test("−7 emblem does not protect creatures you control") {
            val game = withEmblem(board().withCardOnBattlefield(1, "Colossal Dreadmaw"))
            val dreadmaw = game.findPermanent("Colossal Dreadmaw")!!

            game.castSpell(2, "Lightning Bolt", dreadmaw).error shouldBe null
            game.resolveStack()

            game.state.getEntity(dreadmaw)?.get<DamageComponent>()?.amount shouldBe 3
        }
    }

    /** Activates Ajani's −7 from 7 loyalty, then hands priority to the opponent holding a Bolt. */
    private fun withEmblem(
        builder: ScenarioBuilder,
        setup: (TestGame) -> Unit = {}
    ): TestGame {
        val game = builder
            .withCardInHand(2, "Lightning Bolt")
            .withLandsOnBattlefield(2, "Mountain", 1)
            .build()
        setup(game)
        val ajani = game.findPermanent(name)!!
        setLoyalty(game, ajani, 7)

        activate(game, ajani, index = 2)
        game.resolveStack()
        game.findPermanent(name) shouldBe null

        game.passPriority()
        return game
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id, targets = targets)
        ).error shouldBe null
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun loyalty(game: TestGame, id: EntityId): Int = counters(game, id, CounterType.LOYALTY)

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
