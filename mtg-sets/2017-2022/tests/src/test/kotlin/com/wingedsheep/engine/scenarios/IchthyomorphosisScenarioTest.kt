package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.Ornithopter
import com.wingedsheep.mtg.sets.definitions.thb.cards.Ichthyomorphosis
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ichthyomorphosis — "Enchanted creature loses all abilities and is a blue Fish with base power
 * and toughness 0/1."
 *
 * The ruling: it overwrites colors and creature types, but the creature keeps its supertypes and
 * every other card type (CR 205.1a — "is a Fish" names no card type).
 */
class IchthyomorphosisScenarioTest : FunSpec({

    val projector = StateProjector()

    fun enchant(creatureName: String): Pair<GameTestDriver, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Ichthyomorphosis, Ornithopter))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(player, creatureName)
        val aura = driver.putCardInHand(player, "Ichthyomorphosis")
        driver.giveMana(player, Color.BLUE, 4)
        driver.castSpell(player, aura, listOf(creature))
        driver.bothPass()
        return driver to creature
    }

    test("an artifact creature stays an artifact but becomes a blue 0/1 Fish with no abilities") {
        val (driver, thopter) = enchant("Ornithopter")
        val projected = driver.state.projectedState

        projected.hasType(thopter, "ARTIFACT") shouldBe true
        projected.hasType(thopter, "CREATURE") shouldBe true
        projected.hasSubtype(thopter, "Fish") shouldBe true
        projected.hasSubtype(thopter, "Thopter") shouldBe false
        projected.hasColor(thopter, Color.BLUE) shouldBe true
        projected.hasKeyword(thopter, Keyword.FLYING) shouldBe false
        projector.getProjectedPower(driver.state, thopter) shouldBe 0
        projector.getProjectedToughness(driver.state, thopter) shouldBe 1
    }

    test("a legendary red creature stays legendary but is only blue and only a Fish") {
        val (driver, prospector) = enchant("Test Hasty Prospector")
        val projected = driver.state.projectedState

        projected.isLegendary(prospector) shouldBe true
        projected.hasColor(prospector, Color.RED) shouldBe false
        projected.hasColor(prospector, Color.BLUE) shouldBe true
        projected.hasSubtype(prospector, "Monkey") shouldBe false
        projected.hasSubtype(prospector, "Fish") shouldBe true
        projected.hasKeyword(prospector, Keyword.HASTE) shouldBe false
    }
})
