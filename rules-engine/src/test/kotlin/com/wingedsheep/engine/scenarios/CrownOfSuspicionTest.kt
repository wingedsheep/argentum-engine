package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.CrownOfSuspicion
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Crown of Suspicion.
 *
 * Crown of Suspicion: {1}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/-1.
 * Sacrifice Crown of Suspicion: Enchanted creature and other creatures that share
 * a creature type with it get +2/-1 until end of turn.
 */
class CrownOfSuspicionTest : FunSpec({

    val crownAbilityId = CrownOfSuspicion.activatedAbilities.first().id

    // No real Zombie Warrior or plain Zombie with the right stats exist in the sets
    val ZombieWarrior = CardDefinition.creature(
        name = "Zombie Warrior",
        manaCost = ManaCost.parse("{2}{B}"),
        subtypes = setOf(Subtype("Zombie"), Subtype("Warrior")),
        power = 3,
        toughness = 3
    )

    val ZombieKnight = CardDefinition.creature(
        name = "Zombie Knight",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype("Zombie")),
        power = 2,
        toughness = 2
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ZombieWarrior, ZombieKnight))
        return driver
    }

    test("Static ability grants +2/-1 to enchanted creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on battlefield
        val zombie = driver.putCreatureOnBattlefield(activePlayer, "Zombie Warrior")

        // Cast Crown of Suspicion on the zombie
        val crown = driver.putCardInHand(activePlayer, "Crown of Suspicion")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, crown, listOf(zombie))
        driver.bothPass()

        // Zombie should have +2/-1 (3/3 -> 5/2)
        projector.getProjectedPower(driver.state, zombie) shouldBe 5
        projector.getProjectedToughness(driver.state, zombie) shouldBe 2
    }

    test("Sacrifice ability grants +2/-1 to creatures sharing a type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield
        val zombieWarrior = driver.putCreatureOnBattlefield(activePlayer, "Zombie Warrior")
        val zombieKnight = driver.putCreatureOnBattlefield(activePlayer, "Zombie Knight")
        // Elvish Warrior: 2/3 Elf Warrior (shares Warrior type but not Zombie)
        val elfWarrior = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        // Llanowar Elves: 1/1 Elf Druid (shares no type with Zombie Warrior)
        val elfDruid = driver.putCreatureOnBattlefield(activePlayer, "Llanowar Elves")

        // Cast Crown of Suspicion on the Zombie Warrior
        val crown = driver.putCardInHand(activePlayer, "Crown of Suspicion")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, crown, listOf(zombieWarrior))
        driver.bothPass()

        // Before sacrifice: Zombie Warrior has +2/-1 from static ability (3/3 -> 5/2)
        projector.getProjectedPower(driver.state, zombieWarrior) shouldBe 5
        projector.getProjectedToughness(driver.state, zombieWarrior) shouldBe 2
        projector.getProjectedPower(driver.state, zombieKnight) shouldBe 2
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

        // Zombie Warrior: lost static +2/-1 (aura gone), gained +2/-1 from sacrifice = 5/2
        // (shares Zombie and Warrior types with itself)
        projector.getProjectedPower(driver.state, zombieWarrior) shouldBe 5
        projector.getProjectedToughness(driver.state, zombieWarrior) shouldBe 2

        // Zombie Knight: gained +2/-1 (shares Zombie type) = 4/1
        projector.getProjectedPower(driver.state, zombieKnight) shouldBe 4
        projector.getProjectedToughness(driver.state, zombieKnight) shouldBe 1

        // Elvish Warrior: gained +2/-1 (shares Warrior type) = 4/2
        projector.getProjectedPower(driver.state, elfWarrior) shouldBe 4
        projector.getProjectedToughness(driver.state, elfWarrior) shouldBe 2

        // Llanowar Elves: no type shared, should NOT be affected = 1/1
        projector.getProjectedPower(driver.state, elfDruid) shouldBe 1
        projector.getProjectedToughness(driver.state, elfDruid) shouldBe 1
    }

    test("Sacrifice ability effect wears off at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield
        val zombieWarrior = driver.putCreatureOnBattlefield(activePlayer, "Zombie Warrior")
        val zombieKnight = driver.putCreatureOnBattlefield(activePlayer, "Zombie Knight")

        // Cast Crown on Zombie Warrior
        val crown = driver.putCardInHand(activePlayer, "Crown of Suspicion")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, crown, listOf(zombieWarrior))
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

        // After sacrifice: both zombies should have the buff
        projector.getProjectedPower(driver.state, zombieWarrior) shouldBe 5
        projector.getProjectedPower(driver.state, zombieKnight) shouldBe 4

        // Pass to next turn (end of turn cleanup removes the effects)
        driver.passPriorityUntil(Step.UPKEEP)

        // Effects should have worn off
        projector.getProjectedPower(driver.state, zombieWarrior) shouldBe 3
        projector.getProjectedToughness(driver.state, zombieWarrior) shouldBe 3
        projector.getProjectedPower(driver.state, zombieKnight) shouldBe 2
        projector.getProjectedToughness(driver.state, zombieKnight) shouldBe 2
    }

    test("Player can sacrifice Crown of Suspicion enchanting opponent's creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on opponent's battlefield
        val opponentZombie = driver.putCreatureOnBattlefield(opponent, "Zombie Warrior")

        // Cast Crown of Suspicion on the opponent's creature
        val crown = driver.putCardInHand(activePlayer, "Crown of Suspicion")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, crown, listOf(opponentZombie))
        driver.bothPass()

        // Crown should still be controlled by the active player
        driver.getController(crown) shouldBe activePlayer

        // Opponent's zombie should have +2/-1 from static ability (3/3 -> 5/2)
        projector.getProjectedPower(driver.state, opponentZombie) shouldBe 5
        projector.getProjectedToughness(driver.state, opponentZombie) shouldBe 2

        // Active player should be able to sacrifice the Crown
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

        // Opponent's zombie should have +2/-1 from sacrifice effect (3/3 -> 5/2)
        projector.getProjectedPower(driver.state, opponentZombie) shouldBe 5
        projector.getProjectedToughness(driver.state, opponentZombie) shouldBe 2
    }
})
