package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Monstrous War-Leech (DMU #98) — {3}{B} star/star with Kicker {U}, "As this creature enters, if it was
 * kicked, mill four cards", and P/T equal to the greatest mana value among cards in your graveyard.
 *
 * Proves the as-enters mill is gated on kicked-ness read from the permanent at entry (an
 * `OnEnterRun` wrapping `Effects.If(Conditions.WasKicked, …)` on the cast path), and that it is a
 * replacement rather than a trigger: a kicked Leech entering over an empty graveyard is sized by the
 * cards it just milled instead of dying as a 0/0 first (2022-09-09 ruling).
 */
class MonstrousWarLeechScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 30))
        return d
    }

    test("kicked over an empty graveyard: mills four as it enters and is sized by what it milled") {
        val d = driver()
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Top four of the library, top first: Grizzly Bears (2), Craw Wurm (6), Hill Giant (4), Swamp.
        d.putCardOnTopOfLibrary(you, "Swamp")
        d.putCardOnTopOfLibrary(you, "Hill Giant")
        d.putCardOnTopOfLibrary(you, "Craw Wurm")
        d.putCardOnTopOfLibrary(you, "Grizzly Bears")
        d.getGraveyard(you).size shouldBe 0

        val leech = d.putCardInHand(you, "Monstrous War-Leech")
        // {3}{B} + kicker {U}.
        repeat(4) { d.putLandOnBattlefield(you, "Swamp") }
        d.putLandOnBattlefield(you, "Island")

        d.submit(
            CastSpell(
                playerId = you,
                cardId = leech,
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay,
            ),
        ).outcome shouldBe Outcome.Done
        d.bothPass() // resolve the creature; the mill happens as it enters, no trigger

        d.stackSize shouldBe 0
        d.getGraveyardCardNames(you).shouldContainAll("Grizzly Bears", "Craw Wurm", "Hill Giant", "Swamp")
        d.getGraveyardCardNames(you).size shouldBe 4

        val onBattlefield = d.findPermanent(you, "Monstrous War-Leech")
        onBattlefield.shouldNotBeNull()
        d.state.projectedState.getPower(onBattlefield) shouldBe 6
        d.state.projectedState.getToughness(onBattlefield) shouldBe 6
    }

    test("not kicked: nothing is milled and the P/T reads the existing graveyard") {
        val d = driver()
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCardInGraveyard(you, "Hill Giant")
        d.putCardOnTopOfLibrary(you, "Craw Wurm")

        val leech = d.putCardInHand(you, "Monstrous War-Leech")
        repeat(4) { d.putLandOnBattlefield(you, "Swamp") }

        d.submit(
            CastSpell(
                playerId = you,
                cardId = leech,
                declaredCostSlot = null,
                paymentStrategy = PaymentStrategy.AutoPay,
            ),
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.getGraveyardCardNames(you) shouldBe listOf("Hill Giant")

        val onBattlefield = d.findPermanent(you, "Monstrous War-Leech")
        onBattlefield.shouldNotBeNull()
        d.state.projectedState.getPower(onBattlefield) shouldBe 4
        d.state.projectedState.getToughness(onBattlefield) shouldBe 4
    }

    test("not kicked over an empty graveyard: it is a 0/0 and dies to state-based actions") {
        val d = driver()
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCardOnTopOfLibrary(you, "Craw Wurm")

        val leech = d.putCardInHand(you, "Monstrous War-Leech")
        repeat(4) { d.putLandOnBattlefield(you, "Swamp") }

        d.submit(
            CastSpell(
                playerId = you,
                cardId = leech,
                declaredCostSlot = null,
                paymentStrategy = PaymentStrategy.AutoPay,
            ),
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.findPermanent(you, "Monstrous War-Leech").shouldBeNull()
        d.getGraveyardCardNames(you) shouldContain "Monstrous War-Leech"
        d.getGraveyardCardNames(you) shouldNotContain "Craw Wurm"
    }
})
