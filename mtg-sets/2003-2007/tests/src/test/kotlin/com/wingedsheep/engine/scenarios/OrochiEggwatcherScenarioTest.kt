package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.OrochiEggwatcher
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Orochi Eggwatcher // Shidako, Broodmistress (CHK) — a flip card.
 *
 * "{2}{G}, {T}: Create a 1/1 green Snake creature token. If you control ten or more creatures,
 * flip this creature."
 * Shidako: "{G}, Sacrifice a creature: Target creature gets +3/+3 until end of turn."
 *
 * The count happens after the Snake is made, so the token itself can be the tenth creature.
 */
class OrochiEggwatcherScenarioTest : FunSpec({

    val snakeAbility = OrochiEggwatcher.activatedAbilities.single().id
    val shidakoAbility = OrochiEggwatcher.flipSide!!.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + OrochiEggwatcher)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.makeSnake(eggwatcher: EntityId) {
        giveMana(player1, Color.GREEN, 3)
        submitSuccess(
            ActivateAbility(player1, eggwatcher, snakeAbility, paymentStrategy = PaymentStrategy.FromPool)
        )
        bothPass()
    }

    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name

    fun GameTestDriver.creatureCount() = state.getBattlefield(player1).count { state.projectedState.isCreature(it) }

    test("a ninth creature from the Snake leaves it unflipped") {
        val d = driver()
        val egg = d.putCreatureOnBattlefield(d.player1, "Orochi Eggwatcher")
        d.removeSummoningSickness(egg)
        repeat(7) { d.putCreatureOnBattlefield(d.player1, "Centaur Courser") }

        d.makeSnake(egg)

        d.creatureCount() shouldBe 9
        d.name(egg) shouldBe "Orochi Eggwatcher"
    }

    test("the Snake that makes ten creatures flips it into Shidako, whose ability pumps") {
        val d = driver()
        val egg = d.putCreatureOnBattlefield(d.player1, "Orochi Eggwatcher")
        d.removeSummoningSickness(egg)
        val coursers = List(8) { d.putCreatureOnBattlefield(d.player1, "Centaur Courser") }

        d.makeSnake(egg)

        d.creatureCount() shouldBe 10
        d.name(egg) shouldBe "Shidako, Broodmistress"
        d.state.projectedState.getPower(egg) shouldBe 3
        d.state.projectedState.isLegendary(egg) shouldBe true

        d.giveMana(d.player1, Color.GREEN, 1)
        d.submitSuccess(
            ActivateAbility(
                d.player1, egg, shidakoAbility,
                targets = listOf(ChosenTarget.Permanent(coursers[1])),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(coursers[0])),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.bothPass()

        d.getGraveyard(d.player1) shouldContain coursers[0]
        d.state.projectedState.getPower(coursers[1]) shouldBe 6
        d.state.projectedState.getToughness(coursers[1]) shouldBe 6
    }
})
