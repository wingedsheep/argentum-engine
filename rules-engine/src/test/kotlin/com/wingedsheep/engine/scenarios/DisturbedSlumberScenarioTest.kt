package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Disturbed Slumber (LCI #182).
 *
 * {1}{G} Instant
 * "Until end of turn, target land you control becomes a 4/4 Dinosaur creature with reach and
 * haste. It's still a land. It must be blocked this turn if able."
 */
class DisturbedSlumberScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("target land becomes a 4/4 Dinosaur with reach and haste, and is still a land") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putLandOnBattlefield(activePlayer, "Forest")
        val spell = driver.putCardInHand(activePlayer, "Disturbed Slumber")
        // {1}{G}: give 2 green so the generic {1} and the colored {G} are both covered.
        driver.giveMana(activePlayer, Color.GREEN, 2)

        val castResult = driver.castSpell(activePlayer, spell, targets = listOf(forest))
        castResult.isSuccess shouldBe true

        driver.bothPass()

        val projected = projector.project(driver.state)
        // The land is now a creature.
        projected.hasType(forest, "CREATURE") shouldBe true
        // It's still a land ("it's still a land").
        projected.hasType(forest, "LAND") shouldBe true
        // Base P/T is 4/4.
        projected.getPower(forest) shouldBe 4
        projected.getToughness(forest) shouldBe 4
        // Creature type is Dinosaur.
        projected.hasSubtype(forest, "DINOSAUR") shouldBe true
        // Granted keywords: reach and haste.
        projected.hasKeyword(forest, Keyword.REACH) shouldBe true
        projected.hasKeyword(forest, Keyword.HASTE) shouldBe true
    }

    test("animated land must be blocked if able — declining to block is illegal") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putLandOnBattlefield(p1, "Forest")
        driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val spell = driver.putCardInHand(p1, "Disturbed Slumber")
        driver.giveMana(p1, Color.GREEN, 2)

        driver.castSpell(p1, spell, targets = listOf(forest)).isSuccess shouldBe true
        driver.bothPass()

        // The animated land has haste, so it can attack the turn it was animated.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(forest), p2).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // p2 controls a Grizzly Bears that can block, so declaring no blockers is illegal.
        driver.declareBlockers(p2, emptyMap()).isSuccess shouldBe false

        // Blocking the animated land is legal.
        val bears = driver.findPermanent(p2, "Grizzly Bears")!!
        driver.declareBlockers(p2, mapOf(bears to listOf(forest))).isSuccess shouldBe true
    }

    test("two must-be-blocked attackers, one blocker — blocking either is legal (Rule 509.1c)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putLandOnBattlefield(p1, "Forest")
        val swamp = driver.putLandOnBattlefield(p1, "Swamp")
        driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        val spell1 = driver.putCardInHand(p1, "Disturbed Slumber")
        val spell2 = driver.putCardInHand(p1, "Disturbed Slumber")
        driver.giveMana(p1, Color.GREEN, 4)

        driver.castSpell(p1, spell1, targets = listOf(forest)).isSuccess shouldBe true
        driver.bothPass()
        driver.castSpell(p1, spell2, targets = listOf(swamp)).isSuccess shouldBe true
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(forest, swamp), p2).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        val bears = driver.findPermanent(p2, "Grizzly Bears")!!

        // Declining to block is still illegal — the Bears must block one of them.
        driver.declareBlockers(p2, emptyMap()).isSuccess shouldBe false

        // One blocker can only satisfy one requirement: blocking either attacker is legal.
        driver.declareBlockers(p2, mapOf(bears to listOf(forest))).isSuccess shouldBe true
    }

    test("two must-be-blocked attackers, two blockers — both must be blocked") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putLandOnBattlefield(p1, "Forest")
        val swamp = driver.putLandOnBattlefield(p1, "Swamp")
        val bears1 = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val bears2 = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        val spell1 = driver.putCardInHand(p1, "Disturbed Slumber")
        val spell2 = driver.putCardInHand(p1, "Disturbed Slumber")
        driver.giveMana(p1, Color.GREEN, 4)

        driver.castSpell(p1, spell1, targets = listOf(forest)).isSuccess shouldBe true
        driver.bothPass()
        driver.castSpell(p1, spell2, targets = listOf(swamp)).isSuccess shouldBe true
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(forest, swamp), p2).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // Both requirements can be satisfied, so leaving one attacker unblocked is illegal —
        // whether by blocking only one attacker or by stacking both blockers on one.
        driver.declareBlockers(p2, mapOf(bears1 to listOf(forest))).isSuccess shouldBe false
        driver.declareBlockers(p2, mapOf(bears1 to listOf(forest), bears2 to listOf(forest))).isSuccess shouldBe false

        // Covering both attackers is legal.
        driver.declareBlockers(
            p2,
            mapOf(bears1 to listOf(forest), bears2 to listOf(swamp))
        ).isSuccess shouldBe true
    }

    test("animated land reverts to a non-creature land at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putLandOnBattlefield(activePlayer, "Forest")
        val spell = driver.putCardInHand(activePlayer, "Disturbed Slumber")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        driver.castSpell(activePlayer, spell, targets = listOf(forest))
        driver.bothPass()

        // Confirm the land is animated mid-turn.
        val midTurn = projector.project(driver.state)
        midTurn.hasType(forest, "CREATURE") shouldBe true

        // Advance to the opponent's upkeep — the end-of-turn cleanup fires and the effect expires.
        driver.passPriorityUntil(Step.UPKEEP)

        val nextTurn = projector.project(driver.state)
        nextTurn.hasType(forest, "CREATURE") shouldBe false
        nextTurn.hasType(forest, "LAND") shouldBe true
    }
})
