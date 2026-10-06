package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j22.cards.ZaskSkitteringSwarmlord
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Zask, Skittering Swarmlord — {3}{G}{G} Legendary Creature — Insect 5/5.
 *
 * Covers the two compositions the snapshot can't see: the graveyard permission is split into a
 * land grant and an Insect-filtered cast grant (a non-Insect isn't offered), and the death
 * trigger moves the dead Insect out of the graveyard to the bottom of its owner's library before
 * milling two.
 */
class ZaskSkitteringSwarmlordScenarioTest : FunSpec({

    val insect = card("Test Beetle") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Insect"
        power = 2
        toughness = 2
    }
    val beast = card("Test Beast") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ZaskSkitteringSwarmlord, insect, beast))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun graveyardActions(driver: GameTestDriver, player: EntityId, cardId: EntityId) =
        driver.legalActions(player).filter {
            it.sourceZone == "GRAVEYARD" &&
                ((it.action as? CastSpell)?.cardId == cardId || (it.action as? PlayLand)?.cardId == cardId)
        }

    test("lands and Insect spells are playable from the graveyard, other spells are not") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Zask, Skittering Swarmlord")

        val beetle = driver.putCardInGraveyard(player, "Test Beetle")
        val beastCard = driver.putCardInGraveyard(player, "Test Beast")
        val forest = driver.putCardInGraveyard(player, "Forest")
        driver.giveMana(player, Color.GREEN, 2)

        graveyardActions(driver, player, beetle) shouldHaveSize 1
        graveyardActions(driver, player, forest) shouldHaveSize 1
        withClue("a Beast is not an Insect — no permission applies") {
            graveyardActions(driver, player, beastCard) shouldHaveSize 0
        }

        driver.submit(CastSpell(playerId = player, cardId = beetle)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(player, "Test Beetle") shouldBe beetle
    }

    test("another Insect dying goes to the bottom of its owner's library, then two cards are milled") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Zask, Skittering Swarmlord")
        val beetle = driver.putPermanentOnBattlefield(player, "Test Beetle")
        val libraryBefore = driver.state.getZone(ZoneKey(player, Zone.LIBRARY)).size

        val doomBlade = driver.putCardInHand(player, "Doom Blade")
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 1)
        driver.castSpell(player, doomBlade, listOf(beetle)).outcome shouldBe Outcome.Done
        driver.bothPass() // Doom Blade resolves; Zask's trigger goes on the stack
        driver.bothPass() // trigger resolves

        val library = driver.state.getZone(ZoneKey(player, Zone.LIBRARY))
        library.last() shouldBe beetle
        library.size shouldBe libraryBefore + 1 - 2
        val graveyard = driver.getGraveyard(player)
        graveyard shouldNotContain beetle
        graveyard shouldContain doomBlade
        graveyard shouldHaveSize 3
    }

    test("a non-Insect dying does not trigger") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Zask, Skittering Swarmlord")
        val beastId = driver.putPermanentOnBattlefield(player, "Test Beast")
        val libraryBefore = driver.state.getZone(ZoneKey(player, Zone.LIBRARY)).size

        val doomBlade = driver.putCardInHand(player, "Doom Blade")
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 1)
        driver.castSpell(player, doomBlade, listOf(beastId)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.getZone(ZoneKey(player, Zone.LIBRARY)).size shouldBe libraryBefore
        driver.getGraveyard(player) shouldContain beastId
    }
})
