package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.MultiversalPassage
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Multiversal Passage (SPM #180): "As this land enters, choose a basic land type. Then you may
 * pay 2 life. If you don't, it enters tapped. This land is the chosen type."
 *
 * The whole clause is one [com.wingedsheep.sdk.scripting.OnEnterRunEffect] composing three atoms:
 * ChooseOption(BASIC_LAND_TYPE) → SetLandType(Self, Permanent, fromChosen) → an optional pay-2-life
 * gate whose decline branch taps the land. These tests prove the chosen type sticks (subtype +
 * intrinsic mana), and that the pay/decline branch controls tapped-ness.
 */
class MultiversalPassageScenarioTest : FunSpec({

    fun GameTestDriver.pool(playerId: EntityId): ManaPoolComponent =
        state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + MultiversalPassage)
        return driver
    }

    fun GameTestDriver.playPassageChoosing(player: EntityId, landType: String): EntityId {
        val passage = putCardInHand(player, "Multiversal Passage")
        playLand(player, passage).isPaused shouldBe true

        // First decision: choose a basic land type.
        val choice = pendingDecision
        choice.shouldBeInstanceOf<ChooseOptionDecision>()
        choice.options shouldContain landType
        submitDecision(player, OptionChosenResponse(choice.id, choice.options.indexOf(landType)))
        return passage
    }

    test("choosing Island makes it an Island that taps for U; paying 2 life enters untapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val passage = driver.playPassageChoosing(p1, "Island")

        // Second decision: you may pay 2 life. Pay it -> land enters untapped.
        val payDecision = driver.pendingDecision
        payDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(p1, true)

        driver.pendingDecision shouldBe null
        driver.state.getEntity(passage)?.has<TappedComponent>() shouldBe false
        driver.getLifeTotal(p1) shouldBe 18

        // It IS an Island now (chosen type replaces its subtypes) ...
        driver.state.projectedState.hasSubtype(passage, "Island").shouldBeTrue()
        // ... and taps for {U} via the intrinsic Island mana ability.
        driver.submitSuccess(ActivateAbility(p1, passage, AbilityId.intrinsicMana('U')))
        driver.pool(p1).blue shouldBe 1
    }

    test("declining the 2 life makes it enter tapped (still the chosen Island type)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val passage = driver.playPassageChoosing(p1, "Island")

        val payDecision = driver.pendingDecision
        payDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(p1, false)

        driver.pendingDecision shouldBe null
        driver.state.getEntity(passage)?.has<TappedComponent>() shouldBe true
        driver.getLifeTotal(p1) shouldBe 20
        driver.state.projectedState.hasSubtype(passage, "Island").shouldBeTrue()
    }

    test("choosing Mountain makes it a Mountain that taps for R") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val passage = driver.playPassageChoosing(p1, "Mountain")
        driver.submitYesNo(p1, true)

        driver.state.projectedState.hasSubtype(passage, "Mountain").shouldBeTrue()
        driver.state.projectedState.hasSubtype(passage, "Island") shouldBe false
        driver.submitSuccess(ActivateAbility(p1, passage, AbilityId.intrinsicMana('R')))
        driver.pool(p1).red shouldBe 1
    }
})
