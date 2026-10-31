package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mu Yanling, Sky Dancer (M20 #68, {1}{U}{U}, Loyalty 2).
 *
 * The +2's -2/-0 and flying loss outlast the turn it was used in (until your next turn); the −3
 * makes a 4/4 flier; the −8 emblem gives Islands you control "{T}: Draw a card."
 */
class MuYanlingSkyDancerScenarioTest : ScenarioTestBase() {

    private val name = "Mu Yanling, Sky Dancer"

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
                .without<AbilityActivatedThisTurnComponent>()
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities[index]
        game.execute(ActivateAbility(game.player1Id, source, ability.id, targets = targets)).error shouldBe null
    }

    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, name)
            .withCardOnBattlefield(1, "Island")
            .withCardOnBattlefield(2, "Serra Angel")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("+2: target creature gets -2/-0 and loses flying until your next turn") {
            val game = board().build()
            val mu = game.findPermanent(name)!!
            val angel = game.findPermanent("Serra Angel")!!

            activate(game, mu, 0, listOf(ChosenTarget.Permanent(angel)))
            game.resolveStack()

            withClue("Angel is 2/4 without flying") {
                game.state.projectedState.getPower(angel) shouldBe 2
                game.state.projectedState.getToughness(angel) shouldBe 4
                game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe false
            }

            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
            withClue("it is now the opponent's turn") { game.state.activePlayerId shouldBe game.player2Id }
            withClue("the effect still applies during the opponent's turn") {
                game.state.projectedState.getPower(angel) shouldBe 2
                game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe false
            }
        }

        test("+2 with no target chosen still adds loyalty") {
            val game = board().build()
            val mu = game.findPermanent(name)!!
            seedLoyalty(game, mu, 2)

            activate(game, mu, 0)
            game.resolveStack()

            game.state.getEntity(mu)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 4
        }

        test("−3 creates a 4/4 blue Elemental Bird with flying") {
            val game = board().build()
            val mu = game.findPermanent(name)!!
            seedLoyalty(game, mu, 3)

            activate(game, mu, 1)
            game.resolveStack()

            val birds = game.findPermanents("Elemental Bird Token")
            birds shouldHaveSize 1
            val bird = birds.single()
            game.state.projectedState.getPower(bird) shouldBe 4
            game.state.projectedState.getToughness(bird) shouldBe 4
            game.state.projectedState.hasKeyword(bird, Keyword.FLYING) shouldBe true
        }

        test("−8 emblem: Islands you control have '{T}: Draw a card.'") {
            val game = board().build()
            val mu = game.findPermanent(name)!!
            seedLoyalty(game, mu, 8)

            activate(game, mu, 2)
            game.resolveStack()

            val island = game.findPermanent("Island")!!
            val granted = game.getLegalActions(1).firstOrNull { legal ->
                (legal.action as? ActivateAbility)?.sourceId == island &&
                    legal.description.contains("Draw a card")
            }.shouldNotBeNull()

            val handBefore = game.handSize(1)
            game.execute(granted.action.shouldBeInstanceOf<ActivateAbility>()).error shouldBe null
            game.resolveStack()

            withClue("the Island tapped and you drew a card") {
                game.state.getEntity(island)!!.has<TappedComponent>() shouldBe true
                game.handSize(1) shouldBe handBefore + 1
            }
        }
    }
}
