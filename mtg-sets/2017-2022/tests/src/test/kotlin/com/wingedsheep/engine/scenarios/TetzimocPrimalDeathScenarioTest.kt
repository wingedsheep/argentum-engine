package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rix.cards.TetzimocPrimalDeath
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Tetzimoc, Primal Death — {4}{B}{B} 6/6 Legendary Creature — Elder Dinosaur.
 *
 * "Deathtouch / {B}, Reveal this card from your hand: Put a prey counter on target creature.
 *  Activate only during your turn. / When Tetzimoc enters, destroy each creature your opponents
 *  control with a prey counter on it."
 */
class TetzimocPrimalDeathScenarioTest : FunSpec({

    val revealAbility = TetzimocPrimalDeath.activatedAbilities.single()

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + TetzimocPrimalDeath)
        initMirrorMatch(deck = Deck.of("Swamp" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.prey(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PREY) ?: 0

    fun GameTestDriver.mark(tetzimoc: EntityId, creature: EntityId) =
        submit(ActivateAbility(player1, tetzimoc, revealAbility.id, listOf(ChosenTarget.Permanent(creature))))

    test("revealing from hand marks creatures repeatedly, and its entry destroys only the opponents' prey") {
        val d = driver()
        val tetzimoc = d.putCardInHand(d.player1, TetzimocPrimalDeath.name)
        val preyA = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val preyB = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val spared = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val ownMarked = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        // The reveal is a cost the card pays while staying in hand, so it can be paid again.
        for (target in listOf(preyA, preyB, ownMarked)) {
            d.giveMana(d.player1, Color.BLACK, 1)
            val result = d.mark(tetzimoc, target)
            result.outcome shouldBe Outcome.Done
            result.events.filterIsInstance<CardsRevealedEvent>().single().cardIds shouldBe listOf(tetzimoc)
            d.bothPass()
            d.getHand(d.player1) shouldContain tetzimoc
        }
        d.prey(preyA) shouldBe 1
        d.prey(preyB) shouldBe 1
        d.prey(ownMarked) shouldBe 1

        d.giveMana(d.player1, Color.BLACK, 6)
        d.castSpell(d.player1, tetzimoc).outcome shouldBe Outcome.Done
        d.bothPass() // Tetzimoc resolves; its entry trigger goes on the stack
        d.bothPass() // the trigger resolves

        val battlefield = d.state.getBattlefield()
        battlefield shouldContain tetzimoc
        battlefield shouldNotContain preyA
        battlefield shouldNotContain preyB
        // A creature with no prey counter, and a marked creature Tetzimoc's controller controls, survive.
        battlefield shouldContain spared
        battlefield shouldContain ownMarked
    }

    test("the ability can't be activated on an opponent's turn, nor once Tetzimoc is on the battlefield") {
        val d = driver()
        val tetzimoc = d.putCardInHand(d.player1, TetzimocPrimalDeath.name)
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        d.passPriorityUntil(Step.UPKEEP, d.player2)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player2)
        d.giveMana(d.player1, Color.BLACK, 1)
        d.submitExpectFailure(
            ActivateAbility(d.player1, tetzimoc, revealAbility.id, listOf(ChosenTarget.Permanent(bear)))
        )
        d.prey(bear) shouldBe 0

        val onBattlefield = d.putCreatureOnBattlefield(d.player1, TetzimocPrimalDeath.name)
        d.passPriorityUntil(Step.UPKEEP, d.player1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player1)
        d.giveMana(d.player1, Color.BLACK, 1)
        d.submitExpectFailure(
            ActivateAbility(d.player1, onBattlefield, revealAbility.id, listOf(ChosenTarget.Permanent(bear)))
        )
        d.state.getZone(d.player1, Zone.HAND) shouldContain tetzimoc
    }
})
