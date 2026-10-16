package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.BehemothOfVault0
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Behemoth of Vault 0 (PIP) — ETB four energy; when it dies, you may pay {E} equal to target
 * nonland permanent's mana value, and when you do, an untargeted reflexive trigger destroys that
 * permanent. Pins the target being chosen as the dies trigger goes on the stack, the payment
 * amount read off that target on resolution, and "that permanent" carried into the reflexive
 * ability.
 */
class BehemothOfVault0ScenarioTest : FunSpec({

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(BehemothOfVault0))
        it.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun GameTestDriver.seedEnergy(amount: Int) {
        replaceState(
            state.updateEntity(player1) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.ENERGY, amount))
            }
        )
    }

    /** Doom Blade the Behemoth, then choose [target] for its dies trigger as it goes on the stack. */
    fun GameTestDriver.killBehemothTargeting(behemoth: EntityId, target: EntityId) {
        val blade = putCardInHand(player1, "Doom Blade")
        giveMana(player1, Color.BLACK, 1)
        giveColorlessMana(player1, 1)
        castSpell(player1, blade, targets = listOf(behemoth)).error shouldBe null
        bothPass() // resolve Doom Blade -> Behemoth dies

        state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        submitTargetSelection(player1, listOf(target)).error shouldBe null
        bothPass() // resolve the dies trigger
    }

    test("entering gives four energy") {
        val d = driver()
        val spell = d.putCardInHand(d.player1, BehemothOfVault0.name)
        d.giveColorlessMana(d.player1, 6)
        d.castSpell(d.player1, spell).error shouldBe null
        d.bothPass() // resolve the creature spell
        d.bothPass() // resolve the enters trigger
        d.energy() shouldBe 4
    }

    test("paying energy equal to the target's mana value destroys it via the reflexive trigger") {
        val d = driver()
        val behemoth = d.putCreatureOnBattlefield(d.player1, BehemothOfVault0.name)
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.seedEnergy(4)

        d.killBehemothTargeting(behemoth, courser)

        d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.energy() shouldBe 1 // Centaur Courser's mana value is 3
        d.findPermanent(d.player2, "Centaur Courser") shouldNotBe null // reflexive trigger still on the stack

        d.bothPass() // resolve the reflexive trigger
        d.findPermanent(d.player2, "Centaur Courser") shouldBe null
        d.getGraveyardCardNames(d.player2).contains("Centaur Courser") shouldBe true
        d.findPermanent(d.player2, "Grizzly Bears") shouldBe bears
    }

    test("declining the payment keeps the energy and the target") {
        val d = driver()
        val behemoth = d.putCreatureOnBattlefield(d.player1, BehemothOfVault0.name)
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.seedEnergy(4)

        d.killBehemothTargeting(behemoth, courser)

        d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, false).error shouldBe null
        d.energy() shouldBe 4
        d.bothPass()
        d.findPermanent(d.player2, "Centaur Courser") shouldNotBe null
    }

    test("without enough energy for the target's mana value the payment isn't offered") {
        val d = driver()
        val behemoth = d.putCreatureOnBattlefield(d.player1, BehemothOfVault0.name)
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.seedEnergy(2)

        d.killBehemothTargeting(behemoth, courser)

        (d.state.pendingDecision is YesNoDecision) shouldBe false
        d.energy() shouldBe 2
        d.findPermanent(d.player2, "Centaur Courser") shouldNotBe null
    }
})
