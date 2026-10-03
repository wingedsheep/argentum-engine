package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.definitions.mh3.cards.CursedWombat
import com.wingedsheep.mtg.sets.definitions.mh3.cards.GhostfireSlice
import com.wingedsheep.mtg.sets.definitions.mh3.cards.SnappingVoidcraw
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Ghostfire Slice (MH3 #123) — {2}{R} Instant, devoid.
 *
 * "This spell costs {2} less to cast if an opponent controls a multicolored permanent.
 * Ghostfire Slice deals 4 damage to any target."
 */
class GhostfireSliceScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GhostfireSlice, CursedWombat, SnappingVoidcraw))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val players = listOf(driver.player1, driver.player2)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to players
    }

    fun resolveStack(driver: GameTestDriver) {
        while (driver.state.stack.isNotEmpty()) driver.passPriority(driver.state.priorityPlayerId!!)
    }

    test("opponent controls a multicolored permanent: costs only {R} and deals 4 to a player") {
        val (driver, players) = setup()
        val (you, opp) = players
        driver.putCreatureOnBattlefield(opp, "Cursed Wombat")
        val spell = driver.putCardInHand(you, "Ghostfire Slice")
        driver.giveMana(you, Color.RED, 1)
        val lifeBefore = driver.getLifeTotal(opp)

        driver.castSpellWithTargets(you, spell, listOf(ChosenTarget.Player(opp))).error shouldBe null
        resolveStack(driver)

        driver.getLifeTotal(opp) shouldBe lifeBefore - 4
    }

    test("no multicolored permanent: {R} alone cannot pay") {
        val (driver, players) = setup()
        val (you, _) = players
        val spell = driver.putCardInHand(you, "Ghostfire Slice")
        driver.giveMana(you, Color.RED, 1)

        driver.castSpellWithTargets(you, spell, listOf(ChosenTarget.Player(players[1]))).error.shouldNotBeNull()
    }

    test("a devoid (colorless) permanent doesn't count as multicolored") {
        val (driver, players) = setup()
        val (you, opp) = players
        driver.putCreatureOnBattlefield(opp, "Snapping Voidcraw")
        val spell = driver.putCardInHand(you, "Ghostfire Slice")
        driver.giveMana(you, Color.RED, 1)

        driver.castSpellWithTargets(you, spell, listOf(ChosenTarget.Player(opp))).error.shouldNotBeNull()
    }

    test("your own multicolored permanent doesn't reduce the cost") {
        val (driver, players) = setup()
        val (you, opp) = players
        driver.putCreatureOnBattlefield(you, "Cursed Wombat")
        val spell = driver.putCardInHand(you, "Ghostfire Slice")
        driver.giveMana(you, Color.RED, 1)

        driver.castSpellWithTargets(you, spell, listOf(ChosenTarget.Player(opp))).error.shouldNotBeNull()
    }

    test("full {2}{R} deals 4 damage to a creature") {
        val (driver, players) = setup()
        val (you, opp) = players
        val voidcraw = driver.putCreatureOnBattlefield(opp, "Snapping Voidcraw")
        val spell = driver.putCardInHand(you, "Ghostfire Slice")
        driver.giveMana(you, Color.RED, 3)

        driver.castSpellWithTargets(you, spell, listOf(ChosenTarget.Permanent(voidcraw))).error shouldBe null
        resolveStack(driver)

        driver.findPermanent(opp, "Snapping Voidcraw") shouldBe null
    }
})
