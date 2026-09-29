package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Vengeant Earth (MOM #212).
 *
 * {1}{G} Instant
 * "Target creature or land you control becomes a 4/4 Elemental creature with haste in addition to
 * its other types until end of turn. It must be blocked this turn if able."
 */
class VengeantEarthScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    test("a land you control becomes a 4/4 Elemental land creature with haste until end of turn") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putLandOnBattlefield(p1, "Forest")
        val spell = driver.putCardInHand(p1, "Vengeant Earth")
        driver.giveMana(p1, Color.GREEN, 2)

        driver.castSpell(p1, spell, targets = listOf(forest)).outcome shouldBe Outcome.Done
        driver.bothPass()

        val projected = projector.project(driver.state)
        projected.hasType(forest, "CREATURE") shouldBe true
        projected.hasType(forest, "LAND") shouldBe true
        projected.hasSubtype(forest, "Elemental") shouldBe true
        projected.hasSubtype(forest, "Forest") shouldBe true
        projected.getPower(forest) shouldBe 4
        projected.getToughness(forest) shouldBe 4
        projected.hasKeyword(forest, Keyword.HASTE) shouldBe true

        driver.passPriorityUntil(Step.UPKEEP)
        val nextTurn = projector.project(driver.state)
        nextTurn.hasType(forest, "CREATURE") shouldBe false
        nextTurn.hasSubtype(forest, "Elemental") shouldBe false
    }

    test("a creature you control keeps its creature types and becomes a 4/4 Elemental") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val spell = driver.putCardInHand(p1, "Vengeant Earth")
        driver.giveMana(p1, Color.GREEN, 2)

        driver.castSpell(p1, spell, targets = listOf(bears)).outcome shouldBe Outcome.Done
        driver.bothPass()

        val projected = projector.project(driver.state)
        projected.getPower(bears) shouldBe 4
        projected.getToughness(bears) shouldBe 4
        projected.hasSubtype(bears, "Bear") shouldBe true
        projected.hasSubtype(bears, "Elemental") shouldBe true
        projected.hasKeyword(bears, Keyword.HASTE) shouldBe true
    }

    test("cannot target a creature an opponent controls") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val theirBears = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val spell = driver.putCardInHand(p1, "Vengeant Earth")
        driver.giveMana(p1, Color.GREEN, 2)

        driver.castSpell(p1, spell, targets = listOf(theirBears)).outcome shouldNotBe Outcome.Done
    }

    test("the animated land attacks with haste and must be blocked if able") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putLandOnBattlefield(p1, "Forest")
        val blocker = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val spell = driver.putCardInHand(p1, "Vengeant Earth")
        driver.giveMana(p1, Color.GREEN, 2)

        driver.castSpell(p1, spell, targets = listOf(forest)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(forest), p2).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        driver.declareBlockers(p2, emptyMap()).outcome shouldNotBe Outcome.Done
        driver.declareBlockers(p2, mapOf(blocker to listOf(forest))).outcome shouldBe Outcome.Done
    }
})
