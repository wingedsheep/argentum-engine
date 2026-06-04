package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.UginEyeOfTheStorms
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Ugin, Eye of the Storms (TDM, {7}, Loyalty 7).
 *
 * Exercises the new "one or more colors" target predicate (CardPredicate.IsColored), the
 * colorless-spell cast trigger, and the loyalty abilities.
 */
class UginEyeOfTheStormsScenarioTest : FunSpec({

    // A vanilla {2} colorless artifact creature, used to trigger "whenever you cast a colorless spell".
    val ColorlessThopter = CardDefinition.artifactCreature(
        name = "Test Colorless Thopter",
        manaCost = ManaCost.parse("{2}"),
        subtypes = setOf(Subtype("Thopter")),
        power = 1,
        toughness = 1,
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UginEyeOfTheStorms, ColorlessThopter))
        return driver
    }

    fun GameTestDriver.advanceToPlayer1Main() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.PRECOMBAT_MAIN)
            safety++
        }
    }

    test("casting a colorless spell exiles a target colored permanent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.advanceToPlayer1Main()

        driver.putPermanentOnBattlefield(driver.player1, "Ugin, Eye of the Storms")
        repeat(2) { driver.putLandOnBattlefield(driver.player1, "Forest") }
        val thopter = driver.putCardInHand(driver.player1, "Test Colorless Thopter")

        // A green creature the opponent controls (one or more colors → valid target).
        val bears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        driver.castSpell(driver.player1, thopter)
        // The colorless-spell trigger wants a target — pick the colored creature.
        var safety = 0
        while (driver.pendingDecision == null && driver.state.stack.isNotEmpty() && safety < 20) {
            driver.bothPass(); safety++
        }
        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitMultiTargetSelection(driver.player1, mapOf(0 to listOf(bears)))

        // Resolve the trigger (and the Thopter).
        safety = 0
        while (driver.state.stack.isNotEmpty() && driver.pendingDecision == null && safety < 20) {
            driver.bothPass(); safety++
        }

        // The green creature was exiled.
        driver.getExile(driver.player2).contains(bears) shouldBe true
        driver.state.getBattlefield().contains(bears) shouldBe false
    }

    test("0 ability adds three colorless mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.advanceToPlayer1Main()

        val ugin = driver.putPermanentOnBattlefield(driver.player1, "Ugin, Eye of the Storms")

        val zeroAbility = driver.cardRegistry.requireCard("Ugin, Eye of the Storms")
            .script.activatedAbilities.first { it.description.contains("{C}{C}{C}") }

        driver.submit(ActivateAbility(driver.player1, ugin, zeroAbility.id))
        driver.bothPass()

        val pool = driver.state.getEntity(driver.player1)?.get<ManaPoolComponent>()
        (pool?.colorless ?: 0) shouldBe 3
    }
})
