package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.CorrosiveOoze
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Corrosive Ooze.
 *
 * Corrosive Ooze: {1}{G}
 * Creature — Ooze
 * 2/2
 * Whenever Corrosive Ooze blocks or becomes blocked by an equipped creature,
 * destroy all Equipment attached to that creature at end of combat.
 */
class CorrosiveOozeTest : FunSpec({

    // Simple test equipment: Artifact — Equipment with no abilities
    val testEquipment = CardDefinition(
        name = "Test Sword",
        manaCost = ManaCost.parse("{1}"),
        typeLine = TypeLine.parse("Artifact — Equipment"),
        oracleText = "Equip {1}",
        script = CardScript()
    )

    val testEquipment2 = CardDefinition(
        name = "Test Shield",
        manaCost = ManaCost.parse("{2}"),
        typeLine = TypeLine.parse("Artifact — Equipment"),
        oracleText = "Equip {2}",
        script = CardScript()
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.cardRegistry.register(CorrosiveOoze)
        driver.cardRegistry.register(testEquipment)
        driver.cardRegistry.register(testEquipment2)
        return driver
    }

    /**
     * Attach equipment to a creature by setting up the component relationships.
     */
    fun GameTestDriver.attachEquipment(equipmentId: EntityId, creatureId: EntityId) {
        // Add AttachedToComponent to the equipment
        state = state.updateEntity(equipmentId) { container ->
            container.with(AttachedToComponent(creatureId))
        }
        // Add/update AttachmentsComponent on the creature
        state = state.updateEntity(creatureId) { container ->
            val existing = container.get<AttachmentsComponent>()
            val newIds = (existing?.attachedIds ?: emptyList()) + equipmentId
            container.with(AttachmentsComponent(newIds))
        }
    }

    test("ooze blocks equipped creature - equipment destroyed at end of combat") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put equipped creature on attacker's side
        val equippedCreature = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")
        driver.removeSummoningSickness(equippedCreature)
        val sword = driver.putPermanentOnBattlefield(attacker, "Test Sword")
        driver.attachEquipment(sword, equippedCreature)

        // Put Corrosive Ooze on defender's side
        val ooze = driver.putCreatureOnBattlefield(defender, "Corrosive Ooze")
        driver.removeSummoningSickness(ooze)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with equipped creature
        driver.declareAttackers(attacker, listOf(equippedCreature), defender)
        driver.bothPass()

        // Block with Ooze
        driver.declareBlockers(defender, mapOf(ooze to listOf(equippedCreature)))
        driver.bothPass()

        // Trigger fires → delayed trigger created. Both pass to resolve.
        driver.bothPass()

        // Equipment should still exist during combat
        driver.findPermanent(attacker, "Test Sword") shouldNotBe null

        // Advance to end of combat - delayed trigger fires and destroys equipment
        driver.passPriorityUntil(Step.END_COMBAT)

        // Delayed trigger goes on stack, resolve it
        driver.bothPass()

        // Equipment should now be destroyed
        driver.findPermanent(attacker, "Test Sword") shouldBe null
        driver.getGraveyardCardNames(attacker) shouldContain "Test Sword"
    }

    test("ooze becomes blocked by equipped creature - equipment destroyed at end of combat") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put Corrosive Ooze on attacker's side
        val ooze = driver.putCreatureOnBattlefield(attacker, "Corrosive Ooze")
        driver.removeSummoningSickness(ooze)

        // Put equipped creature on defender's side
        val equippedBlocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(equippedBlocker)
        val sword = driver.putPermanentOnBattlefield(defender, "Test Sword")
        driver.attachEquipment(sword, equippedBlocker)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with Ooze
        driver.declareAttackers(attacker, listOf(ooze), defender)
        driver.bothPass()

        // Block with equipped creature
        driver.declareBlockers(defender, mapOf(equippedBlocker to listOf(ooze)))
        driver.bothPass()

        // Trigger fires → delayed trigger created. Both pass to resolve.
        driver.bothPass()

        // Advance to end of combat
        driver.passPriorityUntil(Step.END_COMBAT)

        // Delayed trigger resolves
        driver.bothPass()

        // Equipment should be destroyed
        driver.findPermanent(defender, "Test Sword") shouldBe null
        driver.getGraveyardCardNames(defender) shouldContain "Test Sword"
    }

    test("ooze blocks non-equipped creature - no trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put non-equipped creature on attacker's side
        val creature = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")
        driver.removeSummoningSickness(creature)

        // Put Corrosive Ooze on defender's side
        val ooze = driver.putCreatureOnBattlefield(defender, "Corrosive Ooze")
        driver.removeSummoningSickness(ooze)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with non-equipped creature
        driver.declareAttackers(attacker, listOf(creature), defender)
        driver.bothPass()

        // Block with Ooze — no trigger since creature isn't equipped
        driver.declareBlockers(defender, mapOf(ooze to listOf(creature)))
        driver.bothPass()

        // Should go straight to combat damage (no trigger on stack)
        driver.currentStep shouldBe Step.COMBAT_DAMAGE
    }

    test("multiple equipment on combat partner - all destroyed") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put creature with two equipment on attacker's side
        val equippedCreature = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")
        driver.removeSummoningSickness(equippedCreature)
        val sword = driver.putPermanentOnBattlefield(attacker, "Test Sword")
        val shield = driver.putPermanentOnBattlefield(attacker, "Test Shield")
        driver.attachEquipment(sword, equippedCreature)
        driver.attachEquipment(shield, equippedCreature)

        // Put Corrosive Ooze on defender's side
        val ooze = driver.putCreatureOnBattlefield(defender, "Corrosive Ooze")
        driver.removeSummoningSickness(ooze)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with equipped creature
        driver.declareAttackers(attacker, listOf(equippedCreature), defender)
        driver.bothPass()

        // Block with Ooze
        driver.declareBlockers(defender, mapOf(ooze to listOf(equippedCreature)))
        driver.bothPass()

        // Trigger → delayed trigger. Resolve.
        driver.bothPass()

        // Advance to end of combat
        driver.passPriorityUntil(Step.END_COMBAT)

        // Delayed trigger resolves
        driver.bothPass()

        // Both equipment should be destroyed
        driver.findPermanent(attacker, "Test Sword") shouldBe null
        driver.findPermanent(attacker, "Test Shield") shouldBe null
        driver.getGraveyardCardNames(attacker) shouldContain "Test Sword"
        driver.getGraveyardCardNames(attacker) shouldContain "Test Shield"
    }

    test("combat partner dies before end of combat - no crash") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put small equipped creature (dies to 2 combat damage from ooze)
        val equippedCreature = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(equippedCreature)
        val sword = driver.putPermanentOnBattlefield(attacker, "Test Sword")
        driver.attachEquipment(sword, equippedCreature)

        // Put Corrosive Ooze on defender's side
        val ooze = driver.putCreatureOnBattlefield(defender, "Corrosive Ooze")
        driver.removeSummoningSickness(ooze)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with equipped creature
        driver.declareAttackers(attacker, listOf(equippedCreature), defender)
        driver.bothPass()

        // Block with Ooze
        driver.declareBlockers(defender, mapOf(ooze to listOf(equippedCreature)))
        driver.bothPass()

        // Trigger → delayed trigger. Resolve.
        driver.bothPass()

        // Combat damage kills Grizzly Bears (2/2 takes 2 damage)
        // Equipment goes to battlefield (unattached) since creature died
        driver.currentStep shouldBe Step.COMBAT_DAMAGE

        // Advance to end of combat
        driver.passPriorityUntil(Step.END_COMBAT)

        // Delayed trigger fires — creature is gone, effect should gracefully do nothing
        driver.bothPass()

        // Grizzly Bears should be dead
        driver.findPermanent(attacker, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(attacker) shouldContain "Grizzly Bears"
    }
})
