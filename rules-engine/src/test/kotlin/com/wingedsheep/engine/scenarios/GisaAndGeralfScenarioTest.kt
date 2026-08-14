package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.GisaAndGeralf
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gisa and Geralf — {2}{U}{B} Legendary Creature — Human Wizard 4/4
 * "When Gisa and Geralf enters, mill four cards."
 * "Once during each of your turns, you may cast a Zombie creature spell from your graveyard."
 *
 * Covers the three things `MayCastFromGraveyard(oncePerTurn = true)` has to get right: the
 * permission is filtered (a non-Zombie in the same graveyard is never offered), it is spent by the
 * first cast and gone for the rest of that turn, and it comes back on the controller's next turn.
 */
class GisaAndGeralfScenarioTest : FunSpec({

    val shamblingZombie = card("Shambling Zombie") {
        manaCost = "{1}{B}"
        colorIdentity = "B"
        typeLine = "Creature — Zombie"
        power = 2
        toughness = 2
    }
    val secondZombie = card("Rotting Zombie") {
        manaCost = "{1}{B}"
        colorIdentity = "B"
        typeLine = "Creature — Zombie"
        power = 1
        toughness = 3
    }
    val plainBeast = card("Graveyard Beast") {
        manaCost = "{1}{B}"
        colorIdentity = "B"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GisaAndGeralf, shamblingZombie, secondZombie, plainBeast))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun graveyardCasts(driver: GameTestDriver, player: EntityId, cardId: EntityId) =
        driver.legalActions(player)
            .filter { it.sourceZone == "GRAVEYARD" && (it.action as? CastSpell)?.cardId == cardId }

    fun giveZombieMana(driver: GameTestDriver, player: EntityId) {
        driver.giveMana(driver.activePlayer ?: player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 1)
    }

    /** Pass until the turn rolls over — the UNTAP step never holds priority. */
    fun endTurn(driver: GameTestDriver) {
        val startTurn = driver.state.turnNumber
        var guard = 0
        while (driver.state.turnNumber == startTurn && guard++ < 300) {
            if (driver.isPaused) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    test("entering mills four cards") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.getGraveyard(player) shouldHaveSize 0

        val gisa = driver.putCardInHand(player, "Gisa and Geralf")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 2)
        driver.castSpell(player, gisa).isSuccess shouldBe true
        // One pass resolves the creature spell, the next resolves the enters trigger it put on the stack.
        driver.bothPass()
        driver.bothPass()

        driver.findPermanent(player, "Gisa and Geralf") shouldNotBe null
        driver.getGraveyard(player) shouldHaveSize 4
    }

    test("the grant covers a Zombie creature in the graveyard but not a non-Zombie") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Gisa and Geralf")

        val zombie = driver.putCardInGraveyard(player, "Shambling Zombie")
        val beast = driver.putCardInGraveyard(player, "Graveyard Beast")
        giveZombieMana(driver, player)

        graveyardCasts(driver, player, zombie) shouldHaveSize 1
        withClue("Graveyard Beast is a Beast, not a Zombie — no permission applies") {
            graveyardCasts(driver, player, beast) shouldHaveSize 0
        }
    }

    test("the permission is spent by the first graveyard cast and unavailable again that turn") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Gisa and Geralf")

        val first = driver.putCardInGraveyard(player, "Shambling Zombie")
        val second = driver.putCardInGraveyard(player, "Rotting Zombie")
        giveZombieMana(driver, player)

        driver.submit(CastSpell(playerId = player, cardId = first)).isSuccess shouldBe true
        driver.bothPass()
        driver.findPermanent(player, "Shambling Zombie") shouldNotBe null

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        giveZombieMana(driver, player)
        withClue("once during each of your turns — the second Zombie is no longer castable") {
            graveyardCasts(driver, player, second) shouldHaveSize 0
        }
    }

    test("the permission refreshes on the controller's next turn") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Gisa and Geralf")

        val first = driver.putCardInGraveyard(player, "Shambling Zombie")
        val second = driver.putCardInGraveyard(player, "Rotting Zombie")
        giveZombieMana(driver, player)
        driver.submit(CastSpell(playerId = player, cardId = first)).isSuccess shouldBe true
        driver.bothPass()

        // Round the table back to the controller's own precombat main: a fresh use.
        endTurn(driver)
        endTurn(driver)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe player
        giveZombieMana(driver, player)
        graveyardCasts(driver, player, second) shouldHaveSize 1
    }
})
