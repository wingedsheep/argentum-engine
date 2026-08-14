package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.ArchangelAvacyn
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Archangel Avacyn // Avacyn, the Purifier (SOI) — the delayed upkeep flip.
 *
 * Covers the two printed rulings that make this card awkward: the delayed trigger fires at the next
 * upkeep whoever's turn it is, and a second queued flip must **not** turn her back over when several
 * non-Angel creatures died in the same turn.
 */
class ArchangelAvacynScenarioTest : FunSpec({

    val projector = StateProjector()

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ArchangelAvacyn)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun faceName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    /** Bolt [victim] and let the bolt (plus anything it triggers) finish resolving. */
    fun bolt(driver: GameTestDriver, caster: EntityId, victim: EntityId) {
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Permanent(victim)))
        var guard = 0
        while (guard++ < 20 && driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    /** Advance into the next upkeep (the opponent's) and drain whatever triggers there. */
    fun advanceToNextUpkeep(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        var guard = 0
        while (guard++ < 20 && driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    test("entering grants your creatures indestructible until end of turn") {
        val (driver, you) = newGame()
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        val avacyn = driver.putCardInHand(you, "Archangel Avacyn")
        driver.giveMana(you, Color.WHITE, 5)
        driver.castSpell(you, avacyn)
        driver.bothPass() // Avacyn resolves; the ETB trigger goes on the stack
        driver.bothPass() // the ETB trigger resolves

        // 3 damage would normally kill a 2/2; indestructible keeps it around.
        bolt(driver, you, bears)
        driver.state.getBattlefield().contains(bears) shouldBe true
    }

    test("a non-Angel creature dying queues a flip that resolves at the next upkeep") {
        val (driver, you) = newGame()
        val avacyn = driver.putCreatureOnBattlefield(you, "Archangel Avacyn")
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        bolt(driver, you, bears)

        // Still front-face this turn — the flip is delayed to the next upkeep, not immediate.
        faceName(driver, avacyn) shouldBe "Archangel Avacyn"

        advanceToNextUpkeep(driver)

        faceName(driver, avacyn) shouldBe "Avacyn, the Purifier"
        val projected = projector.project(driver.state)
        projected.getPower(avacyn) shouldBe 6
        projected.getToughness(avacyn) shouldBe 5
    }

    test("transforming deals 3 damage to each other creature and each opponent") {
        val (driver, you) = newGame()
        val opponent = driver.getOpponent(you)
        val avacyn = driver.putCreatureOnBattlefield(you, "Archangel Avacyn")
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        bolt(driver, you, bears)
        advanceToNextUpkeep(driver)

        faceName(driver, avacyn) shouldBe "Avacyn, the Purifier"
        // Every *other* creature took 3 — the opponent's 2/2 is gone, Avacyn herself is untouched.
        driver.state.getBattlefield().contains(theirBears) shouldBe false
        driver.state.getBattlefield().contains(avacyn) shouldBe true
        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(you) shouldBe 20
    }

    test("two deaths in one turn flip her once, never back again") {
        val (driver, you) = newGame()
        val avacyn = driver.putCreatureOnBattlefield(you, "Archangel Avacyn")
        val first = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val second = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        bolt(driver, you, first)
        bolt(driver, you, second)

        advanceToNextUpkeep(driver)

        // Both delayed triggers fire in the same upkeep; the second finds her already flipped and
        // does nothing (printed ruling), rather than turning her back to the front face.
        faceName(driver, avacyn) shouldBe "Avacyn, the Purifier"
    }
})
