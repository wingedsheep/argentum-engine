package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for CantAttackGroupEffect.
 * Verifies that creatures matching the filter cannot be declared as attackers.
 */
class CantAttackGroupTest : FunSpec({

    val HaltAttack = card("Halt Attack") {
        manaCost = "{1}{W}"
        typeLine = "Sorcery"

        spell {
            effect = Effects.CantAttackGroup(GroupFilter.AllCreatures)
        }
    }

    val TestCreature = CardDefinition.creature(
        name = "Test Warrior",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Human"), Subtype("Warrior")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HaltAttack, TestCreature))
        return driver
    }

    test("CantAttackGroup floating effect marks creatures as unable to attack in projected state") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Mountain" to 10),
            startingLife = 20
        )

        val p1 = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(p1, "Test Warrior")

        // Cast and resolve the sorcery
        driver.giveMana(p1, Color.WHITE, 2)
        val spell = driver.putCardInHand(p1, "Halt Attack")
        val castResult = driver.castSpell(p1, spell)
        withClue("Cast should succeed") { castResult.error shouldBe null }
        driver.bothPass()

        // The floating effect should be created
        withClue("Floating effect should exist after resolution") {
            driver.state.floatingEffects.size shouldBe 1
        }

        // Projected state should mark creature as can't attack
        withClue("Creature should be marked can't attack") {
            driver.state.projectedState.cantAttack(creature) shouldBe true
        }
    }

    test("CantAttackGroup prevents attack declarations") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Mountain" to 10),
            startingLife = 20
        )

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creature on battlefield first
        val creature = driver.putCreatureOnBattlefield(p1, "Test Warrior")

        // Inject the floating effect and set step directly to DECLARE_ATTACKERS
        // (avoids passPriorityUntil crossing turn boundary and expiring EndOfTurn effects)
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.SetCantAttack,
                affectedEntities = emptySet(),
                dynamicGroupFilter = GroupFilter.AllCreatures
            ),
            duration = Duration.EndOfTurn,
            sourceId = null,
            controllerId = p1,
            timestamp = System.currentTimeMillis()
        )
        driver.replaceState(driver.state.copy(
            floatingEffects = driver.state.floatingEffects + floatingEffect,
            phase = Phase.COMBAT,
            step = Step.DECLARE_ATTACKERS
        ))

        // Verify projected state marks creature as can't attack
        withClue("Projected state should show can't attack at combat time") {
            driver.state.projectedState.cantAttack(creature) shouldBe true
        }

        // Try to declare attacker - should fail
        val result = driver.declareAttackers(p1, mapOf(creature to p2))
        withClue("Attack should be rejected because creature can't attack") {
            result.error shouldNotBe null
        }
    }

    test("creatures can still attack without the effect") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Mountain" to 10),
            startingLife = 20
        )

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(p1, "Test Warrior")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val result = driver.declareAttackers(p1, mapOf(creature to p2))
        result.error shouldBe null
    }

    test("effect applies dynamically to all creatures on battlefield (Rule 611.2c)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Mountain" to 10),
            startingLife = 20
        )

        val p1 = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Inject the floating effect - simulating it was created earlier
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.SetCantAttack,
                affectedEntities = emptySet(),
                dynamicGroupFilter = GroupFilter.AllCreatures
            ),
            duration = Duration.EndOfTurn,
            sourceId = null,
            controllerId = p1,
            timestamp = System.currentTimeMillis()
        )
        driver.replaceState(driver.state.copy(
            floatingEffects = driver.state.floatingEffects + floatingEffect
        ))

        // Put creature on battlefield AFTER the effect exists
        val creature = driver.putCreatureOnBattlefield(p1, "Test Warrior")

        // The creature should still be affected by the dynamic filter
        withClue("Creature entering after effect should still be restricted (Rule 611.2c)") {
            driver.state.projectedState.cantAttack(creature) shouldBe true
        }
    }
})
