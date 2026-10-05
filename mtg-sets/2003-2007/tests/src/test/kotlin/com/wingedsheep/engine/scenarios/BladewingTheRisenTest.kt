package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scg.cards.BladewingTheRisen
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Bladewing the Risen:
 * {3}{B}{B}{R}{R} Legendary Creature — Zombie Dragon 4/4
 * Flying
 * When Bladewing the Risen enters the battlefield, you may return target Dragon
 * permanent card from your graveyard to the battlefield.
 * {B}{R}: Dragon creatures get +1/+1 until end of turn.
 */
class BladewingTheRisenTest : FunSpec({

    val TestDragon = CardDefinition.creature(
        name = "Test Dragon",
        manaCost = ManaCost.parse("{4}{R}{R}"),
        subtypes = setOf(Subtype("Dragon")),
        power = 5,
        toughness = 5,
        oracleText = "",
        keywords = setOf(Keyword.FLYING)
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestDragon))
        driver.registerCard(BladewingTheRisen)
        return driver
    }

    test("ETB returns a Dragon from graveyard to battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Dragon in the graveyard
        val dragonInGraveyard = driver.putCardInGraveyard(activePlayer, "Test Dragon")

        // Cast Bladewing
        val bladewing = driver.putCardInHand(activePlayer, "Bladewing the Risen")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.giveColorlessMana(activePlayer, 3)
        driver.castSpell(activePlayer, bladewing)
        driver.bothPass() // resolve Bladewing

        // The ETB trigger announces a target before consent.
        driver.submitTargetSelection(activePlayer, listOf(dragonInGraveyard))

        // Trigger on stack — resolve it
        driver.bothPass()
        driver.submitYesNo(activePlayer, true).error shouldBe null

        // Dragon should now be on the battlefield
        driver.findPermanent(activePlayer, "Test Dragon") shouldNotBe null
    }

    test("ETB can be declined") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val dragon = driver.putCardInGraveyard(activePlayer, "Test Dragon")

        val bladewing = driver.putCardInHand(activePlayer, "Bladewing the Risen")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.giveColorlessMana(activePlayer, 3)
        driver.castSpell(activePlayer, bladewing)
        driver.bothPass()

        // Decline the ETB trigger at its may-question
        driver.submitTargetSelection(activePlayer, listOf(dragon)).error shouldBe null
        driver.bothPass().error shouldBe null
        driver.submitYesNo(activePlayer, false).error shouldBe null

        // Dragon should remain in graveyard
        driver.findPermanent(activePlayer, "Test Dragon") shouldBe null
    }

    test("activated ability gives all Dragons +1/+1") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bladewing = driver.putCreatureOnBattlefield(activePlayer, "Bladewing the Risen")
        val dragon = driver.putCreatureOnBattlefield(activePlayer, "Test Dragon")

        // Activate {B}{R}: Dragon creatures get +1/+1
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveMana(activePlayer, Color.RED, 1)

        val abilityId = BladewingTheRisen.activatedAbilities[0].id
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = bladewing,
                abilityId = abilityId
            )
        )
        driver.bothPass()

        // Both Dragons should be pumped
        val projected = projector.project(driver.state)
        projected.getPower(bladewing) shouldBe 5 // 4+1
        projected.getToughness(bladewing) shouldBe 5 // 4+1
        projected.getPower(dragon) shouldBe 6 // 5+1
        projected.getToughness(dragon) shouldBe 6 // 5+1
    }
})
