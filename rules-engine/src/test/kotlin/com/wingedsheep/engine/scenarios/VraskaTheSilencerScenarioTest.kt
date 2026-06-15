package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.VraskaTheSilencer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Vraska, the Silencer (OTJ) — {1}{B}{G} Legendary Gorgon Assassin 3/3.
 *
 *  - Deathtouch.
 *  - Whenever a nontoken creature an opponent controls dies, you may pay {1}. If you do, return
 *    that card to the battlefield tapped under your control. It's a Treasure artifact with
 *    "{T}, Sacrifice this artifact: Add one mana of any color," and it loses all other card types.
 */
class VraskaTheSilencerScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + VraskaTheSilencer)
        return driver
    }

    test("paying {1} returns the opponent's dead creature as a tapped Treasure under your control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Vraska, the Silencer")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3 nontoken

        // Kill the opponent's creature with Lightning Bolt. Give the {1} up front so the may-pay
        // is affordable when the trigger resolves (an unaffordable may-pay is auto-declined).
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        val courser = driver.findPermanent(opponent, "Centaur Courser")!!
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 1)
        driver.castSpell(player, bolt, listOf(courser))
        driver.bothPass() // resolve Lightning Bolt → Courser dies → Vraska's trigger goes on the stack
        driver.bothPass() // resolve the trigger → exposes the may-pay {1}

        // Pay {1} for the trigger (the {1} is already in the pool, so it's spent without a
        // separate mana-source selection).
        driver.submitYesNo(player, true)
        driver.bothPass()

        // The Courser card is no longer in the opponent's graveyard...
        driver.getGraveyard(opponent).mapNotNull { driver.getCardName(it) } shouldNotContain "Centaur Courser"
        // ...it's on the controller's battlefield as a tapped Treasure.
        val treasure = driver.findPermanent(player, "Centaur Courser")!!
        driver.isTapped(treasure) shouldBe true

        val projected = projector.project(driver.state)
        projected.getTypes(treasure) shouldContain "ARTIFACT"
        projected.getTypes(treasure) shouldNotContain "CREATURE"
        projected.hasSubtype(treasure, "Treasure") shouldBe true
    }

    test("declining the payment leaves the card in the opponent's graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Vraska, the Silencer")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        val courser = driver.findPermanent(opponent, "Centaur Courser")!!
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 1) // affordable, so the may-pay yes/no is offered
        driver.castSpell(player, bolt, listOf(courser))
        driver.bothPass()
        driver.bothPass()

        // Decline the may-pay.
        driver.submitYesNo(player, false)
        driver.bothPass()

        driver.getGraveyard(opponent).mapNotNull { driver.getCardName(it) } shouldContain "Centaur Courser"
        driver.findPermanent(player, "Centaur Courser") shouldBe null
    }

    test("your own creature dying does not trigger Vraska") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Vraska, the Silencer")
        driver.putCreatureOnBattlefield(player, "Centaur Courser") // YOUR creature

        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        val courser = driver.findPermanent(player, "Centaur Courser")!!
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, bolt, listOf(courser))
        driver.bothPass()
        driver.bothPass()

        // No may-pay trigger; the creature just dies to your own graveyard.
        driver.getGraveyard(player).mapNotNull { driver.getCardName(it) } shouldContain "Centaur Courser"
    }
})
