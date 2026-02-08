package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantToEnchantedCreatureTypeGroupEffect
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.targeting.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Tests for Crown of Fury.
 *
 * Crown of Fury: {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+0 and has first strike.
 * Sacrifice Crown of Fury: Enchanted creature and other creatures that share
 * a creature type with it get +1/+0 and gain first strike until end of turn.
 */
class CrownOfFuryTest : FunSpec({

    val crownAbilityId = AbilityId(UUID.randomUUID().toString())

    val CrownOfFury = CardDefinition.aura(
        name = "Crown of Fury",
        manaCost = ManaCost.parse("{1}{R}"),
        oracleText = "Enchant creature\nEnchanted creature gets +1/+0 and has first strike.\nSacrifice Crown of Fury: Enchanted creature and other creatures that share a creature type with it get +1/+0 and gain first strike until end of turn.",
        script = CardScript(
            auraTarget = TargetCreature(),
            staticAbilities = listOf(
                ModifyStats(1, 0),
                GrantKeyword(Keyword.FIRST_STRIKE)
            ),
            activatedAbilities = listOf(
                ActivatedAbility(
                    id = crownAbilityId,
                    cost = AbilityCost.SacrificeSelf,
                    effect = GrantToEnchantedCreatureTypeGroupEffect(
                        powerModifier = 1,
                        toughnessModifier = 0,
                        keyword = Keyword.FIRST_STRIKE
                    )
                )
            )
        )
    )

    // Goblin Warrior - 2/2
    val GoblinWarrior = CardDefinition.creature(
        name = "Goblin Warrior",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Warrior")),
        power = 2,
        toughness = 2
    )

    // Another Goblin - 1/1
    val GoblinPiker = CardDefinition.creature(
        name = "Goblin Piker",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 2,
        toughness = 1
    )

    // Elf Warrior - shares Warrior type but not Goblin
    val ElfWarrior = CardDefinition.creature(
        name = "Elf Warrior",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Elf"), Subtype("Warrior")),
        power = 2,
        toughness = 3
    )

    // Elf Druid - shares no type with Goblin Warrior
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
                CrownOfFury, GoblinWarrior, GoblinPiker, ElfWarrior, ElfDruid
            )
        )
        return driver
    }

    test("Static ability grants +1/+0 and first strike to enchanted creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on battlefield
        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")

        // Cast Crown of Fury on the goblin
        val crown = driver.putCardInHand(activePlayer, "Crown of Fury")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, crown, listOf(goblin))
        driver.bothPass()

        // Goblin should have +1/+0 (2/2 -> 3/2) and first strike
        projector.getProjectedPower(driver.state, goblin) shouldBe 3
        projector.getProjectedToughness(driver.state, goblin) shouldBe 2

        val projected = projector.project(driver.state)
        projected.hasKeyword(goblin, Keyword.FIRST_STRIKE) shouldBe true
    }

    test("Sacrifice ability grants +1/+0 and first strike to creatures sharing a type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield
        val goblinWarrior = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val goblinPiker = driver.putCreatureOnBattlefield(activePlayer, "Goblin Piker")
        val elfWarrior = driver.putCreatureOnBattlefield(activePlayer, "Elf Warrior")
        val elfDruid = driver.putCreatureOnBattlefield(activePlayer, "Elf Druid")

        // Cast Crown of Fury on the Goblin Warrior
        val crown = driver.putCardInHand(activePlayer, "Crown of Fury")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, crown, listOf(goblinWarrior))
        driver.bothPass()

        // Before sacrifice: Goblin Warrior has +1/+0 from static ability
        projector.getProjectedPower(driver.state, goblinWarrior) shouldBe 3
        projector.getProjectedPower(driver.state, goblinPiker) shouldBe 2
        projector.getProjectedPower(driver.state, elfWarrior) shouldBe 2
        projector.getProjectedPower(driver.state, elfDruid) shouldBe 1

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

        // Goblin Warrior: lost static +1/+0 (aura gone), gained +1/+0 from sacrifice = 3/2
        // (shares Goblin and Warrior types with itself)
        projector.getProjectedPower(driver.state, goblinWarrior) shouldBe 3
        projector.getProjectedToughness(driver.state, goblinWarrior) shouldBe 2

        // Goblin Piker: gained +1/+0 (shares Goblin type) = 3/1
        projector.getProjectedPower(driver.state, goblinPiker) shouldBe 3
        projector.getProjectedToughness(driver.state, goblinPiker) shouldBe 1

        // Elf Warrior: gained +1/+0 (shares Warrior type) = 3/3
        projector.getProjectedPower(driver.state, elfWarrior) shouldBe 3
        projector.getProjectedToughness(driver.state, elfWarrior) shouldBe 3

        // Elf Druid: no type shared, should NOT be affected = 1/1
        projector.getProjectedPower(driver.state, elfDruid) shouldBe 1
        projector.getProjectedToughness(driver.state, elfDruid) shouldBe 1

        // Check first strike
        val projected = projector.project(driver.state)
        projected.hasKeyword(goblinWarrior, Keyword.FIRST_STRIKE) shouldBe true
        projected.hasKeyword(goblinPiker, Keyword.FIRST_STRIKE) shouldBe true
        projected.hasKeyword(elfWarrior, Keyword.FIRST_STRIKE) shouldBe true
        projected.hasKeyword(elfDruid, Keyword.FIRST_STRIKE) shouldBe false
    }

    test("Sacrifice ability effect wears off at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield
        val goblinWarrior = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val goblinPiker = driver.putCreatureOnBattlefield(activePlayer, "Goblin Piker")

        // Cast Crown on Goblin Warrior
        val crown = driver.putCardInHand(activePlayer, "Crown of Fury")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, crown, listOf(goblinWarrior))
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

        // After sacrifice: both goblins should have the buff
        projector.getProjectedPower(driver.state, goblinWarrior) shouldBe 3
        projector.getProjectedPower(driver.state, goblinPiker) shouldBe 3

        // Pass to next turn (end of turn cleanup removes the effects)
        driver.passPriorityUntil(Step.UPKEEP)

        // Effects should have worn off
        projector.getProjectedPower(driver.state, goblinWarrior) shouldBe 2
        projector.getProjectedToughness(driver.state, goblinWarrior) shouldBe 2
        projector.getProjectedPower(driver.state, goblinPiker) shouldBe 2
        projector.getProjectedToughness(driver.state, goblinPiker) shouldBe 1

        val projected = projector.project(driver.state)
        projected.hasKeyword(goblinWarrior, Keyword.FIRST_STRIKE) shouldBe false
        projected.hasKeyword(goblinPiker, Keyword.FIRST_STRIKE) shouldBe false
    }
})
