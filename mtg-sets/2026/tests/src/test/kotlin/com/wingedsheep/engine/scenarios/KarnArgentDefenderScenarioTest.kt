package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fra.cards.InfiniteCoursework
import com.wingedsheep.mtg.sets.definitions.fra.cards.KarnArgentDefender
import com.wingedsheep.mtg.sets.definitions.mkm.cards.NightdrinkerMoroii
import com.wingedsheep.mtg.sets.definitions.mkm.cards.PolygraphOrb
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Karn, Argent Defender — "Artifacts and creatures entering the battlefield don't cause abilities
 * to trigger."
 *
 * A `SuppressZoneChangeTriggers` widened to `CreatureOrArtifact`. The noncreature-artifact case is the
 * one that fails if the widening is missing; the creature case covers the other half, and the
 * control runs prove the triggers do fire without Karn. Karn's lock is an ability of Karn's, so it
 * stops once Karn loses all abilities (issue #3240).
 */
class KarnArgentDefenderScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(KarnArgentDefender)
        driver.registerCard(InfiniteCoursework)
        driver.registerCard(NightdrinkerMoroii)
        driver.registerCard(PolygraphOrb)
        return driver
    }

    fun GameTestDriver.castAndSettle(you: EntityId, cardName: String, black: Int) {
        val card = putCardInHand(you, cardName)
        giveMana(you, Color.BLACK, black)
        castSpell(you, card).outcome shouldBe Outcome.Done
        var guard = 0
        while (!isPaused && state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    fun setUp(withKarn: Boolean): Pair<GameTestDriver, EntityId> {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        if (withKarn) driver.putCreatureOnBattlefield(you, "Karn, Argent Defender")
        return driver to you
    }

    test("control: without Karn, the creature's and the artifact's enters triggers fire") {
        val (driver, you) = setUp(withKarn = false)
        driver.castAndSettle(you, "Nightdrinker Moroii", 4)
        withClue("the Vampire's enters trigger costs 3 life") { driver.getLifeTotal(you) shouldBe 17 }

        val (driver2, you2) = setUp(withKarn = false)
        driver2.castAndSettle(you2, "Polygraph Orb", 5)
        withClue("the Orb's enters trigger pauses on its dig-two selection") {
            driver2.state.pendingDecision shouldNotBe null
        }
    }

    test("a creature entering causes no triggers while Karn is out") {
        val (driver, you) = setUp(withKarn = true)
        driver.castAndSettle(you, "Nightdrinker Moroii", 4)

        withClue("the Vampire's \"you lose 3 life\" never triggered") { driver.getLifeTotal(you) shouldBe 20 }
        withClue("the creature itself still entered") {
            driver.findPermanent(you, "Nightdrinker Moroii") shouldNotBe null
        }
    }

    test("a noncreature artifact entering causes no triggers either") {
        val (driver, you) = setUp(withKarn = true)
        driver.castAndSettle(you, "Polygraph Orb", 5)

        withClue("the Orb's dig-and-lose-2-life trigger never went on the stack") {
            driver.getLifeTotal(you) shouldBe 20
            driver.state.pendingDecision shouldBe null
        }
        withClue("the artifact itself still entered") {
            driver.findPermanent(you, "Polygraph Orb") shouldNotBe null
        }
    }

    test("once Karn loses all abilities, a creature entering triggers again") {
        val (driver, you) = setUp(withKarn = true)
        val karn = driver.findPermanent(you, "Karn, Argent Defender")!!

        val coursework = driver.putCardInHand(you, "Infinite Coursework")
        driver.giveMana(you, Color.BLUE, 3)
        driver.castSpell(you, coursework, listOf(karn)).outcome shouldBe Outcome.Done
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()
        withClue("Infinite Coursework stripped Karn's abilities") {
            driver.state.projectedState.hasLostAllAbilities(karn) shouldBe true
        }

        driver.castAndSettle(you, "Nightdrinker Moroii", 4)
        withClue("the Vampire's enters trigger fired: you lost 3 life") { driver.getLifeTotal(you) shouldBe 17 }
    }
})
