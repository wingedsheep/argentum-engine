package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.GhituJourneymage
import com.wingedsheep.mtg.sets.definitions.ons.cards.Imagecrafter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Ghitu Journeymage.
 *
 * Ghitu Journeymage: {2}{R}, 3/2 Human Wizard
 * When Ghitu Journeymage enters the battlefield, if you control another Wizard, it deals 2 damage
 * to each opponent.
 *
 * The intervening-if must count Wizards OTHER than Ghitu Journeymage (excludeSelf), not rely on
 * "total Wizards >= 2" — otherwise it breaks whenever Ghitu itself isn't a Wizard when the condition
 * is checked (CR 603.4), e.g. a type-changing effect strips its subtypes.
 */
class GhituJourneymageTest : FunSpec({

    val imagecrafterAbilityId = Imagecrafter.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GhituJourneymage, Imagecrafter))
        return driver
    }

    test("deals 2 to each opponent when you control another Wizard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        // Imagecrafter is a Human Wizard — "another Wizard".
        driver.putCreatureOnBattlefield(p1, "Imagecrafter")

        val ghitu = driver.putCardInHand(p1, "Ghitu Journeymage")
        driver.giveMana(p1, Color.RED, 3)
        driver.castSpell(p1, ghitu).isSuccess shouldBe true
        driver.bothPass() // Ghitu resolves and enters; ETB trigger goes on the stack
        driver.bothPass() // ETB trigger resolves

        driver.assertLifeTotal(p2, 18)
    }

    test("no damage when you control no other Wizard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        // Only Ghitu itself is a Wizard — the "another Wizard" condition is not met.
        val ghitu = driver.putCardInHand(p1, "Ghitu Journeymage")
        driver.giveMana(p1, Color.RED, 3)
        driver.castSpell(p1, ghitu).isSuccess shouldBe true
        driver.bothPass()
        driver.bothPass()

        driver.assertLifeTotal(p2, 20)
    }

    test("still triggers when Ghitu itself is turned into a non-Wizard but another Wizard remains") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        // Imagecrafter stays a Wizard and doubles as the "another Wizard".
        val imagecrafter = driver.putCreatureOnBattlefield(p1, "Imagecrafter")
        driver.removeSummoningSickness(imagecrafter)

        val ghitu = driver.putCardInHand(p1, "Ghitu Journeymage")
        driver.giveMana(p1, Color.RED, 3)
        driver.castSpell(p1, ghitu).isSuccess shouldBe true
        driver.bothPass() // Ghitu enters; ETB trigger on the stack, p1 has priority

        // In response, use Imagecrafter to turn Ghitu into a Goblin (no longer a Wizard).
        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = imagecrafter,
                abilityId = imagecrafterAbilityId,
                targets = listOf(ChosenTarget.Permanent(ghitu))
            )
        ).isSuccess shouldBe true
        driver.bothPass() // resolve Imagecrafter's ability -> pause for the creature-type choice
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(p1, OptionChosenResponse(decision.id, decision.options.indexOf("Goblin")))

        // Ghitu is now a Goblin. The old "total Wizards >= 2" check would see only Imagecrafter and
        // wrongly fizzle; counting OTHER Wizards (Imagecrafter = 1) still fires.
        driver.bothPass() // ETB trigger resolves

        driver.assertLifeTotal(p2, 18)
    }
})
