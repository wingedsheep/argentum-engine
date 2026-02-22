package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.CircleOfSolace
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Circle of Solace (ONS #13).
 *
 * Circle of Solace: {3}{W}
 * Enchantment
 * As Circle of Solace enters the battlefield, choose a creature type.
 * {1}{W}: The next time a creature of the chosen type would deal damage to you this turn, prevent that damage.
 */
class CircleOfSolaceTest : FunSpec({

    val circleAbilityId = CircleOfSolace.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("prevents combat damage from creature of chosen type to controller") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        // Active player attacks with Goblin, opponent has Circle
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has Circle of Solace with chosen type "Goblin"
        val circle = driver.putPermanentOnBattlefield(opponent, "Circle of Solace")
        driver.replaceState(driver.state.updateEntity(circle) { c ->
            c.with(ChosenCreatureTypeComponent("Goblin"))
        })

        // Active player has a Goblin
        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Bully")
        driver.removeSummoningSickness(goblin)

        // Active player passes priority → opponent gets priority and activates Circle
        driver.passPriority(activePlayer)
        driver.giveMana(opponent, Color.WHITE, 2)
        val result = driver.submit(
            ActivateAbility(
                playerId = opponent,
                sourceId = circle,
                abilityId = circleAbilityId
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass() // resolve the ability

        // Advance to combat
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(goblin), opponent)
        driver.bothPass() // declare blockers
        driver.bothPass() // no blocks
        driver.bothPass() // first strike (no first strikers)
        driver.bothPass() // combat damage

        // Opponent (Circle controller) should still be at 20 life - damage was prevented
        val life = driver.state.getEntity(opponent)?.get<LifeTotalComponent>()?.life ?: 0
        life shouldBe 20
    }

    test("does not prevent damage from creature of different type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has Circle of Solace with chosen type "Goblin"
        val circle = driver.putPermanentOnBattlefield(opponent, "Circle of Solace")
        driver.replaceState(driver.state.updateEntity(circle) { c ->
            c.with(ChosenCreatureTypeComponent("Goblin"))
        })

        // Active player has an Elf (not a Goblin)
        val elf = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        driver.removeSummoningSickness(elf)

        // Active player passes → opponent activates Circle
        driver.passPriority(activePlayer)
        driver.giveMana(opponent, Color.WHITE, 2)
        driver.submit(
            ActivateAbility(
                playerId = opponent,
                sourceId = circle,
                abilityId = circleAbilityId
            )
        )
        driver.bothPass() // resolve

        // Advance to combat
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(elf), opponent)
        driver.bothPass() // declare blockers
        driver.bothPass() // no blocks
        driver.bothPass() // first strike
        driver.bothPass() // combat damage

        // Elf is not a Goblin - damage should NOT be prevented
        val life = driver.state.getEntity(opponent)?.get<LifeTotalComponent>()?.life ?: 0
        life shouldBe 18
    }

    test("shield is consumed after preventing damage once") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has Circle of Solace with chosen type "Goblin"
        val circle = driver.putPermanentOnBattlefield(opponent, "Circle of Solace")
        driver.replaceState(driver.state.updateEntity(circle) { c ->
            c.with(ChosenCreatureTypeComponent("Goblin"))
        })

        // Active player has two Goblins
        val goblin1 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Bully")
        val goblin2 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Bully")
        driver.removeSummoningSickness(goblin1)
        driver.removeSummoningSickness(goblin2)

        // Opponent activates Circle ONCE
        driver.passPriority(activePlayer)
        driver.giveMana(opponent, Color.WHITE, 2)
        driver.submit(
            ActivateAbility(
                playerId = opponent,
                sourceId = circle,
                abilityId = circleAbilityId
            )
        )
        driver.bothPass() // resolve

        // Attack with both Goblins (2 damage each)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(goblin1, goblin2), opponent)
        driver.bothPass() // declare blockers
        driver.bothPass() // no blocks
        driver.bothPass() // first strike
        driver.bothPass() // combat damage

        // One activation prevents damage from one Goblin, the other still deals 2
        val life = driver.state.getEntity(opponent)?.get<LifeTotalComponent>()?.life ?: 0
        life shouldBe 18
    }

    test("multiple activations prevent damage from multiple creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has Circle of Solace with chosen type "Goblin"
        val circle = driver.putPermanentOnBattlefield(opponent, "Circle of Solace")
        driver.replaceState(driver.state.updateEntity(circle) { c ->
            c.with(ChosenCreatureTypeComponent("Goblin"))
        })

        // Active player has two Goblins
        val goblin1 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Bully")
        val goblin2 = driver.putCreatureOnBattlefield(activePlayer, "Goblin Bully")
        driver.removeSummoningSickness(goblin1)
        driver.removeSummoningSickness(goblin2)

        // Opponent activates Circle TWICE
        driver.passPriority(activePlayer)
        driver.giveMana(opponent, Color.WHITE, 4)
        driver.submit(
            ActivateAbility(
                playerId = opponent,
                sourceId = circle,
                abilityId = circleAbilityId
            )
        )
        driver.bothPass() // resolve first activation

        driver.submit(
            ActivateAbility(
                playerId = opponent,
                sourceId = circle,
                abilityId = circleAbilityId
            )
        )
        driver.bothPass() // resolve second activation

        // Attack with both Goblins
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(goblin1, goblin2), opponent)
        driver.bothPass() // declare blockers
        driver.bothPass() // no blocks
        driver.bothPass() // first strike
        driver.bothPass() // combat damage

        // Both shields prevent damage - life stays at 20
        val life = driver.state.getEntity(opponent)?.get<LifeTotalComponent>()?.life ?: 0
        life shouldBe 20
    }
})
