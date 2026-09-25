package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.SoratamiSavant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Soratami Savant (CHK) — "{3}, Return a land you control to its owner's hand: Counter target
 * spell unless its controller pays {3}."
 *
 * The opponent casts Grizzly Bears; the Savant's controller bounces an Island and aims the ability
 * at it. The spell's controller is asked to pay: declining counters it, paying saves it.
 */
class SoratamiSavantScenarioTest : FunSpec({

    val abilityId = SoratamiSavant.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SoratamiSavant)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Player 1 casts Grizzly Bears; player 2 answers with the Savant, bouncing an Island. */
    fun GameTestDriver.bearsOnStackWithSavantActivated(): Pair<EntityId, EntityId> {
        val caster = player1
        val savantController = getOpponent(caster)
        val savant = putCreatureOnBattlefield(savantController, "Soratami Savant")
        val island = putLandOnBattlefield(savantController, "Island")
        // Three untapped Forests, so the {3} is payable and the caster is actually asked.
        repeat(3) { putLandOnBattlefield(caster, "Forest") }

        val bears = putCardInHand(caster, "Grizzly Bears")
        giveMana(caster, Color.GREEN, 2)
        castSpell(caster, bears).error shouldBe null
        passPriority(caster)

        giveColorlessMana(savantController, 3)
        submitSuccess(
            ActivateAbility(
                playerId = savantController,
                sourceId = savant,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Spell(bears)),
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(island)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        withClue("the Island returns to hand as the cost is paid") {
            getHand(savantController) shouldContain island
        }
        bothPass()
        return bears to savantController
    }

    test("counters the spell outright when its controller can't pay") {
        val d = driver()
        val caster = d.player1
        val savantController = d.getOpponent(caster)
        val savant = d.putCreatureOnBattlefield(savantController, "Soratami Savant")
        val island = d.putLandOnBattlefield(savantController, "Island")
        val bears = d.putCardInHand(caster, "Grizzly Bears")
        d.giveMana(caster, Color.GREEN, 2)
        d.castSpell(caster, bears).error shouldBe null
        d.passPriority(caster)
        d.giveColorlessMana(savantController, 3)
        d.submitSuccess(
            ActivateAbility(
                playerId = savantController,
                sourceId = savant,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Spell(bears)),
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(island)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        d.bothPass()

        d.getGraveyard(caster) shouldContain bears
    }

    test("counters the spell when its controller declines to pay") {
        val d = driver()
        val (bears, _) = d.bearsOnStackWithSavantActivated()

        val decision = d.pendingDecision.shouldNotBeNull()
        decision.shouldBeInstanceOf<YesNoDecision>()
        withClue("the spell's controller is the one asked to pay") {
            decision.playerId shouldBe d.player1
        }
        d.submitYesNo(d.player1, false)

        d.getGraveyard(d.player1) shouldContain bears
        d.findPermanent(d.player1, "Grizzly Bears") shouldBe null
    }

    test("the spell resolves when its controller pays {3}") {
        val d = driver()
        d.bearsOnStackWithSavantActivated()

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true)
        // Accepting only selects the payment; the {3} itself is then paid from the three Forests.
        d.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        d.submitManaAutoPayOrDecline(d.player1, autoPay = true)
        d.getStackSpellNames() shouldBe listOf("Grizzly Bears")
        d.bothPass()

        d.findPermanent(d.player1, "Grizzly Bears").shouldNotBeNull()
    }
})
