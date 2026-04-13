package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.JackdawSavior
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Jackdaw Savior:
 * {2}{W} Creature — Bird Cleric 3/1
 * Flying
 * Whenever this creature or another creature you control with flying dies,
 * return another target creature card with lesser mana value from your
 * graveyard to the battlefield.
 */
class JackdawSaviorTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(JackdawSavior))
        return driver
    }

    test("Jackdaw Savior dying triggers its own ability and returns creature from graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Mountain" to 20))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Jackdaw Savior (MV 3, 3/1 flying) on battlefield
        val jackdaw = driver.putCreatureOnBattlefield(activePlayer, "Jackdaw Savior")

        // Grizzly Bears (MV 2) in graveyard — lesser mana value than Jackdaw (3)
        val bears = driver.putCardInGraveyard(activePlayer, "Grizzly Bears")

        // Kill Jackdaw Savior with Lightning Bolt (3 damage kills 3/1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)

        // Pass to opponent so they can cast
        driver.passPriority(activePlayer)
        driver.castSpell(opponent, bolt, listOf(jackdaw))
        driver.bothPass()

        // Jackdaw Savior should be dead
        driver.findPermanent(activePlayer, "Jackdaw Savior") shouldBe null

        // Its dies trigger should be on the stack (it's a flying creature you control that died)
        driver.stackSize shouldBe 1

        // Resolve the trigger — only one eligible creature (Grizzly Bears), auto-selected
        driver.bothPass()

        // Grizzly Bears should now be on the battlefield (returned from graveyard)
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldNotBe null

        // And not in the graveyard anymore
        val graveyard = driver.getGraveyard(activePlayer)
        graveyard.none { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
    }
})
