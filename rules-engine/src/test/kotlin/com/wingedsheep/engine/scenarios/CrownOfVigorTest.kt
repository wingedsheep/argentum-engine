package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
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
import com.wingedsheep.sdk.scripting.GrantToEnchantedCreatureTypeGroupEffect
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.targeting.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Tests for Crown of Vigor.
 *
 * Crown of Vigor: {1}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1.
 * Sacrifice Crown of Vigor: Enchanted creature and other creatures that share
 * a creature type with it get +1/+1 until end of turn.
 */
class CrownOfVigorTest : FunSpec({

    val crownAbilityId = AbilityId(UUID.randomUUID().toString())

    val CrownOfVigor = CardDefinition.aura(
        name = "Crown of Vigor",
        manaCost = ManaCost.parse("{1}{G}"),
        oracleText = "Enchant creature\nEnchanted creature gets +1/+1.\nSacrifice Crown of Vigor: Enchanted creature and other creatures that share a creature type with it get +1/+1 until end of turn.",
        script = CardScript(
            auraTarget = TargetCreature(),
            staticAbilities = listOf(
                ModifyStats(1, 1)
            ),
            activatedAbilities = listOf(
                ActivatedAbility(
                    id = crownAbilityId,
                    cost = AbilityCost.SacrificeSelf,
                    effect = GrantToEnchantedCreatureTypeGroupEffect(
                        powerModifier = 1,
                        toughnessModifier = 1
                    )
                )
            )
        )
    )

    // Elf Warrior - 2/3
    val ElfWarrior = CardDefinition.creature(
        name = "Elf Warrior",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Elf"), Subtype("Warrior")),
        power = 2,
        toughness = 3
    )

    // Another Elf - 1/1
    val ElfDruid = CardDefinition.creature(
        name = "Elf Druid",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Elf"), Subtype("Druid")),
        power = 1,
        toughness = 1
    )

    // Human Warrior - shares Warrior type but not Elf
    val HumanWarrior = CardDefinition.creature(
        name = "Human Warrior",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Warrior")),
        power = 2,
        toughness = 2
    )

    // Goblin Rogue - shares no type with Elf Warrior
    val GoblinRogue = CardDefinition.creature(
        name = "Goblin Rogue",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Rogue")),
        power = 1,
        toughness = 1
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                CrownOfVigor, ElfWarrior, ElfDruid, HumanWarrior, GoblinRogue
            )
        )
        return driver
    }

    test("Static ability grants +1/+1 to enchanted creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on battlefield
        val elfWarrior = driver.putCreatureOnBattlefield(activePlayer, "Elf Warrior")

        // Cast Crown of Vigor on the elf
        val crown = driver.putCardInHand(activePlayer, "Crown of Vigor")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.castSpell(activePlayer, crown, listOf(elfWarrior))
        driver.bothPass()

        // Elf Warrior should have +1/+1 (2/3 -> 3/4)
        projector.getProjectedPower(driver.state, elfWarrior) shouldBe 3
        projector.getProjectedToughness(driver.state, elfWarrior) shouldBe 4
    }

    test("Sacrifice ability grants +1/+1 to creatures sharing a type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield
        val elfWarrior = driver.putCreatureOnBattlefield(activePlayer, "Elf Warrior")
        val elfDruid = driver.putCreatureOnBattlefield(activePlayer, "Elf Druid")
        val humanWarrior = driver.putCreatureOnBattlefield(activePlayer, "Human Warrior")
        val goblinRogue = driver.putCreatureOnBattlefield(activePlayer, "Goblin Rogue")

        // Cast Crown of Vigor on the Elf Warrior
        val crown = driver.putCardInHand(activePlayer, "Crown of Vigor")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.castSpell(activePlayer, crown, listOf(elfWarrior))
        driver.bothPass()

        // Before sacrifice: Elf Warrior has +1/+1 from static ability
        projector.getProjectedPower(driver.state, elfWarrior) shouldBe 3
        projector.getProjectedToughness(driver.state, elfWarrior) shouldBe 4
        projector.getProjectedPower(driver.state, elfDruid) shouldBe 1
        projector.getProjectedPower(driver.state, humanWarrior) shouldBe 2
        projector.getProjectedPower(driver.state, goblinRogue) shouldBe 1

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

        // Elf Warrior: lost static +1/+1 (aura gone), gained +1/+1 from sacrifice = 3/4
        projector.getProjectedPower(driver.state, elfWarrior) shouldBe 3
        projector.getProjectedToughness(driver.state, elfWarrior) shouldBe 4

        // Elf Druid: gained +1/+1 (shares Elf type) = 2/2
        projector.getProjectedPower(driver.state, elfDruid) shouldBe 2
        projector.getProjectedToughness(driver.state, elfDruid) shouldBe 2

        // Human Warrior: gained +1/+1 (shares Warrior type) = 3/3
        projector.getProjectedPower(driver.state, humanWarrior) shouldBe 3
        projector.getProjectedToughness(driver.state, humanWarrior) shouldBe 3

        // Goblin Rogue: no type shared, should NOT be affected = 1/1
        projector.getProjectedPower(driver.state, goblinRogue) shouldBe 1
        projector.getProjectedToughness(driver.state, goblinRogue) shouldBe 1
    }

    test("Sacrifice ability effect wears off at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield
        val elfWarrior = driver.putCreatureOnBattlefield(activePlayer, "Elf Warrior")
        val elfDruid = driver.putCreatureOnBattlefield(activePlayer, "Elf Druid")

        // Cast Crown on Elf Warrior
        val crown = driver.putCardInHand(activePlayer, "Crown of Vigor")
        driver.giveMana(activePlayer, Color.GREEN, 2)
        driver.castSpell(activePlayer, crown, listOf(elfWarrior))
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

        // After sacrifice: both elves should have the buff
        projector.getProjectedPower(driver.state, elfWarrior) shouldBe 3
        projector.getProjectedToughness(driver.state, elfWarrior) shouldBe 4
        projector.getProjectedPower(driver.state, elfDruid) shouldBe 2
        projector.getProjectedToughness(driver.state, elfDruid) shouldBe 2

        // Pass to next turn (end of turn cleanup removes the effects)
        driver.passPriorityUntil(Step.UPKEEP)

        // Effects should have worn off
        projector.getProjectedPower(driver.state, elfWarrior) shouldBe 2
        projector.getProjectedToughness(driver.state, elfWarrior) shouldBe 3
        projector.getProjectedPower(driver.state, elfDruid) shouldBe 1
        projector.getProjectedToughness(driver.state, elfDruid) shouldBe 1
    }
})
