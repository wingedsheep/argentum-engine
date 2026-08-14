package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Break Down the Door (DSK #170) — {2}{G} Instant.
 *
 * "Choose one —
 *  • Exile target artifact.
 *  • Exile target enchantment.
 *  • Manifest dread."
 *
 * Pure composition: a "choose one" modal spell. Modes 0/1 are [com.wingedsheep.sdk.dsl.Effects.Exile]
 * on an artifact / enchantment target; mode 2 reuses the shared manifest-dread recipe (the same
 * shape as Twist Reality). No new SDK surface.
 */
class BreakDownTheDoorScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("mode 0 (exile artifact): exiles target artifact") {
        val driver = newDriver()
        val me = driver.player1
        val artifact = driver.putPermanentOnBattlefield(driver.player2, "Artifact Creature")

        val spell = driver.putCardInHand(me, "Break Down the Door")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 2)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(artifact)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(artifact))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(driver.player2, "Artifact Creature") shouldBe null
        driver.state.getZone(driver.player2, Zone.EXILE).any { driver.getCardName(it) == "Artifact Creature" } shouldBe true
    }

    test("mode 1 (exile enchantment): exiles target enchantment") {
        val driver = newDriver()
        val me = driver.player1
        // Growing Dread is a {G}{U} Enchantment — a legal target for "exile target enchantment".
        val enchantment = driver.putPermanentOnBattlefield(driver.player2, "Growing Dread")

        val spell = driver.putCardInHand(me, "Break Down the Door")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 2)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(enchantment)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(enchantment))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(driver.player2, "Growing Dread") shouldBe null
    }

    test("mode 2 (manifest dread): manifests one of the top two and bins the other") {
        val driver = newDriver()
        val me = driver.player1

        val land = driver.putCardOnTopOfLibrary(me, "Forest")
        val creature = driver.putCardOnTopOfLibrary(me, "Centaur Courser") // now top card

        val spell = driver.putCardInHand(me, "Break Down the Door")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 2)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = emptyList(),
                chosenModes = listOf(2),
                modeTargetsOrdered = listOf(emptyList()),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (!driver.isPaused && driver.state.stack.isNotEmpty()) driver.bothPass()

        val pick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(me, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(creature)))

        val entity = driver.state.getEntity(creature)
        entity?.get<FaceDownComponent>() shouldBe FaceDownComponent
        entity?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
        driver.state.projectedState.getPower(creature) shouldBe 2
        driver.getGraveyard(me) shouldContain land
    }
})
