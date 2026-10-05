package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pcy.cards.RhysticStudy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rhystic Study — {2}{U} Enchantment
 * "Whenever an opponent casts a spell, you may draw a card unless that player pays {1}."
 *
 * The caster decides on paying first; only an unpaid toll reaches the Study's controller, who then
 * chooses whether to draw (ruling 2023-09-01).
 */
class RhysticStudyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RhysticStudy))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.greenFloating(playerId: EntityId): Int =
        (state.getEntity(playerId)!!.get<ManaPoolComponent>() ?: ManaPoolComponent()).green

    /**
     * The active player casts Grizzly Bears with one green left floating — enough to pay the toll
     * from the pool — while [studyOwner] controls Rhystic Study. Resolves the trigger up to the
     * caster's pay-or-not prompt.
     */
    fun GameTestDriver.opponentCastsBearsIntoStudy(studyOwner: EntityId, caster: EntityId) {
        putPermanentOnBattlefield(studyOwner, "Rhystic Study")
        val bears = putCardInHand(caster, "Grizzly Bears")
        giveMana(caster, Color.GREEN, 3)
        castSpell(caster, bears).error shouldBe null

        // Bears plus the Study trigger on top.
        stackSize shouldBe 2
        bothPass()

        val payPrompt = pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        payPrompt.playerId shouldBe caster
    }

    test("opponent declines to pay - Study's controller may draw a card") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val studyOwner = driver.getOpponent(caster)
        val handBefore = driver.getHandSize(studyOwner)

        driver.opponentCastsBearsIntoStudy(studyOwner, caster)
        driver.submitYesNo(caster, false)

        // The "you may" is asked of the Study's controller only after the caster declined.
        val mayDraw = driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        mayDraw.playerId shouldBe studyOwner
        driver.submitYesNo(studyOwner, true)

        driver.getHandSize(studyOwner) shouldBe handBefore + 1
        driver.greenFloating(caster) shouldBe 1
    }

    test("opponent declines to pay and Study's controller declines the draw - no card drawn") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val studyOwner = driver.getOpponent(caster)
        val handBefore = driver.getHandSize(studyOwner)

        driver.opponentCastsBearsIntoStudy(studyOwner, caster)
        driver.submitYesNo(caster, false)
        driver.submitYesNo(studyOwner, false)

        driver.getHandSize(studyOwner) shouldBe handBefore
    }

    test("opponent pays {1} - no draw") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val studyOwner = driver.getOpponent(caster)
        val handBefore = driver.getHandSize(studyOwner)

        driver.opponentCastsBearsIntoStudy(studyOwner, caster)
        driver.submitYesNo(caster, true)

        // Floating mana covers the toll; no draw prompt follows.
        driver.pendingDecision shouldBe null
        driver.greenFloating(caster) shouldBe 0
        driver.getHandSize(studyOwner) shouldBe handBefore
    }

    test("an opponent who can't pay is not asked - Study's controller goes straight to the draw choice") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val studyOwner = driver.getOpponent(caster)
        val handBefore = driver.getHandSize(studyOwner)

        driver.putPermanentOnBattlefield(studyOwner, "Rhystic Study")
        val bears = driver.putCardInHand(caster, "Grizzly Bears")
        driver.giveMana(caster, Color.GREEN, 2)
        driver.castSpell(caster, bears).error shouldBe null
        driver.bothPass()

        val mayDraw = driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        mayDraw.playerId shouldBe studyOwner
        driver.submitYesNo(studyOwner, true)

        driver.getHandSize(studyOwner) shouldBe handBefore + 1
    }

    test("its controller's own spells don't trigger it") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        driver.putPermanentOnBattlefield(caster, "Rhystic Study")
        val bears = driver.putCardInHand(caster, "Grizzly Bears")
        driver.giveMana(caster, Color.GREEN, 2)
        driver.castSpell(caster, bears).error shouldBe null

        driver.stackSize shouldBe 1
    }
})
