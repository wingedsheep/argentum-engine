package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.legalactions.support.EnumerationFixtures
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tests for [enumerators.ManaAbilityEnumerator] over the simplest case:
 * basic-land tap-for-mana abilities. More exotic mana costs (sacrifice,
 * tap-permanents, composite) are covered in the ActivatedAbilityEnumerator
 * tests in phase 6.
 */
class ManaAbilityEnumeratorTest : FunSpec({

    test("an untapped Forest produces a tap-for-{G} mana ability") {
        val driver = EnumerationFixtures.allForestsMainPhase()
        val forestId = driver.game.state.getHand(driver.player1).first()
        driver.game.playLand(driver.player1, forestId)

        val manaAbilities = driver.enumerateFor(driver.player1)
            .activatedAbilityActionsFor(forestId)

        manaAbilities shouldHaveSize 1
        val tap = manaAbilities.single()
        tap.isManaAbility shouldBe true
        tap.affordable shouldBe true
        tap.manaCostString shouldBe null  // Tap cost has no mana component
    }

    test("a tapped Forest still appears as an ability but is marked unaffordable") {
        val driver = EnumerationFixtures.allForestsMainPhase()
        val forestId = driver.game.state.getHand(driver.player1).first()
        driver.game.playLand(driver.player1, forestId)

        // Directly tap the Forest by mutating its components.
        val container = driver.game.state.getEntity(forestId)!!
        val tapped = container.with(
            com.wingedsheep.engine.state.components.battlefield.TappedComponent
        )
        driver.game.replaceState(driver.game.state.withEntity(forestId, tapped))

        val manaAbilities = driver.enumerateFor(driver.player1)
            .activatedAbilityActionsFor(forestId)

        manaAbilities shouldHaveSize 1
        manaAbilities.single().affordable shouldBe false
    }

    test("opponent's lands do not produce mana abilities for me") {
        val driver = EnumerationFixtures.allForestsMainPhase()
        val forestId = driver.game.state.getHand(driver.player1).first()
        driver.game.playLand(driver.player1, forestId)

        // Player 2 should see zero mana abilities on Player 1's Forest.
        val opponentView = driver.enumerateFor(driver.player2)
            .activatedAbilityActionsFor(forestId)

        opponentView.shouldBeEmpty()
    }

    test("with no permanents on the battlefield there are no mana abilities") {
        val driver = EnumerationFixtures.allForestsMainPhase()

        val abilities = driver.enumerateFor(driver.player1).activatedAbilityActions()

        abilities.shouldBeEmpty()
    }

    test("two Forests in play produce two distinct mana ability actions") {
        // Place two Forests on the battlefield via state surgery. Going through
        // two full turn cycles to play two lands legally would distract from
        // what's being tested — that the enumerator scans every controlled
        // permanent, not just one.
        val driver = EnumerationFixtures.allForestsMainPhase()

        val handIds = driver.game.state.getHand(driver.player1)
        val forest1 = handIds[0]
        val forest2 = handIds[1]

        var state = driver.game.state
        for (forestId in listOf(forest1, forest2)) {
            state = state.moveToZone(
                forestId,
                from = com.wingedsheep.engine.state.ZoneKey(driver.player1, com.wingedsheep.sdk.core.Zone.HAND),
                to = com.wingedsheep.engine.state.ZoneKey(driver.player1, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)
            )
        }
        driver.game.replaceState(state)

        val mineActions = driver.enumerateFor(driver.player1).activatedAbilityActions()

        mineActions shouldHaveSize 2
        mineActions.map { (it.action as com.wingedsheep.engine.core.ActivateAbility).sourceId }
            .toSet() shouldBe setOf(forest1, forest2)
    }
})
