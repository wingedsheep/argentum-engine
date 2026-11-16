package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.FoulRenewal
import com.wingedsheep.mtg.sets.definitions.ody.cards.CoffinPurge
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Foul Renewal (DTK #101) — {3}{B} Instant.
 * "Return target creature card from your graveyard to your hand. Target creature gets -X/-X until
 * end of turn, where X is the toughness of the card returned this way."
 *
 * X is the toughness of the card that actually came back, so these prove it reads the returned
 * card, that an illegal graveyard target leaves the creature unaffected, and that an illegal
 * creature target still lets the card come back (ruling 2015-02-25).
 */
class FoulRenewalScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FoulRenewal, CoffinPurge))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castFoulRenewal(controller: EntityId, card: EntityId, creature: EntityId) {
        val spell = putCardInHand(controller, "Foul Renewal")
        giveMana(controller, Color.BLACK, 4)
        castSpellWithTargets(
            controller,
            spell,
            listOf(ChosenTarget.Card(card, controller, Zone.GRAVEYARD), ChosenTarget.Permanent(creature))
        ).error shouldBe null
    }

    fun GameTestDriver.resolveStack() {
        while (stackSize > 0) bothPass()
    }

    test("returns the creature card and gives the target -X/-X equal to its toughness") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        val courser = driver.putCardInGraveyard(controller, "Centaur Courser") // 3/3
        val force = driver.putCreatureOnBattlefield(opponent, "Force of Nature") // 5/5

        driver.castFoulRenewal(controller, courser, force)
        driver.resolveStack()

        driver.getHand(controller) shouldContain courser
        withClue("Force of Nature gets -3/-3 from the returned 3/3") {
            driver.state.projectedState.getPower(force) shouldBe 2
            driver.state.projectedState.getToughness(force) shouldBe 2
        }
    }

    test("a toughness at least the target's kills it") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        val courser = driver.putCardInGraveyard(controller, "Centaur Courser") // 3/3
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions") // 2/1

        driver.castFoulRenewal(controller, courser, lions)
        driver.resolveStack()

        driver.getHand(controller) shouldContain courser
        driver.getGraveyard(opponent) shouldContain lions
    }

    test("if the graveyard target is gone, nothing returns and the creature is unaffected") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        val courser = driver.putCardInGraveyard(controller, "Centaur Courser")
        val force = driver.putCreatureOnBattlefield(opponent, "Force of Nature")

        driver.castFoulRenewal(controller, courser, force)

        // In response, exile the creature card Foul Renewal would return.
        val purge = driver.putCardInHand(controller, "Coffin Purge")
        driver.giveMana(controller, Color.BLACK, 1)
        driver.castSpellWithTargets(
            controller,
            purge,
            listOf(ChosenTarget.Card(courser, controller, Zone.GRAVEYARD))
        ).error shouldBe null
        driver.resolveStack()

        driver.getExile(controller) shouldContain courser
        driver.getHand(controller) shouldNotContain courser
        withClue("no card was returned this way, so there is no -X/-X") {
            driver.state.projectedState.getPower(force) shouldBe 5
            driver.state.projectedState.getToughness(force) shouldBe 5
        }
    }

    test("if the creature target is gone, the creature card still returns") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        val courser = driver.putCardInGraveyard(controller, "Centaur Courser")
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        driver.castFoulRenewal(controller, courser, lions)

        // In response, kill the creature target.
        val bolt = driver.putCardInHand(controller, "Lightning Bolt")
        driver.giveMana(controller, Color.RED, 1)
        driver.castSpell(controller, bolt, listOf(lions)).error shouldBe null
        driver.resolveStack()

        driver.getGraveyard(opponent) shouldContain lions
        driver.getHand(controller) shouldContain courser
    }
})
