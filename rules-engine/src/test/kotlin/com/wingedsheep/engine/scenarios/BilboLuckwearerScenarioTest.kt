package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.BilboLuckwearer
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bilbo, Luckwearer // Burglar's Plot (HOB #32) — Adventure (CR 715).
 *
 * Creature face: {1}{U} 1/1 legendary Halfling Rogue — "Bilbo can't be blocked" plus a
 * combat-damage loot trigger.
 * Adventure face: Burglar's Plot {4}{U}, Sorcery — Adventure —
 *   "Exchange control of two target nonland permanents that share a card type."
 *
 * The interesting half is the new cross-target [TargetObject.sameCardType] constraint, so the two
 * exchange tests are the point of this file: two creatures share CREATURE and swap; a creature and
 * a noncreature artifact share no card type and the cast is rejected outright.
 */
class BilboLuckwearerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BilboLuckwearer)
        return driver
    }

    fun startAtMain(driver: GameTestDriver): EntityId {
        driver.initMirrorMatch(deck = Deck.of("Island" to 40, "Grizzly Bears" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return player
    }

    test("the creature face can't be blocked") {
        val driver = createDriver()
        val player = startAtMain(driver)

        val bilbo = driver.putCreatureOnBattlefield(player, "Bilbo, Luckwearer")

        driver.state.projectedState.hasKeyword(bilbo, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
    }

    test("Burglar's Plot exchanges control of two creatures — they share the creature card type") {
        val driver = createDriver()
        val player = startAtMain(driver)
        val opponent = driver.getOpponent(player)

        val mine = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val card = driver.putCardInHand(player, "Bilbo, Luckwearer")
        driver.giveMana(player, Color.BLUE, 5) // {4}{U}

        // faceIndex 0 is the Adventure face (Burglar's Plot).
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = card,
                faceIndex = 0,
                targets = listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // ExchangeControlEffect installs a continuous control-change effect, so the swap shows up
        // in projected state rather than on the base ControllerComponent.
        driver.state.projectedState.getController(mine) shouldBe opponent
        driver.state.projectedState.getController(theirs) shouldBe player
    }

    test("Burglar's Plot can't target a creature and a noncreature artifact — no card type in common") {
        val driver = createDriver()
        val player = startAtMain(driver)
        val opponent = driver.getOpponent(player)

        val creature = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val artifact = driver.putPermanentOnBattlefield(opponent, "Sol Ring")

        val card = driver.putCardInHand(player, "Bilbo, Luckwearer")
        driver.giveMana(player, Color.BLUE, 5)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = card,
                faceIndex = 0,
                targets = listOf(ChosenTarget.Permanent(creature), ChosenTarget.Permanent(artifact)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
        // Rejected specifically by the cross-target constraint, not by some unrelated legality check.
        result.error?.contains("share a card type") shouldBe true

        // Nothing changed hands.
        driver.state.projectedState.getController(creature) shouldBe player
        driver.state.projectedState.getController(artifact) shouldBe opponent
    }
})
