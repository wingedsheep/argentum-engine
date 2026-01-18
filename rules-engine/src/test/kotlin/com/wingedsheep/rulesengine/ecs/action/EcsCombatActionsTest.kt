package com.wingedsheep.rulesengine.ecs.action

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EcsGameEngine
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.game.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class EcsCombatActionsTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    val vigilanceCreatureDef = CardDefinition.creature(
        name = "Serra Angel",
        manaCost = ManaCost.parse("{3}{W}{W}"),
        subtypes = setOf(Subtype(value = "Angel")),
        power = 4,
        toughness = 4,
        keywords = setOf(Keyword.VIGILANCE, Keyword.FLYING)
    )

    val trampleCreatureDef = CardDefinition.creature(
        name = "Craw Wurm",
        manaCost = ManaCost.parse("{4}{G}{G}"),
        subtypes = setOf(Subtype(value = "Wurm")),
        power = 6,
        toughness = 4,
        keywords = setOf(Keyword.TRAMPLE)
    )

    val giantDef = CardDefinition.creature(
        name = "Hill Giant",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype(value = "Giant")),
        power = 3,
        toughness = 3
    )

    fun newGame(): EcsGameState = EcsGameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun EcsGameState.addCreatureToBattlefield(
        def: CardDefinition,
        controllerId: EntityId,
        hasSummoningSickness: Boolean = true
    ): Pair<EntityId, EcsGameState> {
        val components = mutableListOf<Component>(
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        if (hasSummoningSickness) {
            components.add(SummoningSicknessComponent)
        }

        val (creatureId, state1) = createEntity(
            EntityId.generate(),
            components
        )
        return creatureId to state1.addToZone(creatureId, ZoneId.BATTLEFIELD)
    }

    fun createGameInDeclareAttackersStep(): EcsGameState {
        val state = newGame()
        return state
            .advanceToStep(Step.DECLARE_ATTACKERS)
            .startCombat(player2Id)
    }

    fun createGameInDeclareBlockersStep(): EcsGameState {
        return createGameInDeclareAttackersStep()
            .advanceToStep(Step.DECLARE_BLOCKERS)
    }

    val handler = EcsActionHandler()

    context("BeginCombat action") {
        test("initializes combat state") {
            val state = newGame().advanceToStep(Step.BEGIN_COMBAT)

            state.combat.shouldBeNull()

            val action = EcsBeginCombat(player1Id, player2Id)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.combat.shouldNotBeNull()
            success.state.combat!!.attackingPlayer.value shouldBe player1Id.value
            success.state.combat!!.defendingPlayer.value shouldBe player2Id.value
        }
    }

    context("DeclareAttacker action") {
        test("declares creature as attacker") {
            var state = createGameInDeclareAttackersStep()
            val (bearId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false)
            state = state1

            val action = EcsDeclareAttacker(bearId, player1Id)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.hasComponent<AttackingComponent>(bearId).shouldBeTrue()
        }

        test("taps creature when declaring as attacker") {
            var state = createGameInDeclareAttackersStep()
            val (bearId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false)
            state = state1

            val action = EcsDeclareAttacker(bearId, player1Id)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.hasComponent<TappedComponent>(bearId).shouldBeTrue()
        }

        test("fails for creature with summoning sickness") {
            var state = createGameInDeclareAttackersStep()
            val (bearId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = true)
            state = state1

            val action = EcsDeclareAttacker(bearId, player1Id)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Failure>()
        }

        test("fails for tapped creature") {
            var state = createGameInDeclareAttackersStep()
            val (bearId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false)
            state = state1.updateEntity(bearId) { it.with(TappedComponent) }

            val action = EcsDeclareAttacker(bearId, player1Id)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Failure>()
        }
    }

    context("DeclareBlocker action") {
        test("declares creature as blocker") {
            var state = createGameInDeclareBlockersStep()
            val (attackerId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false)
            val (blockerId, state2) = state1.addCreatureToBattlefield(bearDef, player2Id, hasSummoningSickness = false)

            // Add attacker to combat
            state = state2.updateEntity(attackerId) {
                it.with(AttackingComponent.attackingPlayer(player2Id))
            }

            val action = EcsDeclareBlocker(blockerId, attackerId, player2Id)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.hasComponent<BlockingComponent>(blockerId).shouldBeTrue()
        }

        test("fails for tapped creature") {
            var state = createGameInDeclareBlockersStep()
            val (attackerId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false)
            val (blockerId, state2) = state1.addCreatureToBattlefield(bearDef, player2Id, hasSummoningSickness = false)

            // Tap the blocker
            state = state2.updateEntity(blockerId) { it.with(TappedComponent) }
            state = state.updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }

            val action = EcsDeclareBlocker(blockerId, attackerId, player2Id)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Failure>()
        }
    }

    context("EndCombat action") {
        test("clears combat state") {
            var state = createGameInDeclareBlockersStep()
            val (attackerId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false)
            state = state1.updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }

            state.combat.shouldNotBeNull()

            val action = EcsEndCombat(player1Id)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.combat.shouldBeNull()
        }

        test("removes AttackingComponent from attackers") {
            var state = createGameInDeclareBlockersStep()
            val (attackerId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false)
            state = state1.updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }

            val action = EcsEndCombat(player1Id)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.hasComponent<AttackingComponent>(attackerId).shouldBeFalse()
        }

        test("removes BlockingComponent from blockers") {
            var state = createGameInDeclareBlockersStep()
            val (attackerId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false)
            val (blockerId, state2) = state1.addCreatureToBattlefield(bearDef, player2Id, hasSummoningSickness = false)
            state = state2
                .updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }
                .updateEntity(blockerId) { it.with(BlockingComponent(attackerId)) }

            val action = EcsEndCombat(player1Id)
            val result = handler.execute(state, action)

            result.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = result as EcsActionResult.Success
            success.state.hasComponent<BlockingComponent>(blockerId).shouldBeFalse()
        }
    }

    context("Combat Damage") {
        test("unblocked attacker deals damage to defending player") {
            var state = createGameInDeclareBlockersStep()
            val (attackerId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false)
            state = state1.updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }

            val initialLife = state.getComponent<LifeComponent>(player2Id)!!.life

            // Simulate combat damage via action
            val damageResult = handler.execute(state, EcsDealDamageToPlayer(player2Id, 2, attackerId))

            damageResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = damageResult as EcsActionResult.Success
            success.state.getComponent<LifeComponent>(player2Id)!!.life shouldBe initialLife - 2
        }

        test("attacker deals damage to blocker") {
            var state = createGameInDeclareBlockersStep()
            val (attackerId, state1) = state.addCreatureToBattlefield(giantDef, player1Id, hasSummoningSickness = false) // 3/3
            val (blockerId, state2) = state1.addCreatureToBattlefield(bearDef, player2Id, hasSummoningSickness = false) // 2/2
            state = state2
                .updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }
                .updateEntity(blockerId) { it.with(BlockingComponent(attackerId)) }

            // Simulate combat damage
            val damageResult = handler.execute(state, EcsDealDamageToCreature(blockerId, 3, attackerId))

            damageResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = damageResult as EcsActionResult.Success
            success.state.getComponent<DamageComponent>(blockerId)!!.amount shouldBe 3
        }

        test("blocker deals damage to attacker") {
            var state = createGameInDeclareBlockersStep()
            val (attackerId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false) // 2/2
            val (blockerId, state2) = state1.addCreatureToBattlefield(giantDef, player2Id, hasSummoningSickness = false) // 3/3
            state = state2
                .updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }
                .updateEntity(blockerId) { it.with(BlockingComponent(attackerId)) }

            // Simulate combat damage
            val damageResult = handler.execute(state, EcsDealDamageToCreature(attackerId, 3, blockerId))

            damageResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = damageResult as EcsActionResult.Success
            success.state.getComponent<DamageComponent>(attackerId)!!.amount shouldBe 3
        }

        test("creature with lethal damage dies from state-based actions") {
            var state = createGameInDeclareBlockersStep()
            val (creatureId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false) // 2/2

            // Deal lethal damage
            state = state1.updateEntity(creatureId) { it.with(DamageComponent(2)) }

            // Check state-based actions
            val sbaResult = handler.execute(state, EcsCheckStateBasedActions())

            sbaResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = sbaResult as EcsActionResult.Success

            success.state.getBattlefield().contains(creatureId).shouldBeFalse()
            success.state.getGraveyard(player1Id).contains(creatureId).shouldBeTrue()
        }
    }

    context("State-Based Actions in Combat") {
        test("player with 0 life loses the game") {
            var state = newGame()
            state = state.updateEntity(player2Id) { it.with(LifeComponent(0)) }

            val sbaResult = handler.execute(state, EcsCheckStateBasedActions())

            sbaResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = sbaResult as EcsActionResult.Success
            success.state.hasComponent<LostGameComponent>(player2Id).shouldBeTrue()
        }

        test("player with negative life loses the game") {
            var state = newGame()
            state = state.updateEntity(player2Id) { it.with(LifeComponent(-5)) }

            val sbaResult = handler.execute(state, EcsCheckStateBasedActions())

            sbaResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = sbaResult as EcsActionResult.Success
            success.state.hasComponent<LostGameComponent>(player2Id).shouldBeTrue()
        }

        test("player with 10 poison counters loses the game") {
            var state = newGame()
            state = state.updateEntity(player2Id) { it.with(PoisonComponent(10)) }

            val sbaResult = handler.execute(state, EcsCheckStateBasedActions())

            sbaResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = sbaResult as EcsActionResult.Success
            success.state.hasComponent<LostGameComponent>(player2Id).shouldBeTrue()
        }

        test("player with 9 poison counters does not lose") {
            var state = newGame()
            state = state.updateEntity(player2Id) { it.with(PoisonComponent(9)) }

            val sbaResult = handler.execute(state, EcsCheckStateBasedActions())

            sbaResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = sbaResult as EcsActionResult.Success
            success.state.hasComponent<LostGameComponent>(player2Id).shouldBeFalse()
        }

        test("game ends when only one player remains") {
            var state = newGame()
            state = state.updateEntity(player2Id) { it.with(LifeComponent(0)) }

            val sbaResult = handler.execute(state, EcsCheckStateBasedActions())

            sbaResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = sbaResult as EcsActionResult.Success
            success.state.isGameOver.shouldBeTrue()
            success.state.winner shouldBe player1Id
        }

        test("generates CreatureDied event when creature dies") {
            var state = newGame()
            val (bearId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false)
            state = state1.updateEntity(bearId) { it.with(DamageComponent(2)) }

            val sbaResult = handler.execute(state, EcsCheckStateBasedActions())

            sbaResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = sbaResult as EcsActionResult.Success
            success.events.any { it is EcsActionEvent.CreatureDied }.shouldBeTrue()
        }

        test("generates PlayerLost event when player loses") {
            var state = newGame()
            state = state.updateEntity(player2Id) { it.with(LifeComponent(0)) }

            val sbaResult = handler.execute(state, EcsCheckStateBasedActions())

            sbaResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = sbaResult as EcsActionResult.Success
            success.events.any { it is EcsActionEvent.PlayerLost }.shouldBeTrue()
        }

        test("generates GameEnded event when game ends") {
            var state = newGame()
            state = state.updateEntity(player2Id) { it.with(LifeComponent(0)) }

            val sbaResult = handler.execute(state, EcsCheckStateBasedActions())

            sbaResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = sbaResult as EcsActionResult.Success
            success.events.any { it is EcsActionEvent.GameEnded }.shouldBeTrue()
        }

        test("handles multiple creatures dying simultaneously") {
            var state = newGame()
            val (bear1Id, state1) = state.addCreatureToBattlefield(bearDef, player1Id, hasSummoningSickness = false)
            val (bear2Id, state2) = state1.addCreatureToBattlefield(bearDef, player2Id, hasSummoningSickness = false)
            state = state2
                .updateEntity(bear1Id) { it.with(DamageComponent(2)) }
                .updateEntity(bear2Id) { it.with(DamageComponent(3)) }

            val sbaResult = handler.execute(state, EcsCheckStateBasedActions())

            sbaResult.shouldBeInstanceOf<EcsActionResult.Success>()
            val success = sbaResult as EcsActionResult.Success
            success.state.getBattlefield().contains(bear1Id).shouldBeFalse()
            success.state.getBattlefield().contains(bear2Id).shouldBeFalse()
            success.state.getGraveyard(player1Id).contains(bear1Id).shouldBeTrue()
            success.state.getGraveyard(player2Id).contains(bear2Id).shouldBeTrue()
        }
    }

    context("Full Combat Flow") {
        test("complete combat: attack, block, damage, death") {
            // Setup game
            var state = createGameInDeclareAttackersStep()
            val (attackerId, state1) = state.addCreatureToBattlefield(giantDef, player1Id, hasSummoningSickness = false) // 3/3
            val (blockerId, state2) = state1.addCreatureToBattlefield(bearDef, player2Id, hasSummoningSickness = false) // 2/2
            state = state2

            // Declare attacker
            val declareResult = handler.execute(state, EcsDeclareAttacker(attackerId, player1Id))
            declareResult.shouldBeInstanceOf<EcsActionResult.Success>()
            state = (declareResult as EcsActionResult.Success).state.advanceToStep(Step.DECLARE_BLOCKERS)

            // Declare blocker
            val blockResult = handler.execute(state, EcsDeclareBlocker(blockerId, attackerId, player2Id))
            blockResult.shouldBeInstanceOf<EcsActionResult.Success>()
            state = (blockResult as EcsActionResult.Success).state.advanceToStep(Step.COMBAT_DAMAGE)

            // Deal combat damage (attacker 3 to blocker, blocker 2 to attacker)
            val damageResult1 = handler.execute(state, EcsDealDamageToCreature(blockerId, 3, attackerId))
            state = (damageResult1 as EcsActionResult.Success).state
            val damageResult2 = handler.execute(state, EcsDealDamageToCreature(attackerId, 2, blockerId))
            state = (damageResult2 as EcsActionResult.Success).state

            // Attacker took 2 damage, blocker took 3 damage
            state.getComponent<DamageComponent>(attackerId)!!.amount shouldBe 2
            state.getComponent<DamageComponent>(blockerId)!!.amount shouldBe 3

            // Check state-based actions - blocker should die (3 damage >= 2 toughness)
            val sbaResult = handler.execute(state, EcsCheckStateBasedActions())
            state = (sbaResult as EcsActionResult.Success).state

            // Blocker is dead
            state.getBattlefield().contains(blockerId).shouldBeFalse()
            state.getGraveyard(player2Id).contains(blockerId).shouldBeTrue()

            // Attacker survives (2 damage < 3 toughness)
            state.getBattlefield().contains(attackerId).shouldBeTrue()

            // End combat
            val endResult = handler.execute(state, EcsEndCombat(player1Id))
            endResult.shouldBeInstanceOf<EcsActionResult.Success>()
            (endResult as EcsActionResult.Success).state.combat.shouldBeNull()
        }

        test("unblocked attacker deals damage to player") {
            var state = createGameInDeclareAttackersStep()
            val (attackerId, state1) = state.addCreatureToBattlefield(giantDef, player1Id, hasSummoningSickness = false) // 3/3
            state = state1

            val initialLife = state.getComponent<LifeComponent>(player2Id)!!.life

            // Declare attacker
            state = (handler.execute(state, EcsDeclareAttacker(attackerId, player1Id)) as EcsActionResult.Success).state
                .advanceToStep(Step.DECLARE_BLOCKERS)
                .advanceToStep(Step.COMBAT_DAMAGE)

            // Deal combat damage to player (no blockers)
            state = (handler.execute(state, EcsDealDamageToPlayer(player2Id, 3, attackerId)) as EcsActionResult.Success).state

            state.getComponent<LifeComponent>(player2Id)!!.life shouldBe initialLife - 3

            // End combat
            state = (handler.execute(state, EcsEndCombat(player1Id)) as EcsActionResult.Success).state
            state.combat.shouldBeNull()
        }

        test("lethal damage to player ends game") {
            var state = createGameInDeclareAttackersStep()
            val (attackerId, state1) = state.addCreatureToBattlefield(giantDef, player1Id, hasSummoningSickness = false) // 3/3

            // Set player2 to exactly lethal
            state = state1.updateEntity(player2Id) { it.with(LifeComponent(3)) }

            // Declare attacker and deal damage
            state = (handler.execute(state, EcsDeclareAttacker(attackerId, player1Id)) as EcsActionResult.Success).state
                .advanceToStep(Step.DECLARE_BLOCKERS)
                .advanceToStep(Step.COMBAT_DAMAGE)
            state = (handler.execute(state, EcsDealDamageToPlayer(player2Id, 3, attackerId)) as EcsActionResult.Success).state

            state.getComponent<LifeComponent>(player2Id)!!.life shouldBe 0

            // Check state-based actions - player should lose
            state = (handler.execute(state, EcsCheckStateBasedActions()) as EcsActionResult.Success).state

            state.isGameOver.shouldBeTrue()
            state.winner shouldBe player1Id
        }
    }
})

// Extension to advance game to a specific step
private fun EcsGameState.advanceToStep(step: Step): EcsGameState {
    return copy(turnState = turnState.copy(step = step))
}
