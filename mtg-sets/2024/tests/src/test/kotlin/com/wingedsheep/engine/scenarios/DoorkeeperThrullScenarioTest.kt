package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.DoorkeeperThrull
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
import com.wingedsheep.engine.core.Outcome

/**
 * Doorkeeper Thrull — "Artifacts and creatures entering don't cause abilities to trigger."
 *
 * The card is a plain `SuppressZoneChangeTriggers` widened to `CreatureOrArtifact`, so what is worth
 * testing is that the *widening* actually reaches both halves. The suppressed-creature case would
 * pass on the static's `GameObjectFilter.Creature` default too; the noncreature-artifact case is the
 * one that fails if the filter is wrong, which is why both are here — together with a control run
 * proving the triggers do fire when the Thrull isn't on the battlefield.
 */
class DoorkeeperThrullScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DoorkeeperThrull)
        driver.registerCard(NightdrinkerMoroii)
        driver.registerCard(PolygraphOrb)
        return driver
    }

    /** Cast [cardName] for [black] black mana and let the stack drain. */
    fun GameTestDriver.castAndSettle(you: EntityId, cardName: String, black: Int) {
        val card = putCardInHand(you, cardName)
        giveMana(you, Color.BLACK, black)
        castSpell(you, card).outcome shouldBe Outcome.Done
        var guard = 0
        while (!isPaused && state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    test("control: without the Thrull, the creature's enters trigger fires") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.castAndSettle(you, "Nightdrinker Moroii", 4)
        withClue("the Vampire's enters trigger costs 3 life") {
            driver.getLifeTotal(you) shouldBe 17
        }
    }

    test("control: without the Thrull, the artifact's enters trigger fires") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.castAndSettle(you, "Polygraph Orb", 5)
        withClue("the Orb's enters trigger pauses on its dig-two selection") {
            driver.state.pendingDecision shouldNotBe null
        }
    }

    test("a creature entering causes no triggers while the Thrull is out") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "Doorkeeper Thrull")
        driver.castAndSettle(you, "Nightdrinker Moroii", 4)

        withClue("the Vampire's \"you lose 3 life\" never triggered") {
            driver.getLifeTotal(you) shouldBe 20
        }
        withClue("the creature itself still entered — only the trigger was suppressed") {
            driver.findPermanent(you, "Nightdrinker Moroii") shouldNotBe null
        }
    }

    test("a noncreature artifact entering causes no triggers either") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "Doorkeeper Thrull")
        driver.castAndSettle(you, "Polygraph Orb", 5)

        withClue("the Orb's dig-and-lose-2-life trigger never went on the stack") {
            driver.getLifeTotal(you) shouldBe 20
            driver.state.pendingDecision shouldBe null
        }
        withClue("the artifact itself still entered") {
            driver.findPermanent(you, "Polygraph Orb") shouldNotBe null
        }
    }
})
