package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khans.cards.HeartPiercerBow
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests that equipment remains on the battlefield when the equipped creature dies.
 * Rule 704.5p: Equipment that's attached to an illegal permanent becomes unattached
 * but remains on the battlefield.
 */
class EquipmentPersistsTest : FunSpec({

    val TestCreature = CardDefinition.creature(
        name = "Test Soldier",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Soldier")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    fun GameTestDriver.putEquipmentOnBattlefield(
        playerId: EntityId,
        cardDef: CardDefinition,
        targetCreatureId: EntityId
    ): EntityId {
        val equipmentId = EntityId.generate()

        val cardComponent = CardComponent(
            cardDefinitionId = cardDef.name,
            name = cardDef.name,
            manaCost = cardDef.manaCost,
            typeLine = cardDef.typeLine,
            oracleText = cardDef.oracleText,
            baseStats = cardDef.creatureStats,
            baseKeywords = cardDef.keywords,
            baseFlags = cardDef.flags,
            colors = cardDef.colors,
            ownerId = playerId,
            spellEffect = cardDef.spellEffect
        )

        var container = ComponentContainer.of(
            cardComponent,
            OwnerComponent(playerId),
            ControllerComponent(playerId),
            AttachedToComponent(targetCreatureId)
        )

        var newState = state.withEntity(equipmentId, container)
        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, equipmentId)

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
        driver.registerCards(TestCards.all + listOf(TestCreature))
        return driver
    }

    test("equipment stays on battlefield when equipped creature is destroyed") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 10, "Swamp" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creature + equipment on battlefield
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Test Soldier")
        val equipment = driver.putEquipmentOnBattlefield(activePlayer, HeartPiercerBow, creature)

        // Verify equipment is attached
        driver.state.getEntity(equipment)?.get<AttachedToComponent>() shouldNotBe null

        // Destroy the creature with Doom Blade
        driver.giveMana(activePlayer, Color.BLACK, 2)
        val doomBlade = driver.putCardInHand(activePlayer, "Doom Blade")
        driver.castSpellWithTargets(activePlayer, doomBlade, listOf(ChosenTarget.Permanent(creature)))
        driver.bothPass()

        // Creature should be gone
        driver.getPermanents(activePlayer).contains(creature) shouldBe false

        // Equipment should still be on the battlefield
        driver.getPermanents(activePlayer).contains(equipment) shouldBe true

        // Equipment should no longer be attached to anything
        driver.state.getEntity(equipment)?.get<AttachedToComponent>() shouldBe null
    }
})
