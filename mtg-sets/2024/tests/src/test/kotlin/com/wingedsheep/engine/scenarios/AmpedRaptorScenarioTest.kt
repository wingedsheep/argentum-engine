package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.AmpedRaptor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Amped Raptor (MH3) — two energy on entry; then, if it was cast from hand, exile from the top of the
 * library until a nonland card, which may be cast by paying {E} equal to its mana value rather than
 * its mana cost. Uncast cards — lands passed over, a declined or unaffordable spell — stay in exile.
 */
class AmpedRaptorScenarioTest : FunSpec({
    // "Put a creature card from your hand onto the battlefield" — an entry that isn't a cast.
    val RaptorDrop = card("Raptor Drop Test") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Hand.putFromHand(GameObjectFilter.Creature) }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + AmpedRaptor + RaptorDrop)
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun GameTestDriver.setEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })

    /** Cast the Raptor from hand and resolve it, leaving its enters trigger resolving. */
    fun GameTestDriver.castRaptor() {
        val raptor = putCardInHand(player1, "Amped Raptor")
        giveMana(player1, Color.RED, 2)
        castSpell(player1, raptor).error shouldBe null
        bothPass() // Raptor resolves; its trigger goes on the stack
        bothPass() // the trigger resolves
    }

    test("enough energy: cast the exiled card for energy equal to its mana value") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player1, "Centaur Courser") // mana value 3
        d.setEnergy(1)

        d.castRaptor()

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.energy() shouldBe 0 // 1 + 2 from the trigger − 3
        d.getStackSpellNames() shouldBe listOf("Centaur Courser")
        d.bothPass()
        d.findPermanent(d.player1, "Centaur Courser") shouldNotBe null
        d.findPermanent(d.player1, "Amped Raptor") shouldNotBe null
    }

    test("declining leaves the card in exile and the energy unspent") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        d.setEnergy(1)

        d.castRaptor()

        d.submitYesNo(d.player1, false).error shouldBe null
        d.energy() shouldBe 3
        d.getExileCardNames(d.player1) shouldContain "Centaur Courser"
        d.findCardInHand(d.player1, "Centaur Courser") shouldBe null
        d.getStackSpellNames() shouldBe emptyList()
    }

    test("lands are exiled on the way to the nonland card and stay there") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt")
        d.putCardOnTopOfLibrary(d.player1, "Mountain")
        d.putCardOnTopOfLibrary(d.player1, "Mountain")

        d.castRaptor()

        d.submitYesNo(d.player1, true).error shouldBe null
        d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null
        d.energy() shouldBe 1
        d.getExileCardNames(d.player1).filter { it == "Mountain" }.size shouldBe 2
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("too little energy: no offer, the card stays in exile") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player1, "Centaur Courser") // needs 3, the trigger gives 2

        d.castRaptor()

        (d.pendingDecision is YesNoDecision) shouldBe false
        d.energy() shouldBe 2
        d.getExileCardNames(d.player1) shouldContain "Centaur Courser"
        d.getStackSpellNames() shouldBe emptyList()
    }

    test("put onto the battlefield rather than cast: only the energy") {
        val d = driver()
        val courser = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        val raptor = d.putCardInHand(d.player1, "Amped Raptor")
        val drop = d.putCardInHand(d.player1, "Raptor Drop Test")
        d.giveMana(d.player1, Color.GREEN, 1)

        d.castSpell(d.player1, drop).error shouldBe null
        d.bothPass()
        d.submitCardSelection(d.player1, listOf(raptor)).error shouldBe null
        d.bothPass() // the enters trigger resolves

        d.findPermanent(d.player1, "Amped Raptor") shouldNotBe null
        d.energy() shouldBe 2
        (d.pendingDecision is YesNoDecision) shouldBe false
        d.state.getLibrary(d.player1).first() shouldBe courser
        d.getExileCardNames(d.player1) shouldBe emptyList()
    }
})
