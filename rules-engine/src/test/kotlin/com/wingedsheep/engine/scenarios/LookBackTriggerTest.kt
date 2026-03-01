package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.DestroyAllEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * Tests for Rule 603.10: "Looking back in time" for triggered abilities.
 *
 * When multiple permanents are destroyed simultaneously (e.g., by a board wipe),
 * the engine must check triggered abilities based on the game state just before
 * the destruction. Permanents that left the battlefield at the same time as the
 * event source should still see those events trigger.
 *
 * ## Key Scenario
 * An artifact with "Whenever a creature dies, you gain 1 life" is destroyed at the
 * same time as three creatures by a "Destroy all nonland permanents" spell. The
 * artifact was on the battlefield when the creatures died, so it should trigger
 * three times — the player gains 3 life.
 */
class LookBackTriggerTest : FunSpec({

    /**
     * Artifact: "Whenever a creature dies, you gain 1 life."
     */
    val SoulShrine = CardDefinition.artifact(
        name = "Soul Shrine",
        manaCost = ManaCost.parse("{2}"),
        oracleText = "Whenever a creature dies, you gain 1 life.",
        script = CardScript.permanent(
            triggeredAbilities = listOf(
                TriggeredAbility(
                    id = AbilityId("soul-shrine-trigger"),
                    trigger = GameEvent.ZoneChangeEvent(
                        filter = GameObjectFilter.Creature,
                        from = Zone.BATTLEFIELD,
                        to = Zone.GRAVEYARD
                    ),
                    binding = TriggerBinding.ANY,
                    effect = GainLifeEffect(1)
                )
            )
        )
    )

    /**
     * Sorcery: "Destroy all nonland permanents."
     * Destroys creatures and artifacts simultaneously.
     */
    val Devastation = CardDefinition.sorcery(
        name = "Devastation",
        manaCost = ManaCost.parse("{3}{W}{W}"),
        oracleText = "Destroy all nonland permanents.",
        script = CardScript.spell(
            effect = DestroyAllEffect(
                filter = GameObjectFilter.NonlandPermanent,
                canRegenerate = false
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SoulShrine, Devastation))
        return driver
    }

    test("Rule 603.10 - artifact destroyed with creatures still triggers for each creature death") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put the artifact and three creatures on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Soul Shrine")
        driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")
        driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")

        // Verify starting life
        driver.getLifeTotal(activePlayer) shouldBe 20

        // Cast "Devastation" to destroy all nonland permanents
        val spell = driver.putCardInHand(activePlayer, "Devastation")
        driver.giveMana(activePlayer, Color.WHITE, 5)
        driver.castSpell(activePlayer, spell)

        // Resolve the board wipe — all creatures and the artifact are destroyed simultaneously
        driver.bothPass()

        // Rule 603.10: The artifact was on the battlefield when the creatures died,
        // so its "Whenever a creature dies" trigger should fire 3 times (once per creature).
        // The artifact dying does NOT count (it's not a creature).
        driver.stackSize shouldBeGreaterThanOrEqual 3

        // Resolve all three triggers
        driver.bothPass() // Trigger 1
        driver.bothPass() // Trigger 2
        driver.bothPass() // Trigger 3

        // Active player should have gained 3 life (1 per creature death)
        driver.getLifeTotal(activePlayer) shouldBe 23
    }
})
