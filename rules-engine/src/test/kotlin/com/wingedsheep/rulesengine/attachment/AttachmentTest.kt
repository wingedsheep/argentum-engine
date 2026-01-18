package com.wingedsheep.rulesengine.attachment

import com.wingedsheep.rulesengine.action.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.ManaSymbol
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.game.Phase
import com.wingedsheep.rulesengine.game.Step
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class AttachmentTest : FunSpec({

    fun createTestPlayer(id: String): Player {
        return Player.create(PlayerId.of(id), "${id}'s Deck")
    }

    fun createTestGameState(player1: Player, player2: Player): GameState {
        return GameState.newGame(player1, player2)
    }

    fun createCreature(name: String, controllerId: String): CardInstance {
        val def = CardDefinition.creature(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.W)),
            subtypes = setOf(Subtype.HUMAN),
            power = 2,
            toughness = 2
        )
        return CardInstance.create(def, controllerId)
    }

    fun createEquipment(name: String, controllerId: String): CardInstance {
        val def = CardDefinition.equipment(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.generic(2))),
            equipCost = ManaCost(listOf(ManaSymbol.generic(1))),
            oracleText = "Equipped creature gets +1/+1"
        )
        return CardInstance.create(def, controllerId)
    }

    fun createAura(name: String, controllerId: String): CardInstance {
        val def = CardDefinition.aura(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.W)),
            oracleText = "Enchant creature"
        )
        return CardInstance.create(def, controllerId)
    }

    fun createEnchantment(name: String, controllerId: String): CardInstance {
        val def = CardDefinition.enchantment(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.W, ManaSymbol.W)),
            oracleText = "All creatures get +1/+1"
        )
        return CardInstance.create(def, controllerId)
    }

    fun createArtifact(name: String, controllerId: String): CardInstance {
        val def = CardDefinition.artifact(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.generic(3))),
            oracleText = "Test artifact"
        )
        return CardInstance.create(def, controllerId)
    }

    context("Equipment attaching") {
        test("equipment can be attached to controller's creature") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Grizzly Bears", "player1")
            val equipment = createEquipment("Bonesplitter", "player1")

            state = state.updateBattlefield { it.addToTop(creature).addToTop(equipment) }
            // Advance to main phase for equip
            state = state.advanceToPhase(Phase.PRECOMBAT_MAIN).advanceToStep(Step.PRECOMBAT_MAIN)

            val action = AttachCard(
                attachmentId = equipment.id,
                targetId = creature.id,
                controllerId = PlayerId.of("player1")
            )

            val result = ActionExecutor.execute(state, action)

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            val attachedEquipment = newState.battlefield.getCard(equipment.id)!!
            attachedEquipment.attachedTo shouldBe creature.id
            attachedEquipment.isAttached shouldBe true
        }

        test("equipment cannot be attached to opponent's creature") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Grizzly Bears", "player2")  // Opponent's creature
            val equipment = createEquipment("Bonesplitter", "player1")

            state = state.updateBattlefield { it.addToTop(creature).addToTop(equipment) }
            state = state.advanceToPhase(Phase.PRECOMBAT_MAIN).advanceToStep(Step.PRECOMBAT_MAIN)

            val action = AttachCard(
                attachmentId = equipment.id,
                targetId = creature.id,
                controllerId = PlayerId.of("player1")
            )

            val result = ActionExecutor.execute(state, action)

            result.shouldBeInstanceOf<ActionResult.Failure>()
        }

        test("equipment can be moved to a different creature") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature1 = createCreature("Bear 1", "player1")
            val creature2 = createCreature("Bear 2", "player1")
            val equipment = createEquipment("Bonesplitter", "player1")

            state = state.updateBattlefield {
                it.addToTop(creature1).addToTop(creature2).addToTop(equipment)
            }
            state = state.advanceToPhase(Phase.PRECOMBAT_MAIN).advanceToStep(Step.PRECOMBAT_MAIN)

            // Attach to first creature
            val attach1 = AttachCard(equipment.id, creature1.id, PlayerId.of("player1"))
            var result = ActionExecutor.execute(state, attach1)
            state = (result as ActionResult.Success).state

            // Now attach to second creature
            val attach2 = AttachCard(equipment.id, creature2.id, PlayerId.of("player1"))
            result = ActionExecutor.execute(state, attach2)
            state = (result as ActionResult.Success).state

            val attachedEquipment = state.battlefield.getCard(equipment.id)!!
            attachedEquipment.attachedTo shouldBe creature2.id
        }
    }

    context("Aura attaching") {
        test("aura can be attached to any creature") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Grizzly Bears", "player2")  // Opponent's creature
            val aura = createAura("Pacifism", "player1")

            state = state.updateBattlefield { it.addToTop(creature).addToTop(aura) }

            val action = AttachCard(
                attachmentId = aura.id,
                targetId = creature.id,
                controllerId = PlayerId.of("player1")
            )

            val result = ActionExecutor.execute(state, action)

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            val attachedAura = newState.battlefield.getCard(aura.id)!!
            attachedAura.attachedTo shouldBe creature.id
        }
    }

    context("Detaching") {
        test("equipment can be detached") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Grizzly Bears", "player1")
            val equipment = createEquipment("Bonesplitter", "player1")

            state = state.updateBattlefield { it.addToTop(creature).addToTop(equipment) }
            state = state.advanceToPhase(Phase.PRECOMBAT_MAIN).advanceToStep(Step.PRECOMBAT_MAIN)

            // Attach first
            val attachAction = AttachCard(equipment.id, creature.id, PlayerId.of("player1"))
            var result = ActionExecutor.execute(state, attachAction)
            state = (result as ActionResult.Success).state

            // Detach
            val detachAction = DetachCard(equipment.id)
            result = ActionExecutor.execute(state, detachAction)

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            val detachedEquipment = newState.battlefield.getCard(equipment.id)!!
            detachedEquipment.attachedTo shouldBe null
            detachedEquipment.isAttached shouldBe false
        }

        test("detaching unattached card fails") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val equipment = createEquipment("Bonesplitter", "player1")
            state = state.updateBattlefield { it.addToTop(equipment) }

            val detachAction = DetachCard(equipment.id)
            val result = ActionExecutor.execute(state, detachAction)

            result.shouldBeInstanceOf<ActionResult.Failure>()
        }
    }

    context("Equip action") {
        test("equip action at sorcery speed succeeds") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Grizzly Bears", "player1")
            val equipment = createEquipment("Bonesplitter", "player1")

            state = state.updateBattlefield { it.addToTop(creature).addToTop(equipment) }
            state = state.advanceToPhase(Phase.PRECOMBAT_MAIN).advanceToStep(Step.PRECOMBAT_MAIN)

            val action = Equip(
                equipmentId = equipment.id,
                targetCreatureId = creature.id,
                controllerId = PlayerId.of("player1")
            )

            val result = ActionExecutor.execute(state, action)

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            newState.battlefield.getCard(equipment.id)!!.attachedTo shouldBe creature.id
        }

        test("equip action during combat fails") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Grizzly Bears", "player1")
            val equipment = createEquipment("Bonesplitter", "player1")

            state = state.updateBattlefield { it.addToTop(creature).addToTop(equipment) }
            state = state.advanceToPhase(Phase.COMBAT).advanceToStep(Step.DECLARE_ATTACKERS)

            val action = Equip(
                equipmentId = equipment.id,
                targetCreatureId = creature.id,
                controllerId = PlayerId.of("player1")
            )

            val result = ActionExecutor.execute(state, action)

            result.shouldBeInstanceOf<ActionResult.Failure>()
        }

        test("equip action on opponent's turn fails") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Grizzly Bears", "player1")
            val equipment = createEquipment("Bonesplitter", "player1")

            state = state.updateBattlefield { it.addToTop(creature).addToTop(equipment) }
            state = state.advanceToPhase(Phase.PRECOMBAT_MAIN).advanceToStep(Step.PRECOMBAT_MAIN)

            // Try to equip as player2 when it's player1's turn
            val action = Equip(
                equipmentId = equipment.id,
                targetCreatureId = creature.id,
                controllerId = PlayerId.of("player2")
            )

            val result = ActionExecutor.execute(state, action)

            result.shouldBeInstanceOf<ActionResult.Failure>()
        }
    }

    context("Card type detection") {
        test("enchantment is detected correctly") {
            val enchantment = createEnchantment("Glorious Anthem", "player1")
            enchantment.isEnchantment shouldBe true
            enchantment.isAura shouldBe false
            enchantment.isPermanent shouldBe true
        }

        test("aura is detected correctly") {
            val aura = createAura("Pacifism", "player1")
            aura.isEnchantment shouldBe true
            aura.isAura shouldBe true
            aura.isPermanent shouldBe true
        }

        test("artifact is detected correctly") {
            val artifact = createArtifact("Sol Ring", "player1")
            artifact.isArtifact shouldBe true
            artifact.isEquipment shouldBe false
            artifact.isPermanent shouldBe true
        }

        test("equipment is detected correctly") {
            val equipment = createEquipment("Bonesplitter", "player1")
            equipment.isArtifact shouldBe true
            equipment.isEquipment shouldBe true
            equipment.isPermanent shouldBe true
        }
    }

    context("CardDefinition factory methods") {
        test("enchantment factory creates correct type line") {
            val def = CardDefinition.enchantment(
                name = "Test Enchantment",
                manaCost = ManaCost(listOf(ManaSymbol.W))
            )
            def.isEnchantment shouldBe true
            def.isPermanent shouldBe true
        }

        test("aura factory creates correct type line") {
            val def = CardDefinition.aura(
                name = "Test Aura",
                manaCost = ManaCost(listOf(ManaSymbol.W))
            )
            def.isEnchantment shouldBe true
            def.isAura shouldBe true
        }

        test("artifact factory creates correct type line") {
            val def = CardDefinition.artifact(
                name = "Test Artifact",
                manaCost = ManaCost(listOf(ManaSymbol.generic(3)))
            )
            def.isArtifact shouldBe true
        }

        test("equipment factory creates correct type line and equip cost") {
            val def = CardDefinition.equipment(
                name = "Test Equipment",
                manaCost = ManaCost(listOf(ManaSymbol.generic(2))),
                equipCost = ManaCost(listOf(ManaSymbol.generic(1)))
            )
            def.isArtifact shouldBe true
            def.isEquipment shouldBe true
            def.equipCost shouldNotBe null
            def.equipCost!!.cmc shouldBe 1
        }

        test("artifact creature factory creates correct type line") {
            val def = CardDefinition.artifactCreature(
                name = "Ornithopter",
                manaCost = ManaCost.ZERO,
                subtypes = setOf(Subtype("Thopter")),
                power = 0,
                toughness = 2
            )
            def.isArtifact shouldBe true
            def.isCreature shouldBe true
            def.creatureStats shouldNotBe null
            def.creatureStats!!.basePower shouldBe 0
            def.creatureStats!!.baseToughness shouldBe 2
        }
    }
})
