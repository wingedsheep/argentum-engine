package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.Hushbringer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hushbringer — "Creatures entering or dying don't cause abilities to trigger."
 *
 * The entering half is Torpor Orb's lock (covered by its tests); this pins the card's dying half:
 * a creature's own dies trigger stays silent while Hushbringer is out, and fires without it.
 * The engine-level rulings (look-back, from-anywhere, sacrifice triggers) live in
 * `SuppressDyingTriggersTest`.
 */
class HushbringerScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Hushbringer)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to you
    }

    fun GameTestDriver.boltAndSettle(you: EntityId, target: EntityId) {
        val bolt = putCardInHand(you, "Lightning Bolt")
        giveMana(you, Color.RED, 1)
        castSpell(you, bolt, listOf(target)).outcome shouldBe Outcome.Done
        var guard = 0
        while (!isPaused && state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    test("control: without Hushbringer, the creature's dies trigger fires") {
        val (driver, you) = setup()
        val victim = driver.putCreatureOnBattlefield(you, "Death Trigger Test Creature")
        driver.boltAndSettle(you, victim)
        withClue("\"When this creature dies, you gain 3 life\" resolved") { driver.getLifeTotal(you) shouldBe 23 }
    }

    test("a creature dying causes no triggers while Hushbringer is out") {
        val (driver, you) = setup()
        driver.putCreatureOnBattlefield(you, "Hushbringer")
        val victim = driver.putCreatureOnBattlefield(you, "Death Trigger Test Creature")
        driver.boltAndSettle(you, victim)
        withClue("the creature died") { driver.findPermanent(you, "Death Trigger Test Creature") shouldBe null }
        withClue("its dies trigger never fired") { driver.getLifeTotal(you) shouldBe 20 }
    }
})
