package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Day of the Dragons.
 *
 * Day of the Dragons: {4}{U}{U}{U}
 * Enchantment
 * When Day of the Dragons enters the battlefield, exile all creatures you control.
 * Then create that many 5/5 red Dragon creature tokens with flying.
 * When Day of the Dragons leaves the battlefield, sacrifice all Dragons you control.
 * Then return the exiled cards to the battlefield under your control.
 */
class DayOfTheDragonsTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun GameTestDriver.findAllPermanentsByName(name: String): List<com.wingedsheep.sdk.model.EntityId> {
        return state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == name
        }
    }

    fun GameTestDriver.isInExile(playerId: com.wingedsheep.sdk.model.EntityId, cardName: String): Boolean {
        return state.getExile(playerId).any { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    test("ETB exiles creatures and creates Dragon tokens") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Glory Seeker" to 10, "Elvish Aberration" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 2 creatures on active player's battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Glory Seeker")
        driver.putCreatureOnBattlefield(activePlayer, "Elvish Aberration")

        // Give mana and card in hand
        val dayOfDragons = driver.putCardInHand(activePlayer, "Day of the Dragons")
        driver.giveMana(activePlayer, Color.BLUE, 7)

        // Cast Day of the Dragons
        driver.castSpell(activePlayer, dayOfDragons)

        // Resolve the enchantment spell
        driver.bothPass()

        // Resolve the ETB trigger
        driver.bothPass()

        // Creatures should be in exile
        driver.isInExile(activePlayer, "Glory Seeker") shouldBe true
        driver.isInExile(activePlayer, "Elvish Aberration") shouldBe true
        driver.findPermanent(activePlayer, "Glory Seeker") shouldBe null
        driver.findPermanent(activePlayer, "Elvish Aberration") shouldBe null

        // Two 5/5 Dragon tokens should be on the battlefield
        val dragons = driver.findAllPermanentsByName("Dragon Token")
        dragons shouldHaveSize 2

        // Day of the Dragons should have LinkedExileComponent with 2 exiled IDs
        val dayId = driver.findPermanent(activePlayer, "Day of the Dragons")
        dayId shouldNotBe null
        val linked = driver.state.getEntity(dayId!!)?.get<LinkedExileComponent>()
        linked shouldNotBe null
        linked!!.exiledIds shouldHaveSize 2
    }

    test("ETB does not exile opponent's creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Glory Seeker" to 10, "Elvish Aberration" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Active player has 1 creature
        driver.putCreatureOnBattlefield(activePlayer, "Glory Seeker")
        // Opponent has 1 creature
        driver.putCreatureOnBattlefield(opponent, "Elvish Aberration")

        val dayOfDragons = driver.putCardInHand(activePlayer, "Day of the Dragons")
        driver.giveMana(activePlayer, Color.BLUE, 7)

        driver.castSpell(activePlayer, dayOfDragons)
        driver.bothPass() // resolve spell
        driver.bothPass() // resolve ETB

        // Opponent's creature should still be on battlefield
        driver.findPermanent(opponent, "Elvish Aberration") shouldNotBe null

        // Only 1 Dragon token (for 1 creature exiled)
        driver.findAllPermanentsByName("Dragon Token") shouldHaveSize 1
    }

    test("LTB sacrifices Dragons and returns exiled creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Glory Seeker" to 10, "Elvish Aberration" to 10, "Plains" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 2 creatures on battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Glory Seeker")
        driver.putCreatureOnBattlefield(activePlayer, "Elvish Aberration")

        // Cast and resolve Day of the Dragons + ETB
        val dayOfDragons = driver.putCardInHand(activePlayer, "Day of the Dragons")
        driver.giveMana(activePlayer, Color.BLUE, 7)
        driver.castSpell(activePlayer, dayOfDragons)
        driver.bothPass() // resolve spell
        driver.bothPass() // resolve ETB

        // Verify setup
        driver.findPermanent(activePlayer, "Day of the Dragons") shouldNotBe null
        driver.findAllPermanentsByName("Dragon Token") shouldHaveSize 2
        driver.findPermanent(activePlayer, "Glory Seeker") shouldBe null
        driver.findPermanent(activePlayer, "Elvish Aberration") shouldBe null

        // Now cast Wipe Clean targeting Day of the Dragons to trigger LTB
        val wipeClean = driver.putCardInHand(activePlayer, "Wipe Clean")
        driver.giveMana(activePlayer, Color.WHITE, 2)
        val dayId = driver.findPermanent(activePlayer, "Day of the Dragons")!!
        driver.castSpellWithTargets(activePlayer, wipeClean, listOf(ChosenTarget.Permanent(dayId)))
        driver.bothPass() // resolve Wipe Clean (exiles enchantment, LTB triggers)
        driver.bothPass() // resolve LTB trigger (sacrifices Dragons, returns creatures)

        // Day of the Dragons should be gone
        driver.findPermanent(activePlayer, "Day of the Dragons") shouldBe null

        // Dragon tokens should be gone (sacrificed)
        driver.findAllPermanentsByName("Dragon Token") shouldHaveSize 0

        // Original creatures should be back on the battlefield
        driver.findPermanent(activePlayer, "Glory Seeker") shouldNotBe null
        driver.findPermanent(activePlayer, "Elvish Aberration") shouldNotBe null
    }

    test("ETB with no creatures creates no tokens") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val dayOfDragons = driver.putCardInHand(activePlayer, "Day of the Dragons")
        driver.giveMana(activePlayer, Color.BLUE, 7)

        driver.castSpell(activePlayer, dayOfDragons)
        driver.bothPass() // resolve spell
        driver.bothPass() // resolve ETB

        // No tokens created
        driver.findAllPermanentsByName("Dragon Token") shouldHaveSize 0

        // Enchantment should still be on battlefield
        driver.findPermanent(activePlayer, "Day of the Dragons") shouldNotBe null
    }
})
