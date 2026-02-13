package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ChooseCreatureTypeMustAttackEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Tests for Walking Desecration.
 *
 * Walking Desecration ({2}{B}, Creature — Zombie, 1/1):
 * {B}, {T}: Creatures of the creature type of your choice attack this turn if able.
 */
class WalkingDesecrationTest : FunSpec({

    val abilityId = AbilityId(UUID.randomUUID().toString())

    // Simple Elf creature for testing creature type selection
    val ElvishWarrior = CardDefinition.creature(
        name = "Elvish Warrior",
        manaCost = ManaCost.parse("{G}{G}"),
        oracleText = "",
        power = 2,
        toughness = 3,
        subtypes = setOf(Subtype("Elf"), Subtype("Warrior"))
    )

    val WalkingDesecration = CardDefinition.creature(
        name = "Walking Desecration",
        manaCost = ManaCost.parse("{2}{B}"),
        oracleText = "{B}, {T}: Creatures of the creature type of your choice attack this turn if able.",
        power = 1,
        toughness = 1,
        subtypes = setOf(Subtype("Zombie")),
        script = CardScript.permanent(
            ActivatedAbility(
                id = abilityId,
                cost = AbilityCost.Composite(listOf(
                    AbilityCost.Mana(ManaCost.parse("{B}")),
                    AbilityCost.Tap
                )),
                effect = ChooseCreatureTypeMustAttackEffect
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WalkingDesecration)
        driver.registerCard(ElvishWarrior)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Grizzly Bears" to 10,
                "Elvish Warrior" to 10
            ),
            skipMulligans = true
        )
        return driver
    }

    test("Walking Desecration ability marks creatures of chosen type with MustAttackThisTurnComponent") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Walking Desecration on battlefield
        val desecration = driver.putCreatureOnBattlefield(activePlayer, "Walking Desecration")
        driver.removeSummoningSickness(desecration)
        driver.giveMana(activePlayer, Color.BLACK, 1)

        // Put some Elves on the battlefield for the opponent
        val elf1 = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")
        driver.removeSummoningSickness(elf1)
        val elf2 = driver.putCreatureOnBattlefield(opponent, "Elvish Warrior")
        driver.removeSummoningSickness(elf2)

        // Put a non-Elf creature for the opponent (should not be affected)
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(bear)

        // Activate the ability
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = desecration,
                abilityId = abilityId
            )
        )
        result.isSuccess shouldBe true

        // Resolve the ability
        driver.bothPass()

        // Choose "Elf" creature type
        val decision = driver.pendingDecision as ChooseOptionDecision
        val elfIndex = decision.options.indexOf("Elf")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, elfIndex))

        // Verify Elves have MustAttackThisTurnComponent
        driver.state.getEntity(elf1)?.has<MustAttackThisTurnComponent>() shouldBe true
        driver.state.getEntity(elf2)?.has<MustAttackThisTurnComponent>() shouldBe true

        // Verify non-Elf creature does NOT have MustAttackThisTurnComponent
        driver.state.getEntity(bear)?.has<MustAttackThisTurnComponent>() shouldBe false
    }

    test("Creatures with MustAttackThisTurnComponent must attack when declaring attackers") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Bear on the battlefield for the active player
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(bear)

        // Manually add MustAttackThisTurnComponent
        driver.replaceState(
            driver.state.updateEntity(bear) { it.with(MustAttackThisTurnComponent) }
        )

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val opponent = driver.getOpponent(activePlayer)

        // Try to declare no attackers - should fail
        val noAttackResult = driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = emptyMap()
            )
        )
        noAttackResult.isSuccess shouldBe false

        // Declare the bear as attacker - should succeed
        val attackResult = driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = mapOf(bear to opponent)
            )
        )
        attackResult.isSuccess shouldBe true
    }

    test("MustAttackThisTurnComponent is cleaned up at end of turn") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Bear and manually mark it
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.removeSummoningSickness(bear)
        driver.replaceState(
            driver.state.updateEntity(bear) { it.with(MustAttackThisTurnComponent) }
        )

        // Verify it has the component
        driver.state.getEntity(bear)?.has<MustAttackThisTurnComponent>() shouldBe true

        // Advance to end of turn (pass through combat declaring bear as attacker)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val opponent = driver.getOpponent(activePlayer)
        driver.submit(DeclareAttackers(activePlayer, mapOf(bear to opponent)))

        // Pass through rest of combat and into cleanup
        driver.passPriorityUntil(Step.CLEANUP)

        // Component should be removed during cleanup
        driver.state.getEntity(bear)?.has<MustAttackThisTurnComponent>() shouldBe false
    }

    test("Creatures with summoning sickness are not forced to attack") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Bear with summoning sickness
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        // Don't remove summoning sickness

        // Mark it as must attack
        driver.replaceState(
            driver.state.updateEntity(bear) { it.with(MustAttackThisTurnComponent) }
        )

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Declaring no attackers should succeed (creature can't attack due to sickness)
        val result = driver.submit(
            DeclareAttackers(
                playerId = activePlayer,
                attackers = emptyMap()
            )
        )
        result.isSuccess shouldBe true
    }
})
