package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.state.components.battlefield.SaddledComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.FortuneLoyalSteed
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.SelectCardsDecision
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Fortune, Loyal Steed (OTJ rare Beast Mount), {2}{W}.
 *
 * - When Fortune enters, scry 2.
 * - Whenever Fortune attacks while saddled, at end of combat, exile it and up to one creature
 *   that saddled it this turn, then return those cards to the battlefield under their owner's
 *   control.
 * - Saddle 1.
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.effects.CardSource.CreaturesThatSaddledSource]
 * gather (the creatures that saddled the Mount) feeding the end-of-combat linked-exile blink.
 */
class FortuneLoyalSteedScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + FortuneLoyalSteed)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.isSaddled(id: EntityId): Boolean =
        state.getEntity(id)?.has<SaddledComponent>() == true

    test("ETB scry 2 resolves without error when Fortune is cast") {
        val driver = createDriver()
        val player = driver.player1
        val spell = driver.putCardInHand(player, "Fortune, Loyal Steed")
        driver.giveMana(player, com.wingedsheep.sdk.core.Color.WHITE, 1) // {W}
        driver.giveColorlessMana(player, 2)                              // {2}
        driver.submitSuccess(com.wingedsheep.engine.core.CastSpell(player, spell))
        // Resolving Fortune fires its enter trigger (scry 2); pass to the next main phase, letting
        // the auto-resolver answer the resulting library-reorder decision.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.findPermanent(player, "Fortune, Loyal Steed") shouldNotBe null
    }

    test("attacks while saddled: at end of combat, Fortune and its saddler are blinked") {
        val driver = createDriver()
        val fortune = driver.putCreatureOnBattlefield(driver.player1, "Fortune, Loyal Steed")
        val saddler = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") // power 2 >= saddle 1
        driver.removeSummoningSickness(fortune)

        // Saddle Fortune with the bear (taps the bear, marks Fortune saddled).
        driver.submitSuccess(SaddleMount(driver.player1, fortune, listOf(saddler)))
        driver.bothPass()
        driver.isSaddled(fortune) shouldBe true
        driver.isTapped(saddler) shouldBe true

        // Attack with Fortune.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(fortune), driver.player2)
        driver.bothPass() // resolve the attacks-while-saddled trigger (schedules the delayed blink)
        driver.isTapped(fortune) shouldBe true // attacking tapped Fortune

        // Advance toward end of combat. The delayed trigger fires and asks the controller to pick
        // up to one creature that saddled Fortune; choose the bear.
        var guard = 0
        while (driver.pendingDecision !is SelectCardsDecision && driver.state.step != Step.POSTCOMBAT_MAIN && guard++ < 40) {
            if (driver.pendingDecision != null) {
                driver.autoResolveDecision()
            } else if (driver.state.priorityPlayerId != null) {
                driver.bothPass()
            }
        }
        val decision = driver.pendingDecision as? SelectCardsDecision
            ?: error("Expected a SelectCardsDecision for the saddler choice (have ${driver.pendingDecision}, step ${driver.state.step})")
        // The bear that saddled Fortune is among the options.
        decision.options.contains(saddler) shouldBe true
        driver.submitCardSelection(decision.playerId, listOf(saddler))

        // Finish out combat so the linked-exile return resolves.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Exactly one Fortune is on the battlefield (it was exiled and returned, not duplicated),
        // and it re-entered untapped (it had been tapped attacking).
        val fortunesAfter = driver.getCreatures(driver.player1).filter {
            driver.state.getEntity(it)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name ==
                "Fortune, Loyal Steed"
        }
        fortunesAfter.size shouldBe 1
        driver.isTapped(fortunesAfter.single()) shouldBe false

        // The saddler was exiled and returned too — untapped (it had been tapped to saddle).
        val saddlerAfter = driver.findPermanent(driver.player1, "Grizzly Bears")
        saddlerAfter shouldNotBe null
        driver.isTapped(saddlerAfter!!) shouldBe false
    }

    test("attacks while NOT saddled: no end-of-combat blink") {
        val driver = createDriver()
        val fortune = driver.putCreatureOnBattlefield(driver.player1, "Fortune, Loyal Steed")
        driver.removeSummoningSickness(fortune)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(fortune), driver.player2)

        // Run through combat: no saddle means no trigger, no blink — Fortune stays the same object.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.findPermanent(driver.player1, "Fortune, Loyal Steed") shouldBe fortune
    }
})
