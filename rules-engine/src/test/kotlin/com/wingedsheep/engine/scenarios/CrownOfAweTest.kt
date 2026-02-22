package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.CrownOfAwe
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.effects.GrantToEnchantedCreatureTypeGroupEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Tests for Crown of Awe.
 *
 * Crown of Awe: {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has protection from black and from red.
 * Sacrifice Crown of Awe: Enchanted creature and other creatures that share
 * a creature type with it gain protection from black and from red until end of turn.
 */
class CrownOfAweTest : FunSpec({

    val crownAbilityId = AbilityId(UUID.randomUUID().toString())

    // Human Cleric - 2/2
    val HumanCleric = CardDefinition.creature(
        name = "Human Cleric",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Cleric")),
        power = 2,
        toughness = 2
    )

    // Another Cleric - 1/1
    val GoblinCleric = CardDefinition.creature(
        name = "Goblin Cleric",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Cleric")),
        power = 1,
        toughness = 1
    )

    // Human Soldier - shares Human type but not Cleric
    val HumanSoldier = CardDefinition.creature(
        name = "Human Soldier",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Soldier")),
        power = 2,
        toughness = 2
    )

    // Elf Druid - shares no type with Human Cleric
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
                CrownOfAwe, HumanCleric, GoblinCleric, HumanSoldier, ElfDruid
            )
        )
        return driver
    }

    test("Static ability grants protection from black and red to enchanted creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on battlefield
        val cleric = driver.putCreatureOnBattlefield(activePlayer, "Human Cleric")

        // Cast Crown of Awe on the cleric
        val crown = driver.putCardInHand(activePlayer, "Crown of Awe")
        driver.giveMana(activePlayer, Color.WHITE, 2)
        driver.castSpell(activePlayer, crown, listOf(cleric))
        driver.bothPass()

        // Cleric should have protection from black and red
        val projected = projector.project(driver.state)
        projected.hasKeyword(cleric, "PROTECTION_FROM_BLACK") shouldBe true
        projected.hasKeyword(cleric, "PROTECTION_FROM_RED") shouldBe true
        // But not from other colors
        projected.hasKeyword(cleric, "PROTECTION_FROM_WHITE") shouldBe false
        projected.hasKeyword(cleric, "PROTECTION_FROM_BLUE") shouldBe false
        projected.hasKeyword(cleric, "PROTECTION_FROM_GREEN") shouldBe false
    }

    test("Sacrifice ability grants protection to creatures sharing a type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield
        val humanCleric = driver.putCreatureOnBattlefield(activePlayer, "Human Cleric")
        val goblinCleric = driver.putCreatureOnBattlefield(activePlayer, "Goblin Cleric")
        val humanSoldier = driver.putCreatureOnBattlefield(activePlayer, "Human Soldier")
        val elfDruid = driver.putCreatureOnBattlefield(activePlayer, "Elf Druid")

        // Cast Crown of Awe on the Human Cleric
        val crown = driver.putCardInHand(activePlayer, "Crown of Awe")
        driver.giveMana(activePlayer, Color.WHITE, 2)
        driver.castSpell(activePlayer, crown, listOf(humanCleric))
        driver.bothPass()

        // Before sacrifice: only enchanted creature has protection (from static ability)
        val projectedBefore = projector.project(driver.state)
        projectedBefore.hasKeyword(humanCleric, "PROTECTION_FROM_BLACK") shouldBe true
        projectedBefore.hasKeyword(humanCleric, "PROTECTION_FROM_RED") shouldBe true
        projectedBefore.hasKeyword(goblinCleric, "PROTECTION_FROM_BLACK") shouldBe false
        projectedBefore.hasKeyword(humanSoldier, "PROTECTION_FROM_BLACK") shouldBe false
        projectedBefore.hasKeyword(elfDruid, "PROTECTION_FROM_BLACK") shouldBe false

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

        // Human Cleric: shares Human and Cleric with itself - should have protection
        val projected = projector.project(driver.state)
        projected.hasKeyword(humanCleric, "PROTECTION_FROM_BLACK") shouldBe true
        projected.hasKeyword(humanCleric, "PROTECTION_FROM_RED") shouldBe true

        // Goblin Cleric: shares Cleric type - should have protection
        projected.hasKeyword(goblinCleric, "PROTECTION_FROM_BLACK") shouldBe true
        projected.hasKeyword(goblinCleric, "PROTECTION_FROM_RED") shouldBe true

        // Human Soldier: shares Human type - should have protection
        projected.hasKeyword(humanSoldier, "PROTECTION_FROM_BLACK") shouldBe true
        projected.hasKeyword(humanSoldier, "PROTECTION_FROM_RED") shouldBe true

        // Elf Druid: no type shared, should NOT be affected
        projected.hasKeyword(elfDruid, "PROTECTION_FROM_BLACK") shouldBe false
        projected.hasKeyword(elfDruid, "PROTECTION_FROM_RED") shouldBe false
    }

    test("Sacrifice ability effect wears off at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield
        val humanCleric = driver.putCreatureOnBattlefield(activePlayer, "Human Cleric")
        val goblinCleric = driver.putCreatureOnBattlefield(activePlayer, "Goblin Cleric")

        // Cast Crown on Human Cleric
        val crown = driver.putCardInHand(activePlayer, "Crown of Awe")
        driver.giveMana(activePlayer, Color.WHITE, 2)
        driver.castSpell(activePlayer, crown, listOf(humanCleric))
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

        // After sacrifice: both creatures should have protection
        val projected = projector.project(driver.state)
        projected.hasKeyword(humanCleric, "PROTECTION_FROM_BLACK") shouldBe true
        projected.hasKeyword(humanCleric, "PROTECTION_FROM_RED") shouldBe true
        projected.hasKeyword(goblinCleric, "PROTECTION_FROM_BLACK") shouldBe true
        projected.hasKeyword(goblinCleric, "PROTECTION_FROM_RED") shouldBe true

        // Pass to next turn (end of turn cleanup removes the effects)
        driver.passPriorityUntil(Step.UPKEEP)

        // Effects should have worn off
        val projectedAfter = projector.project(driver.state)
        projectedAfter.hasKeyword(humanCleric, "PROTECTION_FROM_BLACK") shouldBe false
        projectedAfter.hasKeyword(humanCleric, "PROTECTION_FROM_RED") shouldBe false
        projectedAfter.hasKeyword(goblinCleric, "PROTECTION_FROM_BLACK") shouldBe false
        projectedAfter.hasKeyword(goblinCleric, "PROTECTION_FROM_RED") shouldBe false
    }
})
