package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ichormoon Gauntlet (ONE #56) — {2}{U} Artifact.
 *
 *   Planeswalkers you control have "[0]: Proliferate" and "[−12]: Take an extra turn after this one."
 *   Whenever you cast a noncreature spell, choose a counter on target permanent. Put an additional
 *   counter of that kind on that permanent.
 *
 * Pins the cast trigger (a player choice among the kinds on the target; no choice with one kind),
 * that creature spells don't trigger it, and both granted loyalty abilities on a planeswalker you
 * control — the −12 only once it can be paid.
 */
class IchormoonGauntletScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Ichormoon Gauntlet")
        .withCardOnBattlefield(1, "Jace Beleren")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, "Divination")
        .withCardInHand(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Island", 3)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun grantedAction(game: TestGame, jace: EntityId, text: String) =
        game.getLegalActions(1).firstOrNull {
            (it.action as? ActivateAbility)?.sourceId == jace && it.description.contains(text)
        }

    init {
        test("a noncreature spell lets you choose a kind on the target and add one of it") {
            val game = board()
            val bears = game.findPermanents("Grizzly Bears").single()
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)
            seed(game, bears, CounterType.STUN, 1)

            game.castSpell(1, "Divination").error shouldBe null
            withClue("The cast trigger asks for its target permanent") {
                game.selectTargets(listOf(bears)).error shouldBe null
            }
            game.resolveStack()

            val decision = game.getPendingDecision() as ChooseOptionDecision
            decision.options.toSet() shouldBe setOf(CounterType.PLUS_ONE_PLUS_ONE.printed, CounterType.STUN.printed)
            game.submitDecision(
                OptionChosenResponse(decision.id, decision.options.indexOf(CounterType.PLUS_ONE_PLUS_ONE.printed))
            ).error shouldBe null
            game.resolveStack()

            count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            count(game, bears, CounterType.STUN) shouldBe 1
        }

        test("a planeswalker with only loyalty counters gets one more with no prompt") {
            val game = board()
            val jace = game.findPermanent("Jace Beleren")!!

            game.castSpell(1, "Divination").error shouldBe null
            game.selectTargets(listOf(jace)).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false

            count(game, jace, CounterType.LOYALTY) shouldBe 4
        }

        test("creature spells don't trigger it") {
            val game = board()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.hasPendingDecision() shouldBe false
            game.state.stack.size shouldBe 1
        }

        test("planeswalkers you control have [0]: Proliferate") {
            val game = board()
            val jace = game.findPermanent("Jace Beleren")!!
            val bears = game.findPermanents("Grizzly Bears").single()
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            val action = grantedAction(game, jace, "Proliferate")!!.action as ActivateAbility
            game.execute(action).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(jace, bears)).error shouldBe null
            game.resolveStack()

            count(game, jace, CounterType.LOYALTY) shouldBe 4
            count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("the granted −12 needs twelve loyalty and takes an extra turn") {
            val game = board()
            val jace = game.findPermanent("Jace Beleren")!!

            (grantedAction(game, jace, "extra turn")?.isAffordable ?: false) shouldBe false

            seed(game, jace, CounterType.LOYALTY, 9)
            val action = grantedAction(game, jace, "extra turn")!!.action as ActivateAbility
            game.execute(action.copy(targets = emptyList<ChosenTarget>())).error shouldBe null
            game.resolveStack()

            game.state.getEntity(game.player2Id)?.get<SkipNextTurnComponent>() shouldNotBe null
        }
    }
}
