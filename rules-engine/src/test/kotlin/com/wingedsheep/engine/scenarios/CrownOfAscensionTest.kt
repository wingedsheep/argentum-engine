package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.CrownOfAscension
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Crown of Ascension.
 *
 * Crown of Ascension: {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has flying.
 * Sacrifice Crown of Ascension: Enchanted creature and other creatures that share
 * a creature type with it gain flying until end of turn.
 */
class CrownOfAscensionTest : FunSpec({

    val crownAbilityId = CrownOfAscension.activatedAbilities.first().id

    // Wizard Bird - 1/1
    val WizardBird = CardDefinition.creature(
        name = "Wizard Bird",
        manaCost = ManaCost.parse("{1}{U}"),
        subtypes = setOf(Subtype("Wizard"), Subtype("Bird")),
        power = 1,
        toughness = 1
    )

    // Another Wizard - 2/2
    val WizardAdept = CardDefinition.creature(
        name = "Wizard Adept",
        manaCost = ManaCost.parse("{2}{U}"),
        subtypes = setOf(Subtype("Wizard")),
        power = 2,
        toughness = 2
    )

    // Bird Soldier - shares Bird type but not Wizard
    val BirdSoldier = CardDefinition.creature(
        name = "Bird Soldier",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Bird"), Subtype("Soldier")),
        power = 1,
        toughness = 2
    )

    // Elf Druid - shares no type with Wizard Bird
    val ElfDruid = CardDefinition.creature(
        name = "Elf Druid",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Elf"), Subtype("Druid")),
        power = 1,
        toughness = 1
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                CrownOfAscension, WizardBird, WizardAdept, BirdSoldier, ElfDruid
            )
        )
        return driver
    }

    test("Static ability grants flying to enchanted creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on battlefield
        val wizard = driver.putCreatureOnBattlefield(activePlayer, "Wizard Bird")

        // Cast Crown of Ascension on the wizard
        val crown = driver.putCardInHand(activePlayer, "Crown of Ascension")
        driver.giveMana(activePlayer, Color.BLUE, 2)
        driver.castSpell(activePlayer, crown, listOf(wizard))
        driver.bothPass()

        // Wizard should have flying
        val projected = projector.project(driver.state)
        projected.hasKeyword(wizard, Keyword.FLYING) shouldBe true
    }

    test("Sacrifice ability grants flying to creatures sharing a type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield
        val wizardBird = driver.putCreatureOnBattlefield(activePlayer, "Wizard Bird")
        val wizardAdept = driver.putCreatureOnBattlefield(activePlayer, "Wizard Adept")
        val birdSoldier = driver.putCreatureOnBattlefield(activePlayer, "Bird Soldier")
        val elfDruid = driver.putCreatureOnBattlefield(activePlayer, "Elf Druid")

        // Cast Crown of Ascension on the Wizard Bird
        val crown = driver.putCardInHand(activePlayer, "Crown of Ascension")
        driver.giveMana(activePlayer, Color.BLUE, 2)
        driver.castSpell(activePlayer, crown, listOf(wizardBird))
        driver.bothPass()

        // Before sacrifice: only Wizard Bird has flying (from static ability)
        val projectedBefore = projector.project(driver.state)
        projectedBefore.hasKeyword(wizardBird, Keyword.FLYING) shouldBe true
        projectedBefore.hasKeyword(wizardAdept, Keyword.FLYING) shouldBe false
        projectedBefore.hasKeyword(birdSoldier, Keyword.FLYING) shouldBe false
        projectedBefore.hasKeyword(elfDruid, Keyword.FLYING) shouldBe false

        // Sacrifice the Crown
        val activateResult = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = crown,
                abilityId = crownAbilityId
            )
        )
        activateResult.isSuccess shouldBe true

        // Let the ability resolve
        driver.bothPass()

        // Wizard Bird: had static flying (aura gone), gained flying from sacrifice
        val projectedAfter = projector.project(driver.state)
        projectedAfter.hasKeyword(wizardBird, Keyword.FLYING) shouldBe true

        // Wizard Adept: shares Wizard type = gains flying
        projectedAfter.hasKeyword(wizardAdept, Keyword.FLYING) shouldBe true

        // Bird Soldier: shares Bird type = gains flying
        projectedAfter.hasKeyword(birdSoldier, Keyword.FLYING) shouldBe true

        // Elf Druid: no type shared = no flying
        projectedAfter.hasKeyword(elfDruid, Keyword.FLYING) shouldBe false
    }

    test("Sacrifice ability effect wears off at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield
        val wizardBird = driver.putCreatureOnBattlefield(activePlayer, "Wizard Bird")
        val wizardAdept = driver.putCreatureOnBattlefield(activePlayer, "Wizard Adept")

        // Cast Crown on Wizard Bird
        val crown = driver.putCardInHand(activePlayer, "Crown of Ascension")
        driver.giveMana(activePlayer, Color.BLUE, 2)
        driver.castSpell(activePlayer, crown, listOf(wizardBird))
        driver.bothPass()

        // Sacrifice the Crown
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = crown,
                abilityId = crownAbilityId
            )
        )
        driver.bothPass()

        // After sacrifice: both should have flying
        val projectedAfter = projector.project(driver.state)
        projectedAfter.hasKeyword(wizardBird, Keyword.FLYING) shouldBe true
        projectedAfter.hasKeyword(wizardAdept, Keyword.FLYING) shouldBe true

        // Pass to next turn (end of turn cleanup removes the effects)
        driver.passPriorityUntil(Step.UPKEEP)

        // Effects should have worn off
        val projectedNextTurn = projector.project(driver.state)
        projectedNextTurn.hasKeyword(wizardBird, Keyword.FLYING) shouldBe false
        projectedNextTurn.hasKeyword(wizardAdept, Keyword.FLYING) shouldBe false
    }
})
