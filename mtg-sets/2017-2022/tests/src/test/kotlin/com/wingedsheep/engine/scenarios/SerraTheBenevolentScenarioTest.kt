package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.mtg.sets.definitions.mh1.cards.SerraTheBenevolent
import com.wingedsheep.mtg.sets.definitions.por.cards.WindDrake
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Serra the Benevolent (MH1 #26, {2}{W}{W}, loyalty 4).
 *
 * The −6 emblem is a replacement effect held by an emblem: with a creature in play, a Lightning Bolt
 * that would take its controller from 2 to −1 leaves them at 1, and it keeps working after Serra is
 * gone. The +2 pumps only fliers; the −3 makes a 4/4 flying, vigilance Angel.
 */
class SerraTheBenevolentScenarioTest : ScenarioTestBase() {

    private val name = "Serra the Benevolent"

    init {
        cardRegistry.register(listOf(SerraTheBenevolent, GrizzlyBears, WindDrake))

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("+2 gives only creatures you control with flying +1/+1") {
            val game = board()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Wind Drake")
                .build()
            val serra = game.findPermanent(name)!!
            setLoyalty(game, serra, 4)

            activate(game, serra, index = 0)
            game.resolveStack()

            game.state.projectedState.getPower(game.findPermanent("Wind Drake")!!) shouldBe 3
            game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
            loyalty(game, serra) shouldBe 6
        }

        test("−3 creates a 4/4 white Angel with flying and vigilance") {
            val game = board().build()
            val serra = game.findPermanent(name)!!
            setLoyalty(game, serra, 4)

            activate(game, serra, index = 1)
            game.resolveStack()

            val angel = game.findPermanent("Angel Token")!!
            game.state.projectedState.getPower(angel) shouldBe 4
            game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(angel, Keyword.VIGILANCE) shouldBe true
            loyalty(game, serra) shouldBe 1
        }

        test("−6 emblem holds lethal damage at 1 while you control a creature, after Serra is gone") {
            val game = board()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withLifeTotal(1, 2)
                .build()
            val serra = game.findPermanent(name)!!
            setLoyalty(game, serra, 6)

            activate(game, serra, index = 2)
            game.resolveStack()
            game.findPermanent(name) shouldBe null

            game.passPriority()
            bolt(game, target = game.player1Id)
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 1
        }
    }

    private fun bolt(game: TestGame, target: EntityId) {
        val bolt = game.state.getHand(game.player2Id).single {
            game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
        }
        game.execute(CastSpell(game.player2Id, bolt, listOf(ChosenTarget.Player(target)))).error shouldBe null
    }

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id)
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
