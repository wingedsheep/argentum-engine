package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.PartInFriendship
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Part in Friendship {4}{G} — Enchantment
 *   Whenever a nontoken creature you control dies, reveal cards from the top of your library
 *   until you reveal a creature card. If its mana value is less than or equal to the number of
 *   lands you control, put it onto the battlefield. Otherwise, put it into your hand. Put the
 *   rest on the bottom of your library in a random order. This ability triggers only once each
 *   turn.
 *
 * The branch is the interesting part: the same revealed creature goes to the battlefield or to
 * hand purely on the mana-value-vs-lands comparison, and the cards revealed underneath it must
 * end up on the bottom of the library either way rather than following it.
 */
class PartInFriendshipScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PartInFriendship))
        return driver
    }

    /**
     * Board: the enchantment out, [lands] Forests on the battlefield, a doomed Savannah Lions to
     * kill, and a library topped by two Forests over [creature]. Bolts the Lions and lets the
     * trigger resolve.
     */
    fun setUpAndKill(driver: GameTestDriver, lands: Int, creature: String) {
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(you, "Part in Friendship")
        repeat(lands) { driver.putPermanentOnBattlefield(you, "Forest") }

        val doomed = driver.putCreatureOnBattlefield(you, "Savannah Lions")

        // Library from the top: Forest, Forest, then the creature the reveal will stop on.
        driver.putCardOnTopOfLibrary(you, creature)
        driver.putCardOnTopOfLibrary(you, "Forest")
        driver.putCardOnTopOfLibrary(you, "Forest")

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(doomed)).isSuccess shouldBe true
        driver.bothPass() // Lightning Bolt kills the Lions
        driver.bothPass() // Part in Friendship's trigger
    }

    test("a creature whose mana value fits the land count hits the battlefield") {
        val driver = createDriver()
        // Centaur Courser is {2}{G} — mana value 3, and we control four lands.
        setUpAndKill(driver, lands = 4, creature = "Centaur Courser")
        val you = driver.activePlayer!!

        driver.getPermanents(you).count { driver.getCardName(it) == "Centaur Courser" } shouldBe 1
        driver.getHand(you).count { driver.getCardName(it) == "Centaur Courser" } shouldBe 0
        // The two Forests revealed above it went to the bottom, not along with it.
        driver.getPermanents(you).count { driver.getCardName(it) == "Forest" } shouldBe 4
    }

    test("a creature too expensive for the land count goes to hand instead") {
        val driver = createDriver()
        // Force of Nature is {2}{G}{G}{G} — mana value 5, against only two lands.
        setUpAndKill(driver, lands = 2, creature = "Force of Nature")
        val you = driver.activePlayer!!

        driver.getPermanents(you).count { driver.getCardName(it) == "Force of Nature" } shouldBe 0
        driver.getHand(you).count { driver.getCardName(it) == "Force of Nature" } shouldBe 1
    }

    test("the ability triggers only once each turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(you, "Part in Friendship")
        repeat(4) { driver.putPermanentOnBattlefield(you, "Forest") }

        val first = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        val second = driver.putCreatureOnBattlefield(you, "Savannah Lions")

        driver.putCardOnTopOfLibrary(you, "Centaur Courser")
        driver.putCardOnTopOfLibrary(you, "Centaur Courser")

        val firstBolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, firstBolt, listOf(first)).isSuccess shouldBe true
        driver.bothPass()
        driver.bothPass()

        val secondBolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, secondBolt, listOf(second)).isSuccess shouldBe true
        driver.bothPass()
        driver.bothPass()

        // Only the first death found a Courser; the second death didn't trigger at all.
        driver.getPermanents(you).count { driver.getCardName(it) == "Centaur Courser" } shouldBe 1
    }
})
