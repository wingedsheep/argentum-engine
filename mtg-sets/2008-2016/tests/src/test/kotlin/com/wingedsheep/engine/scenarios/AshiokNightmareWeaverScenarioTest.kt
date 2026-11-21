package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ths.cards.AshiokNightmareWeaver
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Ashiok, Nightmare Weaver — the +2 and −10 file their exiles in Ashiok's linked pile, and the −X
 * puts back only a creature card from that pile whose mana value equals X, under Ashiok's
 * controller's control, as a Nightmare in addition to its other types.
 */
class AshiokNightmareWeaverScenarioTest : FunSpec({
    val bearTwo = card("Ashiok Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val giantThree = card("Ashiok Test Giant") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Giant"
        power = 3
        toughness = 3
    }
    val sorceryTwo = card("Ashiok Test Sorcery") {
        manaCost = "{1}{B}"
        typeLine = "Sorcery"
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(AshiokNightmareWeaver, bearTwo, giantThree, sorceryTwo))
        it.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun loyalty(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    val plusTwo = AshiokNightmareWeaver.script.activatedAbilities[0]
    val minusX = AshiokNightmareWeaver.script.activatedAbilities[1]
    val minusTen = AshiokNightmareWeaver.script.activatedAbilities[2]

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 30) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    test("+2 exiles the opponent's top three; -X returns only the creature with mana value X, as a Nightmare") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        val ashiok = d.putPermanentOnBattlefield(me, "Ashiok, Nightmare Weaver").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 3)))
        }
        val sorcery = d.putCardOnTopOfLibrary(opp, "Ashiok Test Sorcery")
        val giant = d.putCardOnTopOfLibrary(opp, "Ashiok Test Giant")
        val bear = d.putCardOnTopOfLibrary(opp, "Ashiok Test Bear")

        d.submitSuccess(ActivateAbility(me, ashiok, plusTwo.id, targets = listOf(ChosenTarget.Player(opp))))
        d.settle()
        loyalty(d, ashiok) shouldBe 5
        withClue("the top three cards of the opponent's library are exiled") {
            listOf(bear, giant, sorcery).forEach { d.state.getZone(ZoneKey(opp, Zone.EXILE)).contains(it) shouldBe true }
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opp)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = me)
        d.submitSuccess(ActivateAbility(me, ashiok, minusX.id, xValue = 2))
        loyalty(d, ashiok) shouldBe 3
        d.bothPass()
        (d.pendingDecision as? SelectCardsDecision)?.let { decision ->
            withClue("only the mana value 2 creature card is eligible") {
                decision.options shouldContainExactly listOf(bear)
            }
            d.submitCardSelection(me, listOf(bear))
        }
        d.settle()

        withClue("the bear enters under Ashiok's controller's control as a Nightmare Bear") {
            d.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(bear) shouldBe true
            d.state.projectedState.getController(bear) shouldBe me
            d.state.projectedState.hasSubtype(bear, "Nightmare") shouldBe true
            d.state.projectedState.hasSubtype(bear, "Bear") shouldBe true
        }
        withClue("the giant (wrong mana value) and the sorcery stay exiled") {
            d.state.getZone(ZoneKey(opp, Zone.EXILE)).contains(giant) shouldBe true
            d.state.getZone(ZoneKey(opp, Zone.EXILE)).contains(sorcery) shouldBe true
        }
    }

    test("-10 exiles opponents' hands and graveyards into Ashiok's pile, so -X can return one of them") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        val ashiok = d.putPermanentOnBattlefield(me, "Ashiok, Nightmare Weaver").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 13)))
        }
        val myGiant = d.putCardInGraveyard(me, "Ashiok Test Giant")
        val theirGiant = d.putCardInGraveyard(opp, "Ashiok Test Giant")
        val theirBear = d.putCardInHand(opp, "Ashiok Test Bear")
        val theirHand = d.getHand(opp)

        d.submitSuccess(ActivateAbility(me, ashiok, minusTen.id))
        d.settle()
        loyalty(d, ashiok) shouldBe 3

        withClue("every card in the opponent's hand and graveyard is exiled") {
            d.getHandSize(opp) shouldBe 0
            d.getGraveyard(opp).isEmpty() shouldBe true
            theirHand.forEach { d.state.getZone(ZoneKey(opp, Zone.EXILE)).contains(it) shouldBe true }
            d.state.getZone(ZoneKey(opp, Zone.EXILE)).contains(theirGiant) shouldBe true
        }
        withClue("your own graveyard is untouched") {
            d.getGraveyard(me).contains(myGiant) shouldBe true
        }

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opp)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = me)
        d.submitSuccess(ActivateAbility(me, ashiok, minusX.id, xValue = 3))
        d.settle()

        withClue("a creature exiled by the -10 is in Ashiok's pile and comes back for X = 3") {
            d.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(theirGiant) shouldBe true
            d.state.projectedState.hasSubtype(theirGiant, "Nightmare") shouldBe true
            d.state.getZone(ZoneKey(opp, Zone.EXILE)).contains(theirBear) shouldBe true
        }
    }
})
