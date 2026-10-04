package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent
import com.wingedsheep.engine.state.components.player.LifeGainedThisTurnComponent
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
 * Sorin of House Markov // Sorin, Ravenous Neonate (MH3 #245).
 *
 * Front: lifelink, extort; at the beginning of each of your postcombat main phases, if you gained 3
 * or more life this turn, exile Sorin and return him transformed.
 * Back: extort; +2 Food; −1 damage equal to life gained this turn to any target; −6 steal a
 * creature, make it a Vampire, and give it a lifelink counter if you control another white permanent.
 */
class SorinOfHouseMarkovScenarioTest : ScenarioTestBase() {

    private val front = "Sorin of House Markov"
    private val back = "Sorin, Ravenous Neonate"

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun abilityId(index: Int) = cardRegistry.requireCard(back).script.activatedAbilities[index].id

    private fun gainedLifeThisTurn(game: TestGame, amount: Int) {
        game.state = game.state.updateEntity(game.player1Id) { c ->
            c.with(LifeGainedThisTurnComponent).with(LifeGainedAmountThisTurnComponent(amount))
        }
    }

    /** Answer any extort prompt with [pay], auto-pay its mana, and drain the stack. */
    private fun settle(game: TestGame, pay: Boolean) {
        var guard = 0
        while (guard++ < 30) {
            when (val decision = game.getPendingDecision()) {
                is YesNoDecision -> game.answerYesNo(pay)
                is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay()
                null -> if (game.state.stack.isNotEmpty()) game.resolveStack() else return
                else -> error("unexpected decision $decision")
            }
        }
    }

    private fun neonateOnMyTurn(extra: ScenarioBuilder.() -> Unit = {}): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, back)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .apply(extra)
        .build()

    init {
        test("extort: paying {W/B} after casting a spell drains each opponent for 1") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withCardInHand(1, "Savannah Lions")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Savannah Lions").error shouldBe null
            settle(game, pay = true)

            game.findPermanent("Savannah Lions") shouldNotBe null
            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 21
        }

        test("extort: declining to pay drains nobody") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withCardInHand(1, "Savannah Lions")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Savannah Lions").error shouldBe null
            settle(game, pay = false)

            game.getLifeTotal(2) shouldBe 20
            game.getLifeTotal(1) shouldBe 20
        }

        test("gaining 3 life this turn flips Sorin at the beginning of your postcombat main phase") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            gainedLifeThisTurn(game, 3)

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            game.findPermanent(front) shouldBe null
            val neonate = game.findPermanent(back)
            withClue("Sorin returned transformed") { neonate shouldNotBe null }
            loyalty(game, neonate!!) shouldBe 3
        }

        test("gaining only 2 life this turn doesn't flip Sorin") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, front)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            gainedLifeThisTurn(game, 2)

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            game.findPermanent(front) shouldNotBe null
            game.findPermanent(back) shouldBe null
        }

        test("+2 creates a Food token") {
            val game = neonateOnMyTurn()
            val sorin = game.findPermanent(back)!!

            game.execute(ActivateAbility(game.player1Id, sorin, abilityId(0))).error shouldBe null
            game.resolveStack()

            loyalty(game, sorin) shouldBe 5
            game.findPermanent("Food") shouldNotBe null
        }

        test("−1 deals damage equal to the life you gained this turn") {
            val game = neonateOnMyTurn()
            val sorin = game.findPermanent(back)!!
            gainedLifeThisTurn(game, 4)

            game.execute(
                ActivateAbility(
                    game.player1Id, sorin, abilityId(1),
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            game.resolveStack()

            loyalty(game, sorin) shouldBe 2
            game.getLifeTotal(2) shouldBe 16
        }

        test("−6 steals a creature, makes it a Vampire, and adds lifelink with another white permanent") {
            val game = neonateOnMyTurn {
                withCardOnBattlefield(1, "Savannah Lions")
                withCardOnBattlefield(2, "Grizzly Bears")
            }
            val sorin = game.findPermanent(back)!!
            game.state = game.state.updateEntity(sorin) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 7))
            }
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, sorin, abilityId(2),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            loyalty(game, sorin) shouldBe 1
            val projected = game.state.projectedState
            projected.getController(bears) shouldBe game.player1Id
            projected.hasSubtype(bears, "Vampire") shouldBe true
            projected.hasSubtype(bears, "Bear") shouldBe true
            game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.LIFELINK) shouldBe 1
        }

        test("−6 adds no lifelink counter when Sorin is your only white permanent") {
            val game = neonateOnMyTurn {
                withCardOnBattlefield(2, "Grizzly Bears")
            }
            val sorin = game.findPermanent(back)!!
            game.state = game.state.updateEntity(sorin) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 6))
            }
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, sorin, abilityId(2),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getController(bears) shouldBe game.player1Id
            game.state.projectedState.hasSubtype(bears, "Vampire") shouldBe true
            (game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.LIFELINK) ?: 0) shouldBe 0
        }

        test("−6 on a white creature needs a second white permanent besides it and Sorin") {
            val game = neonateOnMyTurn {
                withCardOnBattlefield(2, "Savannah Lions")
            }
            val sorin = game.findPermanent(back)!!
            game.state = game.state.updateEntity(sorin) { c ->
                c.with(CountersComponent().withAdded(CounterType.LOYALTY, 6))
            }
            val lions = game.findPermanent("Savannah Lions")!!

            game.execute(
                ActivateAbility(
                    game.player1Id, sorin, abilityId(2),
                    targets = listOf(ChosenTarget.Permanent(lions)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getController(lions) shouldBe game.player1Id
            withClue("the stolen white creature itself doesn't count") {
                (game.state.getEntity(lions)?.get<CountersComponent>()?.getCount(CounterType.LIFELINK) ?: 0) shouldBe 0
            }
        }
    }
}
