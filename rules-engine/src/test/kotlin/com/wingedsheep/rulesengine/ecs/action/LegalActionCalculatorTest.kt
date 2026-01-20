package com.wingedsheep.rulesengine.ecs.action

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.combat.CombatState
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.layers.Layer
import com.wingedsheep.rulesengine.ecs.layers.Modification
import com.wingedsheep.rulesengine.ecs.layers.Modifier
import com.wingedsheep.rulesengine.ecs.layers.ModifierFilter
import com.wingedsheep.rulesengine.ecs.layers.StateProjector
import com.wingedsheep.rulesengine.game.Step
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class LegalActionCalculatorTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    val hasteBearDef = CardDefinition.creature(
        name = "Raging Goblin",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype.GOBLIN),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.HASTE)
    )

    val defenderWallDef = CardDefinition.creature(
        name = "Wall of Stone",
        manaCost = ManaCost.parse("{1}{R}{R}"),
        subtypes = setOf(Subtype.WALL),
        power = 0,
        toughness = 8,
        keywords = setOf(Keyword.DEFENDER)
    )

    val forestDef = CardDefinition.basicLand("Forest", Subtype.FOREST)

    val lightningBoltDef = CardDefinition.instant(
        name = "Lightning Bolt",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Lightning Bolt deals 3 damage to any target."
    )

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun GameState.withStep(step: Step): GameState =
        copy(turnState = turnState.copy(step = step, phase = step.phase))

    fun GameState.withActivePlayer(playerId: EntityId): GameState =
        copy(turnState = turnState.copy(activePlayer = playerId, priorityPlayer = playerId))

    fun GameState.addCardToHand(playerId: EntityId, definition: CardDefinition): Pair<EntityId, GameState> {
        val (cardId, state1) = createEntity(
            EntityId.generate(),
            CardComponent(definition, playerId),
            ControllerComponent(playerId)
        )
        return cardId to state1.addToZone(cardId, ZoneId.hand(playerId))
    }

    fun GameState.addCreatureToBattlefield(
        controllerId: EntityId,
        definition: CardDefinition,
        tapped: Boolean = false,
        summoningSickness: Boolean = true
    ): Pair<EntityId, GameState> {
        var (creatureId, state1) = createEntity(
            EntityId.generate(),
            CardComponent(definition, controllerId),
            ControllerComponent(controllerId)
        )
        if (tapped) {
            state1 = state1.updateEntity(creatureId) { it.with(TappedComponent) }
        }
        if (summoningSickness) {
            state1 = state1.updateEntity(creatureId) { it.with(SummoningSicknessComponent) }
        }
        return creatureId to state1.addToZone(creatureId, ZoneId.BATTLEFIELD)
    }

    fun GameState.addLandToBattlefield(controllerId: EntityId, tapped: Boolean = false): Pair<EntityId, GameState> {
        var (landId, state1) = createEntity(
            EntityId.generate(),
            CardComponent(forestDef, controllerId),
            ControllerComponent(controllerId)
        )
        if (tapped) {
            state1 = state1.updateEntity(landId) { it.with(TappedComponent) }
        }
        return landId to state1.addToZone(landId, ZoneId.BATTLEFIELD)
    }

    val calculator = LegalActionCalculator()

    context("priority") {
        test("active player can pass priority") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.PRECOMBAT_MAIN)

            val actions = calculator.calculateLegalActions(state, player1Id)

            actions.canPassPriority shouldBe true
        }

        test("non-active player cannot act without priority") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.PRECOMBAT_MAIN)

            val actions = calculator.calculateLegalActions(state, player2Id)

            // Player 2 may still have priority in some cases, but no special actions
            actions.playableLands.shouldBeEmpty()
            actions.declarableAttackers.shouldBeEmpty()
        }
    }

    context("playing lands") {
        test("can play land from hand during main phase on your turn") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.PRECOMBAT_MAIN)
            val (forestId, stateWithLand) = state.addCardToHand(player1Id, forestDef)

            val actions = calculator.calculateLegalActions(stateWithLand, player1Id)

            actions.playableLands shouldHaveSize 1
            actions.playableLands[0].cardId shouldBe forestId
            actions.playableLands[0].cardName shouldBe "Forest"
        }

        test("cannot play land during combat phase") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)
            val (_, stateWithLand) = state.addCardToHand(player1Id, forestDef)

            val actions = calculator.calculateLegalActions(stateWithLand, player1Id)

            actions.playableLands.shouldBeEmpty()
        }

        test("cannot play land on opponent's turn") {
            val state = newGame()
                .withActivePlayer(player2Id)
                .withStep(Step.PRECOMBAT_MAIN)
            val (_, stateWithLand) = state.addCardToHand(player1Id, forestDef)

            val actions = calculator.calculateLegalActions(stateWithLand, player1Id)

            actions.playableLands.shouldBeEmpty()
        }

        test("cannot play land if already played one this turn") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.PRECOMBAT_MAIN)
            val (_, stateWithLand) = state.addCardToHand(player1Id, forestDef)
            val stateWithLandPlayed = stateWithLand.updateEntity(player1Id) {
                it.with(LandsPlayedComponent(count = 1, maximum = 1))
            }

            val actions = calculator.calculateLegalActions(stateWithLandPlayed, player1Id)

            actions.playableLands.shouldBeEmpty()
        }
    }

    context("casting spells") {
        test("can cast creature during main phase with enough mana") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.PRECOMBAT_MAIN)
            val (_, stateWithCreature) = state.addCardToHand(player1Id, bearDef)
            // Add 2 untapped lands to provide mana
            val (_, stateWithLand1) = stateWithCreature.addLandToBattlefield(player1Id)
            val (_, stateWithLand2) = stateWithLand1.addLandToBattlefield(player1Id)

            val actions = calculator.calculateLegalActions(stateWithLand2, player1Id)

            actions.castableSpells shouldHaveSize 1
            actions.castableSpells[0].cardName shouldBe "Grizzly Bears"
            actions.castableSpells[0].isCreature shouldBe true
        }

        test("cannot cast creature without enough mana") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.PRECOMBAT_MAIN)
            val (_, stateWithCreature) = state.addCardToHand(player1Id, bearDef)
            // Only 1 land (bear costs 2)
            val (_, stateWithLand) = stateWithCreature.addLandToBattlefield(player1Id)

            val actions = calculator.calculateLegalActions(stateWithLand, player1Id)

            actions.castableSpells.shouldBeEmpty()
        }

        test("cannot cast creature during combat phase") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)
            val (_, stateWithCreature) = state.addCardToHand(player1Id, bearDef)
            val (_, stateWithLand1) = stateWithCreature.addLandToBattlefield(player1Id)
            val (_, stateWithLand2) = stateWithLand1.addLandToBattlefield(player1Id)

            val actions = calculator.calculateLegalActions(stateWithLand2, player1Id)

            actions.castableSpells.shouldBeEmpty()
        }

        test("can cast instant during any step with priority") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)
            val (_, stateWithInstant) = state.addCardToHand(player1Id, lightningBoltDef)
            val (_, stateWithLand) = stateWithInstant.addLandToBattlefield(player1Id)

            val actions = calculator.calculateLegalActions(stateWithLand, player1Id)

            actions.castableSpells shouldHaveSize 1
            actions.castableSpells[0].cardName shouldBe "Lightning Bolt"
        }
    }

    context("mana abilities") {
        test("can activate mana ability from untapped land") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.PRECOMBAT_MAIN)
            val (forestId, stateWithLand) = state.addLandToBattlefield(player1Id, tapped = false)

            val actions = calculator.calculateLegalActions(stateWithLand, player1Id)

            actions.activatableAbilities shouldHaveSize 1
            actions.activatableAbilities[0].sourceId shouldBe forestId
            actions.activatableAbilities[0].isManaAbility shouldBe true
        }

        test("cannot activate mana ability from tapped land") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.PRECOMBAT_MAIN)
            val (_, stateWithLand) = state.addLandToBattlefield(player1Id, tapped = true)

            val actions = calculator.calculateLegalActions(stateWithLand, player1Id)

            actions.activatableAbilities.shouldBeEmpty()
        }
    }

    context("declaring attackers") {
        test("can declare attacker during declare attackers step on your turn") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)
            // Add creature without summoning sickness
            val (bearId, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, bearDef, tapped = false, summoningSickness = false
            )

            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id)

            actions.declarableAttackers shouldHaveSize 1
            actions.declarableAttackers[0].creatureId shouldBe bearId
            actions.declarableAttackers[0].power shouldBe 2
            actions.declarableAttackers[0].toughness shouldBe 2
        }

        test("cannot declare attacker with summoning sickness") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)
            val (_, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, bearDef, tapped = false, summoningSickness = true
            )

            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id)

            actions.declarableAttackers.shouldBeEmpty()
        }

        test("creature with haste can attack despite summoning sickness") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)
            val (goblinId, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, hasteBearDef, tapped = false, summoningSickness = true
            )

            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id)

            actions.declarableAttackers shouldHaveSize 1
            actions.declarableAttackers[0].creatureId shouldBe goblinId
        }

        test("cannot declare tapped creature as attacker") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)
            val (_, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, bearDef, tapped = true, summoningSickness = false
            )

            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id)

            actions.declarableAttackers.shouldBeEmpty()
        }

        test("creature with defender cannot attack") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)
            val (_, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, defenderWallDef, tapped = false, summoningSickness = false
            )

            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id)

            actions.declarableAttackers.shouldBeEmpty()
        }

        test("cannot declare attackers during wrong step") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.PRECOMBAT_MAIN)
            val (_, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, bearDef, tapped = false, summoningSickness = false
            )

            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id)

            actions.declarableAttackers.shouldBeEmpty()
        }
    }

    context("declaring blockers") {
        test("can declare blocker during declare blockers step when defending") {
            var state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_BLOCKERS)

            // Add attacker for player1
            val (attackerId, stateWithAttacker) = state.addCreatureToBattlefield(
                player1Id, bearDef, tapped = false, summoningSickness = false
            )
            state = stateWithAttacker.updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }

            // Add blocker for player2
            val (blockerId, stateWithBlocker) = state.addCreatureToBattlefield(
                player2Id, bearDef, tapped = false, summoningSickness = false
            )

            // Set up combat with player2 as defender
            val stateWithCombat = stateWithBlocker.copy(
                combat = CombatState(
                    attackingPlayer = player1Id,
                    defendingPlayer = player2Id
                )
            )

            val actions = calculator.calculateLegalActions(stateWithCombat, player2Id)

            actions.declarableBlockers shouldHaveSize 1
            actions.declarableBlockers[0].blockerId shouldBe blockerId
            actions.declarableBlockers[0].attackerId shouldBe attackerId
        }

        test("cannot declare tapped creature as blocker") {
            var state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_BLOCKERS)

            // Add attacker
            val (attackerId, stateWithAttacker) = state.addCreatureToBattlefield(
                player1Id, bearDef, tapped = false, summoningSickness = false
            )
            state = stateWithAttacker.updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }

            // Add tapped blocker
            val (_, stateWithBlocker) = state.addCreatureToBattlefield(
                player2Id, bearDef, tapped = true, summoningSickness = false
            )

            val stateWithCombat = stateWithBlocker.copy(
                combat = CombatState(
                    attackingPlayer = player1Id,
                    defendingPlayer = player2Id
                )
            )

            val actions = calculator.calculateLegalActions(stateWithCombat, player2Id)

            actions.declarableBlockers.shouldBeEmpty()
        }
    }

    context("layer-modified keywords for attacking") {
        beforeTest {
            Modifier.resetTimestamps()
        }

        test("creature gains haste via layer effect can attack with summoning sickness") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)

            // Add creature WITH summoning sickness and no base haste
            val (creatureId, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, bearDef, tapped = false, summoningSickness = true
            )

            // Create a modifier that grants haste
            val hasteModifier = Modifier(
                layer = Layer.ABILITY,
                sourceId = EntityId.of("fervor"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.AddKeyword(Keyword.HASTE),
                filter = ModifierFilter.Specific(creatureId)
            )

            // Create projector with the haste modifier
            val projector = StateProjector(stateWithCreature, listOf(hasteModifier))

            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id, projector = projector)

            // Creature should be able to attack because it has layer-granted haste
            actions.declarableAttackers shouldHaveSize 1
            actions.declarableAttackers[0].creatureId shouldBe creatureId
        }

        test("creature loses haste via layer effect cannot attack with summoning sickness") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)

            // Add creature with summoning sickness and BASE haste
            val (creatureId, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, hasteBearDef, tapped = false, summoningSickness = true
            )

            // Create a modifier that removes haste (e.g., "creatures lose all abilities")
            val removeHasteModifier = Modifier(
                layer = Layer.ABILITY,
                sourceId = EntityId.of("humility"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.RemoveKeyword(Keyword.HASTE),
                filter = ModifierFilter.Specific(creatureId)
            )

            // Create projector with the modifier
            val projector = StateProjector(stateWithCreature, listOf(removeHasteModifier))

            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id, projector = projector)

            // Creature should NOT be able to attack (haste was removed)
            actions.declarableAttackers.shouldBeEmpty()
        }

        test("creature gains defender via layer effect cannot attack") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)

            // Add creature without summoning sickness and no base defender
            val (creatureId, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, bearDef, tapped = false, summoningSickness = false
            )

            // Create a modifier that grants defender
            val defenderModifier = Modifier(
                layer = Layer.ABILITY,
                sourceId = EntityId.of("some_effect"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.AddKeyword(Keyword.DEFENDER),
                filter = ModifierFilter.Specific(creatureId)
            )

            val projector = StateProjector(stateWithCreature, listOf(defenderModifier))

            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id, projector = projector)

            // Creature cannot attack because it gained defender
            actions.declarableAttackers.shouldBeEmpty()
        }

        test("creature loses defender via layer effect can attack") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)

            // Add wall with defender, no summoning sickness
            val (creatureId, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, defenderWallDef, tapped = false, summoningSickness = false
            )

            // Create a modifier that removes defender (e.g., Rolling Stones)
            val removeDefenderModifier = Modifier(
                layer = Layer.ABILITY,
                sourceId = EntityId.of("rolling_stones"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.RemoveKeyword(Keyword.DEFENDER),
                filter = ModifierFilter.Specific(creatureId)
            )

            val projector = StateProjector(stateWithCreature, listOf(removeDefenderModifier))

            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id, projector = projector)

            // Wall can now attack because defender was removed
            actions.declarableAttackers shouldHaveSize 1
            actions.declarableAttackers[0].creatureId shouldBe creatureId
        }
    }

    context("layer-modified P/T values") {
        beforeTest {
            Modifier.resetTimestamps()
        }

        test("attacker info shows layer-modified P/T") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)

            // Add 2/2 bear without summoning sickness
            val (creatureId, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, bearDef, tapped = false, summoningSickness = false
            )

            // Create a +3/+3 modifier (like Giant Growth)
            val pumpModifier = Modifier(
                layer = Layer.PT_MODIFY,
                sourceId = EntityId.of("giant_growth"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ModifyPT(3, 3),
                filter = ModifierFilter.Specific(creatureId)
            )

            val projector = StateProjector(stateWithCreature, listOf(pumpModifier))

            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id, projector = projector)

            actions.declarableAttackers shouldHaveSize 1
            // P/T should reflect the pump effect: 2+3 = 5
            actions.declarableAttackers[0].power shouldBe 5
            actions.declarableAttackers[0].toughness shouldBe 5
        }

        test("attacker info shows P/T with negative modifier") {
            val state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_ATTACKERS)

            // Add 2/2 bear without summoning sickness
            val (creatureId, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, bearDef, tapped = false, summoningSickness = false
            )

            // Create a -1/-1 modifier
            val weakenModifier = Modifier(
                layer = Layer.PT_MODIFY,
                sourceId = EntityId.of("weakness"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ModifyPT(-1, -1),
                filter = ModifierFilter.Specific(creatureId)
            )

            val projector = StateProjector(stateWithCreature, listOf(weakenModifier))

            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id, projector = projector)

            actions.declarableAttackers shouldHaveSize 1
            // P/T should reflect the weaken effect: 2-1 = 1
            actions.declarableAttackers[0].power shouldBe 1
            actions.declarableAttackers[0].toughness shouldBe 1
        }
    }

    context("layer-modified blocking with flying/reach") {
        val flyingDef = CardDefinition.creature(
            name = "Air Elemental",
            manaCost = ManaCost.parse("{3}{U}{U}"),
            subtypes = setOf(Subtype.ELEMENTAL),
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.FLYING)
        )

        beforeTest {
            Modifier.resetTimestamps()
        }

        test("creature without flying/reach cannot block creature with flying") {
            var state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_BLOCKERS)

            // Add flying attacker for player1
            val (attackerId, stateWithAttacker) = state.addCreatureToBattlefield(
                player1Id, flyingDef, tapped = false, summoningSickness = false
            )
            state = stateWithAttacker.updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }

            // Add non-flying blocker for player2
            val (blockerId, stateWithBlocker) = state.addCreatureToBattlefield(
                player2Id, bearDef, tapped = false, summoningSickness = false
            )

            val stateWithCombat = stateWithBlocker.copy(
                combat = CombatState(
                    attackingPlayer = player1Id,
                    defendingPlayer = player2Id
                )
            )

            val projector = StateProjector(stateWithCombat)
            val actions = calculator.calculateLegalActions(stateWithCombat, player2Id, projector = projector)

            // Non-flying creature cannot block flying creature
            actions.declarableBlockers.shouldBeEmpty()
        }

        test("creature gaining flying via layer can block flying attackers") {
            var state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_BLOCKERS)

            // Add flying attacker for player1
            val (attackerId, stateWithAttacker) = state.addCreatureToBattlefield(
                player1Id, flyingDef, tapped = false, summoningSickness = false
            )
            state = stateWithAttacker.updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }

            // Add non-flying blocker for player2
            val (blockerId, stateWithBlocker) = state.addCreatureToBattlefield(
                player2Id, bearDef, tapped = false, summoningSickness = false
            )

            val stateWithCombat = stateWithBlocker.copy(
                combat = CombatState(
                    attackingPlayer = player1Id,
                    defendingPlayer = player2Id
                )
            )

            // Grant flying to the blocker
            val flyingModifier = Modifier(
                layer = Layer.ABILITY,
                sourceId = EntityId.of("levitation"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.AddKeyword(Keyword.FLYING),
                filter = ModifierFilter.Specific(blockerId)
            )

            val projector = StateProjector(stateWithCombat, listOf(flyingModifier))
            val actions = calculator.calculateLegalActions(stateWithCombat, player2Id, projector = projector)

            // Creature with layer-granted flying can block the flyer
            actions.declarableBlockers shouldHaveSize 1
            actions.declarableBlockers[0].blockerId shouldBe blockerId
        }

        test("creature gaining reach via layer can block flying attackers") {
            var state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_BLOCKERS)

            // Add flying attacker for player1
            val (attackerId, stateWithAttacker) = state.addCreatureToBattlefield(
                player1Id, flyingDef, tapped = false, summoningSickness = false
            )
            state = stateWithAttacker.updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }

            // Add non-flying, non-reach blocker for player2
            val (blockerId, stateWithBlocker) = state.addCreatureToBattlefield(
                player2Id, bearDef, tapped = false, summoningSickness = false
            )

            val stateWithCombat = stateWithBlocker.copy(
                combat = CombatState(
                    attackingPlayer = player1Id,
                    defendingPlayer = player2Id
                )
            )

            // Grant reach to the blocker
            val reachModifier = Modifier(
                layer = Layer.ABILITY,
                sourceId = EntityId.of("spider_climb"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.AddKeyword(Keyword.REACH),
                filter = ModifierFilter.Specific(blockerId)
            )

            val projector = StateProjector(stateWithCombat, listOf(reachModifier))
            val actions = calculator.calculateLegalActions(stateWithCombat, player2Id, projector = projector)

            // Creature with layer-granted reach can block the flyer
            actions.declarableBlockers shouldHaveSize 1
            actions.declarableBlockers[0].blockerId shouldBe blockerId
        }

        test("flying creature losing flying via layer cannot block flying attackers when it has no reach") {
            var state = newGame()
                .withActivePlayer(player1Id)
                .withStep(Step.DECLARE_BLOCKERS)

            // Add flying attacker for player1
            val (attackerId, stateWithAttacker) = state.addCreatureToBattlefield(
                player1Id, flyingDef, tapped = false, summoningSickness = false
            )
            state = stateWithAttacker.updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }

            // Add flying blocker for player2 (will lose flying)
            val defenderFlyingDef = CardDefinition.creature(
                name = "Flying Defender",
                manaCost = ManaCost.parse("{2}{U}"),
                subtypes = setOf(Subtype.ELEMENTAL),
                power = 1,
                toughness = 4,
                keywords = setOf(Keyword.FLYING)
            )
            val (blockerId, stateWithBlocker) = state.addCreatureToBattlefield(
                player2Id, defenderFlyingDef, tapped = false, summoningSickness = false
            )

            val stateWithCombat = stateWithBlocker.copy(
                combat = CombatState(
                    attackingPlayer = player1Id,
                    defendingPlayer = player2Id
                )
            )

            // Remove flying from the blocker
            val removeFlyingModifier = Modifier(
                layer = Layer.ABILITY,
                sourceId = EntityId.of("gravity_well"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.RemoveKeyword(Keyword.FLYING),
                filter = ModifierFilter.Specific(blockerId)
            )

            val projector = StateProjector(stateWithCombat, listOf(removeFlyingModifier))
            val actions = calculator.calculateLegalActions(stateWithCombat, player2Id, projector = projector)

            // The blocker lost flying and has no reach, so it cannot block the flyer
            actions.declarableBlockers.shouldBeEmpty()
        }
    }

    context("layer-modified controller affects who can attack") {
        beforeTest {
            Modifier.resetTimestamps()
        }

        test("creature with changed controller appears in new controller's attackers") {
            val state = newGame()
                .withActivePlayer(player2Id)  // Player 2's turn (will try to attack)
                .withStep(Step.DECLARE_ATTACKERS)

            // Add creature OWNED by player1 but we'll change control to player2
            val (creatureId, state1) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id),  // Owned by player1
                ControllerComponent(player1Id)       // Initially controlled by player1
            )
            val stateWithCreature = state1.addToZone(creatureId, ZoneId.BATTLEFIELD)

            // Create a control change modifier (e.g., Control Magic)
            val controlModifier = Modifier(
                layer = Layer.CONTROL,
                sourceId = EntityId.of("control_magic"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ChangeControl(player2Id),
                filter = ModifierFilter.Specific(creatureId)
            )

            val projector = StateProjector(stateWithCreature, listOf(controlModifier))

            // Player 2 should be able to attack with the stolen creature
            val actions = calculator.calculateLegalActions(stateWithCreature, player2Id, projector = projector)

            actions.declarableAttackers shouldHaveSize 1
            actions.declarableAttackers[0].creatureId shouldBe creatureId
        }

        test("original owner cannot attack with creature whose control was changed") {
            val state = newGame()
                .withActivePlayer(player1Id)  // Player 1's turn
                .withStep(Step.DECLARE_ATTACKERS)

            // Add creature owned and initially controlled by player1
            val (creatureId, stateWithCreature) = state.addCreatureToBattlefield(
                player1Id, bearDef, tapped = false, summoningSickness = false
            )

            // Create a control change modifier giving control to player2
            val controlModifier = Modifier(
                layer = Layer.CONTROL,
                sourceId = EntityId.of("control_magic"),
                timestamp = Modifier.nextTimestamp(),
                modification = Modification.ChangeControl(player2Id),
                filter = ModifierFilter.Specific(creatureId)
            )

            val projector = StateProjector(stateWithCreature, listOf(controlModifier))

            // Player 1 should NOT be able to attack with the creature they no longer control
            val actions = calculator.calculateLegalActions(stateWithCreature, player1Id, projector = projector)

            actions.declarableAttackers.shouldBeEmpty()
        }
    }
})
