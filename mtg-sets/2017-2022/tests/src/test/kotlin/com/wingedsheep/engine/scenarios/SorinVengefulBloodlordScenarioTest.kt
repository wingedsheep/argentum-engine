package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.SorinVengefulBloodlord
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sorin, Vengeful Bloodlord {2}{W}{B} — WAR #217.
 *
 * Pins the your-turn lifelink grant reaching planeswalkers (Sorin's own +2 gains life), its absence
 * on the opponent's turn, and the −X: the target's mana value must equal X, and the returned
 * creature stays a Vampire.
 */
class SorinVengefulBloodlordScenarioTest : FunSpec({

    val plusTwo = SorinVengefulBloodlord.script.activatedAbilities[0].id
    val minusX = SorinVengefulBloodlord.script.activatedAbilities[1].id

    data class Board(val d: GameTestDriver, val you: EntityId, val opp: EntityId, val sorin: EntityId)

    fun setup(): Board {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(SorinVengefulBloodlord))
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = d.activePlayer!!
        val sorin = d.putPermanentOnBattlefield(you, "Sorin, Vengeful Bloodlord").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 4)))
        }
        return Board(d, you, d.getOpponent(you), sorin)
    }

    test("creatures and planeswalkers you control have lifelink only during your turn") {
        val (d, you, opp, sorin) = setup()
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val theirBear = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.state.projectedState.hasKeyword(bear, Keyword.LIFELINK) shouldBe true
        d.state.projectedState.hasKeyword(sorin, Keyword.LIFELINK) shouldBe true
        d.state.projectedState.hasKeyword(theirBear, Keyword.LIFELINK) shouldBe false

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opp)
        d.state.projectedState.hasKeyword(bear, Keyword.LIFELINK) shouldBe false
        d.state.projectedState.hasKeyword(sorin, Keyword.LIFELINK) shouldBe false
    }

    test("+2 deals 1 damage to the target player and Sorin's lifelink gains 1 life") {
        val (d, you, opp, sorin) = setup()

        d.submitSuccess(ActivateAbility(you, sorin, plusTwo, targets = listOf(ChosenTarget.Player(opp))))
        d.state.getEntity(sorin)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 6
        d.bothPass()

        d.getLifeTotal(opp) shouldBe 19
        d.getLifeTotal(you) shouldBe 21
    }

    test("−X returns a creature card with mana value exactly X, and it is a Vampire") {
        val (d, you, _, sorin) = setup()
        val courser = d.putCardInGraveyard(you, "Centaur Courser")

        // Mana value 3 doesn't match X = 2.
        d.submit(
            ActivateAbility(
                you, sorin, minusX,
                targets = listOf(ChosenTarget.Card(courser, you, Zone.GRAVEYARD)),
                xValue = 2,
            )
        ).outcome shouldNotBe Outcome.Done

        d.submitSuccess(
            ActivateAbility(
                you, sorin, minusX,
                targets = listOf(ChosenTarget.Card(courser, you, Zone.GRAVEYARD)),
                xValue = 3,
            )
        )
        d.state.getEntity(sorin)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 1
        d.bothPass()

        val returned = d.findPermanent(you, "Centaur Courser")
        returned shouldNotBe null
        d.state.projectedState.hasSubtype(returned!!, "Vampire") shouldBe true
        d.state.projectedState.hasSubtype(returned, "Centaur") shouldBe true

        // The type change has no duration: still a Vampire on a later turn.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = d.getOpponent(you))
        d.state.projectedState.hasSubtype(returned, "Vampire") shouldBe true
    }
})
