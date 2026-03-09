package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.filters.unified.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.ints.shouldBeGreaterThan

/**
 * Tests for death triggers (when a creature dies).
 *
 * ## Covered Scenarios
 * - Death trigger fires when creature dies from lethal damage (via spell)
 * - Death trigger effect is executed (controller gains life)
 * - Filtered ANY-binding death trigger fires when the source creature itself dies
 *
 * The key issue being tested: Death triggers must fire when creatures die
 * from state-based actions (SBAs). The events from SBAs must be included
 * when detecting triggers after spell/ability resolution.
 */
class DeathTriggerTest : FunSpec({

    /**
     * 3/4 creature with "Whenever a creature you control with toughness 4 or greater dies, you gain 4 life."
     * Tests ANY-binding filtered death triggers (like Sultai Flayer).
     */
    val filteredDeathTriggerCreature = CardDefinition.creature(
        name = "Filtered Death Trigger Creature",
        manaCost = ManaCost.parse("{3}{G}"),
        subtypes = setOf(Subtype("Test")),
        power = 3,
        toughness = 4,
        oracleText = "Whenever a creature you control with toughness 4 or greater dies, you gain 4 life.",
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = GameEvent.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.youControl().toughnessAtLeast(4),
                    from = Zone.BATTLEFIELD,
                    to = Zone.GRAVEYARD
                ),
                binding = TriggerBinding.ANY,
                effect = GainLifeEffect(4)
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.cardRegistry.register(filteredDeathTriggerCreature)
        return driver
    }

    test("death trigger fires when creature is destroyed by lethal damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Death Trigger Test Creature on the battlefield for active player
        // This creature has: "When this creature dies, you gain 3 life."
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Death Trigger Test Creature")
        creature shouldNotBe null

        // Verify starting life
        driver.getLifeTotal(activePlayer) shouldBe 20

        // Give active player mana to cast Lightning Bolt
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")

        // Cast Lightning Bolt targeting our own creature (to trigger death)
        val castResult = driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(creature)))
        castResult.isSuccess shouldBe true

        // Resolve the spell (creature takes 3 damage, dies from SBA)
        driver.bothPass()

        // The creature should be dead (moved to graveyard)
        driver.findPermanent(activePlayer, "Death Trigger Test Creature") shouldBe null

        // The death trigger should be on the stack
        // This is the key assertion - the fix ensures SBA events (death) are
        // included in trigger detection after spell resolution
        driver.stackSize shouldBeGreaterThan 0

        // Resolve the death trigger
        driver.bothPass()

        // Active player should have gained 3 life from the death trigger
        driver.getLifeTotal(activePlayer) shouldBe 23
    }

    test("filtered ANY-binding death trigger fires when source creature itself dies") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 20,
                "Mountain" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put the filtered death trigger creature (3/4) on the battlefield
        // It has: "Whenever a creature you control with toughness 4 or greater dies, you gain 4 life."
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Filtered Death Trigger Creature")
        creature shouldNotBe null

        // Verify starting life
        driver.getLifeTotal(activePlayer) shouldBe 20

        // Give active player mana to cast two Lightning Bolts (need 6 damage to kill a 3/4)
        driver.giveMana(activePlayer, Color.RED, 2)
        val bolt1 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val bolt2 = driver.putCardInHand(activePlayer, "Lightning Bolt")

        // Cast first Lightning Bolt targeting our creature
        val castResult1 = driver.castSpellWithTargets(activePlayer, bolt1, listOf(ChosenTarget.Permanent(creature)))
        castResult1.isSuccess shouldBe true
        driver.bothPass() // Resolve first bolt (3 damage, creature survives)

        // Cast second Lightning Bolt targeting our creature
        val castResult2 = driver.castSpellWithTargets(activePlayer, bolt2, listOf(ChosenTarget.Permanent(creature)))
        castResult2.isSuccess shouldBe true
        driver.bothPass() // Resolve second bolt (3 more damage, creature dies from SBA)

        // The creature should be dead
        driver.findPermanent(activePlayer, "Filtered Death Trigger Creature") shouldBe null

        // The death trigger should be on the stack - the creature itself has toughness >= 4
        // and was controlled by its owner, so it should trigger its own ability
        driver.stackSize shouldBeGreaterThan 0

        // Resolve the death trigger
        driver.bothPass()

        // Active player should have gained 4 life from the death trigger
        driver.getLifeTotal(activePlayer) shouldBe 24
    }
})
