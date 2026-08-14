package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Under the Skin (DSK #203) — {2}{G} Sorcery.
 *
 * "Manifest dread. You may return a permanent card from your graveyard to your hand."
 *
 * Pure composition: the shared manifest-dread recipe, then an optional Gather → SelectUpTo(1) →
 * Move-to-hand of a permanent card from your graveyard (the Overlord of the Balemurk shape). The
 * manifest-dread pick comes first, then the optional return.
 */
class UnderTheSkinScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castUnderTheSkin(me: com.wingedsheep.sdk.model.EntityId) {
        val spell = putCardInHand(me, "Under the Skin")
        giveMana(me, Color.GREEN, 1)
        giveColorlessMana(me, 2)
        castSpell(me, spell)
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    test("manifests dread then returns a permanent card from the graveyard to hand") {
        val driver = newDriver()
        val me = driver.player1

        // A permanent card (creature) already in the graveyard to return.
        val grizzly = driver.putCardInGraveyard(me, "Grizzly Bears")

        // Top two of library for manifest dread: creature on top, land beneath.
        val land = driver.putCardOnTopOfLibrary(me, "Forest")
        val creature = driver.putCardOnTopOfLibrary(me, "Centaur Courser")

        driver.castUnderTheSkin(me)

        // First pause: choose which of the looked-at two to manifest.
        val manifestPick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(me, CardsSelectedResponse(decisionId = manifestPick.id, selectedCards = listOf(creature)))
        while (!driver.isPaused && driver.state.stack.isNotEmpty()) driver.bothPass()

        // Second pause: optional return of a permanent card from the graveyard.
        val returnPick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        returnPick.options shouldContain grizzly
        driver.submitDecision(me, CardsSelectedResponse(decisionId = returnPick.id, selectedCards = listOf(grizzly)))
        while (!driver.isPaused && driver.state.stack.isNotEmpty()) driver.bothPass()

        // Manifested creature is a face-down 2/2; the land was binned.
        driver.state.getEntity(creature)?.get<FaceDownComponent>() shouldBe FaceDownComponent
        driver.state.getEntity(creature)?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
        driver.getGraveyard(me) shouldContain land
        // The permanent card was returned to hand.
        driver.getHand(me) shouldContain grizzly
        driver.getGraveyard(me) shouldNotContain grizzly
    }

    test("the optional return may be declined") {
        val driver = newDriver()
        val me = driver.player1

        val grizzly = driver.putCardInGraveyard(me, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(me, "Forest")
        val creature = driver.putCardOnTopOfLibrary(me, "Centaur Courser")

        driver.castUnderTheSkin(me)
        val manifestPick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(me, CardsSelectedResponse(decisionId = manifestPick.id, selectedCards = listOf(creature)))
        while (!driver.isPaused && driver.state.stack.isNotEmpty()) driver.bothPass()

        // Decline the return (choose nothing — it's "up to one").
        val returnPick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(me, CardsSelectedResponse(decisionId = returnPick.id, selectedCards = emptyList()))
        while (!driver.isPaused && driver.state.stack.isNotEmpty()) driver.bothPass()

        // The card stays in the graveyard; manifest still happened.
        driver.getGraveyard(me) shouldContain grizzly
        driver.getHand(me) shouldNotContain grizzly
        driver.state.getEntity(creature)?.get<FaceDownComponent>() shouldBe FaceDownComponent
    }
})
