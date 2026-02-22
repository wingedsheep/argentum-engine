package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.CrownOfFury
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

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

    val crownAbilityId = CrownOfFury.activatedAbilities.first().id

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
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

        // Put a creature on battlefield (Goblin Brigand is a 2/2 Goblin Warrior)
        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Brigand")

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
        // Goblin Brigand: 2/2 Goblin Warrior
        val goblinWarrior = driver.putCreatureOnBattlefield(activePlayer, "Goblin Brigand")
        // Goblin Bully: 2/1 Goblin (shares Goblin type)
        val goblinPiker = driver.putCreatureOnBattlefield(activePlayer, "Goblin Bully")
        // Elvish Warrior: 2/3 Elf Warrior (shares Warrior type)
        val elfWarrior = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        // Llanowar Elves: 1/1 Elf Druid (shares no type with Goblin Warrior)
        val elfDruid = driver.putCreatureOnBattlefield(activePlayer, "Llanowar Elves")

        // Cast Crown of Fury on the Goblin Brigand
        val crown = driver.putCardInHand(activePlayer, "Crown of Fury")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, crown, listOf(goblinWarrior))
        driver.bothPass()

        // Before sacrifice: Goblin Brigand has +1/+0 from static ability
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

        // Goblin Brigand: lost static +1/+0 (aura gone), gained +1/+0 from sacrifice = 3/2
        // (shares Goblin and Warrior types with itself)
        projector.getProjectedPower(driver.state, goblinWarrior) shouldBe 3
        projector.getProjectedToughness(driver.state, goblinWarrior) shouldBe 2

        // Goblin Bully: gained +1/+0 (shares Goblin type) = 3/1
        projector.getProjectedPower(driver.state, goblinPiker) shouldBe 3
        projector.getProjectedToughness(driver.state, goblinPiker) shouldBe 1

        // Elvish Warrior: gained +1/+0 (shares Warrior type) = 3/3
        projector.getProjectedPower(driver.state, elfWarrior) shouldBe 3
        projector.getProjectedToughness(driver.state, elfWarrior) shouldBe 3

        // Llanowar Elves: no type shared, should NOT be affected = 1/1
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
        val goblinWarrior = driver.putCreatureOnBattlefield(activePlayer, "Goblin Brigand")
        val goblinPiker = driver.putCreatureOnBattlefield(activePlayer, "Goblin Bully")

        // Cast Crown on Goblin Brigand
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
