package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.mtg.sets.definitions.dom.cards.ForebearsBlade
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Forebear's Blade equipped creature dies trigger.
 *
 * Card reference:
 * - Forebear's Blade ({3}): Artifact — Equipment
 *   "Equipped creature gets +3/+0 and has vigilance and trample.
 *    Whenever equipped creature dies, attach Forebear's Blade to target creature you control.
 *    Equip {3}"
 */
class ForebearsBladeTriggerTest : FunSpec({

    val TestCreature = CardDefinition.creature(
        name = "Test Soldier",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Soldier")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    val TestCreature2 = CardDefinition.creature(
        name = "Test Knight",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Knight")),
        power = 3,
        toughness = 3,
        oracleText = ""
    )

    /**
     * Put equipment on battlefield attached to a creature, using putPermanentOnBattlefield
     * (which sets up ContinuousEffectComponent) then manually attaching.
     */
    fun GameTestDriver.putEquipmentAttached(
        playerId: EntityId,
        cardName: String,
        targetCreatureId: EntityId
    ): EntityId {
        val equipmentId = putPermanentOnBattlefield(playerId, cardName)

        // Attach equipment to creature
        var newState = state.updateEntity(equipmentId) { c ->
            c.with(AttachedToComponent(targetCreatureId))
        }
        val existingAttachments = newState.getEntity(targetCreatureId)
            ?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        newState = newState.updateEntity(targetCreatureId) { c ->
            c.with(AttachmentsComponent(existingAttachments + equipmentId))
        }
        replaceState(newState)
        return equipmentId
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestCreature, TestCreature2, ForebearsBlade))
        return driver
    }

    val stateProjector = StateProjector()

    test("equipped creature gets +3/+0 and has vigilance and trample") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Swamp" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(activePlayer, "Test Soldier")
        driver.putEquipmentAttached(activePlayer, "Forebear's Blade", creature)

        val projected = stateProjector.project(driver.state)
        projected.getPower(creature) shouldBe 5  // 2 + 3
        projected.getToughness(creature) shouldBe 2  // unchanged
        projected.hasKeyword(creature, Keyword.VIGILANCE) shouldBe true
        projected.hasKeyword(creature, Keyword.TRAMPLE) shouldBe true
    }

    test("when equipped creature dies, trigger lets you attach to another creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Swamp" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two creatures and the equipment on battlefield
        val creature1 = driver.putCreatureOnBattlefield(activePlayer, "Test Soldier")
        val creature2 = driver.putCreatureOnBattlefield(activePlayer, "Test Knight")
        val equipment = driver.putEquipmentAttached(activePlayer, "Forebear's Blade", creature1)

        // Verify equipment is attached to creature1
        driver.state.getEntity(equipment)?.get<AttachedToComponent>()?.targetId shouldBe creature1

        // Destroy creature1 with Doom Blade
        driver.giveMana(activePlayer, Color.BLACK, 2)
        val doomBlade = driver.putCardInHand(activePlayer, "Doom Blade")
        driver.castSpellWithTargets(activePlayer, doomBlade, listOf(ChosenTarget.Permanent(creature1)))
        driver.bothPass()

        // Creature1 should be dead
        driver.getPermanents(activePlayer).contains(creature1) shouldBe false

        // Equipment's trigger should fire - we need a target decision for "creature you control"
        driver.pendingDecision shouldNotBe null

        // Select creature2 as the target
        driver.submitTargetSelection(activePlayer, listOf(creature2))

        // Resolve the triggered ability on the stack
        driver.bothPass()

        // Equipment should now be attached to creature2
        driver.state.getEntity(equipment)?.get<AttachedToComponent>()?.targetId shouldBe creature2

        // Creature2 should now get the bonuses
        val projected = stateProjector.project(driver.state)
        projected.getPower(creature2) shouldBe 6  // 3 + 3
        projected.hasKeyword(creature2, Keyword.VIGILANCE) shouldBe true
        projected.hasKeyword(creature2, Keyword.TRAMPLE) shouldBe true
    }

    test("when equipped creature dies and no other creatures, blade stays unattached") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Swamp" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put only one creature and the equipment
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Test Soldier")
        val equipment = driver.putEquipmentAttached(activePlayer, "Forebear's Blade", creature)

        // Destroy the creature
        driver.giveMana(activePlayer, Color.BLACK, 2)
        val doomBlade = driver.putCardInHand(activePlayer, "Doom Blade")
        driver.castSpellWithTargets(activePlayer, doomBlade, listOf(ChosenTarget.Permanent(creature)))
        driver.bothPass()

        // Creature should be dead
        driver.getPermanents(activePlayer).contains(creature) shouldBe false

        // Equipment should remain on the battlefield unattached
        // (trigger has no valid target so it doesn't go on stack)
        driver.getPermanents(activePlayer).contains(equipment) shouldBe true
        driver.state.getEntity(equipment)?.get<AttachedToComponent>() shouldBe null
    }

    /**
     * Regression: the dies trigger must fire the same way whether the host was destroyed by an
     * effect or died to lethal damage as a state-based action.
     *
     * The two paths unattach the Equipment at different moments. A destroy effect emits the zone
     * change during resolution, so the attachment link is still live when triggers are detected.
     * Lethal damage does not — CR 704.5g (lethal damage) and CR 704.5m (unattach) are both
     * state-based actions applied in the *same* pass, so the link is already gone. The detector
     * falls back to the host's last-known attachment ids (CR 608.2h) to cover it; before that
     * fallback existed, every "whenever equipped creature dies" Equipment silently no-opped on
     * the combat-damage path.
     */
    test("the dies trigger also fires when the equipped creature dies to lethal damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Mountain" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature1 = driver.putCreatureOnBattlefield(activePlayer, "Test Soldier")
        val creature2 = driver.putCreatureOnBattlefield(activePlayer, "Test Knight")
        val equipment = driver.putEquipmentAttached(activePlayer, "Forebear's Blade", creature1)

        // Bolt the 2/2 host — it dies to state-based actions, not to a destroy effect.
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(creature1)))
        driver.bothPass()

        driver.getPermanents(activePlayer).contains(creature1) shouldBe false

        // The trigger still fired and is asking for its target.
        driver.pendingDecision shouldNotBe null
        driver.submitTargetSelection(activePlayer, listOf(creature2))
        driver.bothPass()

        driver.state.getEntity(equipment)?.get<AttachedToComponent>()?.targetId shouldBe creature2
    }
})
