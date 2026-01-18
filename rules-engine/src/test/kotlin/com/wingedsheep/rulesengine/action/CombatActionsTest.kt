package com.wingedsheep.rulesengine.action

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.game.Step
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class CombatActionsTest : FunSpec({

    val player1Id = PlayerId.of("player1")
    val player2Id = PlayerId.of("player2")

    fun createPlayer1() = Player.create(player1Id, "Alice")
    fun createPlayer2() = Player.create(player2Id, "Bob")

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

    fun createGameInDeclareAttackersStep(): GameState {
        val state = GameState.newGame(createPlayer1(), createPlayer2())
        return state
            .advanceToStep(Step.DECLARE_ATTACKERS)
            .startCombat(player2Id)
    }

    fun createGameInDeclareBlockersStep(): GameState {
        return createGameInDeclareAttackersStep()
            .advanceToStep(Step.DECLARE_BLOCKERS)
    }

    context("BeginCombat action") {
        test("initializes combat state") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.BEGIN_COMBAT)

            state.combat.shouldBeNull()

            val action = BeginCombat(player1Id, player2Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.combat.shouldNotBeNull()
            result.state.combat!!.attackingPlayer shouldBe player1Id
            result.state.combat!!.defendingPlayer shouldBe player2Id
        }

        test("fails when already in combat") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.BEGIN_COMBAT)
                .startCombat(player2Id)

            val action = BeginCombat(player1Id, player2Id)
            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
            (result as ActionResult.Failure).reason shouldBe "Already in combat"
        }
    }

    context("DeclareAttacker action") {
        test("declares creature as attacker") {
            var state = createGameInDeclareAttackersStep()
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(bear) }

            val action = DeclareAttacker(bear.id, player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.combat!!.isAttacking(bear.id).shouldBeTrue()
        }

        test("taps creature when declaring as attacker") {
            var state = createGameInDeclareAttackersStep()
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(bear) }

            val action = DeclareAttacker(bear.id, player1Id)
            val result = ActionExecutor.execute(state, action)

            result.state.battlefield.getCard(bear.id)!!.isTapped.shouldBeTrue()
        }

        test("vigilance creature does not tap when attacking") {
            var state = createGameInDeclareAttackersStep()
            val angel = CardInstance.create(vigilanceCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(angel) }

            val action = DeclareAttacker(angel.id, player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.combat!!.isAttacking(angel.id).shouldBeTrue()
            result.state.battlefield.getCard(angel.id)!!.isTapped.shouldBeFalse()
        }

        test("generates CardTapped event for non-vigilance attacker") {
            var state = createGameInDeclareAttackersStep()
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(bear) }

            val action = DeclareAttacker(bear.id, player1Id)
            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events.any { it is GameEvent.CardTapped && it.cardId == bear.id.value }.shouldBeTrue()
        }

        test("does not generate CardTapped event for vigilance attacker") {
            var state = createGameInDeclareAttackersStep()
            val angel = CardInstance.create(vigilanceCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(angel) }

            val action = DeclareAttacker(angel.id, player1Id)
            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events.none { it is GameEvent.CardTapped }.shouldBeTrue()
        }

        test("fails for invalid attacker") {
            var state = createGameInDeclareAttackersStep()
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = true) // Has summoning sickness
            state = state.updateBattlefield { it.addToTop(bear) }

            val action = DeclareAttacker(bear.id, player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
        }
    }

    context("DeclareBlocker action") {
        test("declares creature as blocker") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker) }
                .updateCombat { it.addAttacker(attacker.id) }

            val action = DeclareBlocker(blocker.id, attacker.id, player2Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.combat!!.isBlocking(blocker.id).shouldBeTrue()
            result.state.combat!!.getBlockersFor(attacker.id).contains(blocker.id).shouldBeTrue()
        }

        test("multiple creatures can block same attacker") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker1 = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            val blocker2 = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker1).addToTop(blocker2) }
                .updateCombat { it.addAttacker(attacker.id) }

            val result1 = ActionExecutor.execute(state, DeclareBlocker(blocker1.id, attacker.id, player2Id))
            val result2 = ActionExecutor.execute(result1.state, DeclareBlocker(blocker2.id, attacker.id, player2Id))

            result2.isSuccess.shouldBeTrue()
            result2.state.combat!!.getBlockersFor(attacker.id).size shouldBe 2
        }

        test("fails for invalid blocker") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false, isTapped = true) // Tapped
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker) }
                .updateCombat { it.addAttacker(attacker.id) }

            val action = DeclareBlocker(blocker.id, attacker.id, player2Id)
            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
        }
    }

    context("SetDamageAssignmentOrder action") {
        test("sets damage assignment order for attacker") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(trampleCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker1 = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            val blocker2 = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker1).addToTop(blocker2) }
                .updateCombat {
                    it.addAttacker(attacker.id)
                        .addBlocker(blocker1.id, attacker.id)
                        .addBlocker(blocker2.id, attacker.id)
                }

            val action = SetDamageAssignmentOrder(attacker.id, listOf(blocker2.id, blocker1.id))
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.combat!!.damageAssignmentOrder[attacker.id] shouldBe listOf(blocker2.id, blocker1.id)
        }
    }

    context("ResolveCombatDamage action") {
        test("unblocked attacker deals damage to defending player") {
            var state = createGameInDeclareBlockersStep()
                .advanceToStep(Step.COMBAT_DAMAGE)
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker) }
                .updateCombat { it.addAttacker(attacker.id) }

            val initialLife = state.getPlayer(player2Id).life

            val action = ResolveCombatDamage()
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player2Id).life shouldBe initialLife - 2
        }

        test("blocked attacker deals damage to blocker") {
            var state = createGameInDeclareBlockersStep()
                .advanceToStep(Step.COMBAT_DAMAGE)
            val attacker = CardInstance.create(giantDef, player1Id.value) // 3/3
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value) // 2/2
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker) }
                .updateCombat { it.addAttacker(attacker.id).addBlocker(blocker.id, attacker.id) }

            val action = ResolveCombatDamage()
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.getCard(blocker.id)!!.damageMarked shouldBe 3
        }

        test("blocker deals damage back to attacker") {
            var state = createGameInDeclareBlockersStep()
                .advanceToStep(Step.COMBAT_DAMAGE)
            val attacker = CardInstance.create(bearDef, player1Id.value) // 2/2
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(giantDef, player2Id.value) // 3/3
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker) }
                .updateCombat { it.addAttacker(attacker.id).addBlocker(blocker.id, attacker.id) }

            val action = ResolveCombatDamage()
            val result = ActionExecutor.execute(state, action)

            result.state.battlefield.getCard(attacker.id)!!.damageMarked shouldBe 3
        }

        test("trample damage goes through to player") {
            var state = createGameInDeclareBlockersStep()
                .advanceToStep(Step.COMBAT_DAMAGE)
            val trampler = CardInstance.create(trampleCreatureDef, player1Id.value) // 6/4
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value) // 2/2
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(trampler).addToTop(blocker) }
                .updateCombat { it.addAttacker(trampler.id).addBlocker(blocker.id, trampler.id) }

            val initialLife = state.getPlayer(player2Id).life

            val action = ResolveCombatDamage()
            val result = ActionExecutor.execute(state, action)

            // Blocker takes 2 damage (lethal), 4 tramples through
            result.state.battlefield.getCard(blocker.id)!!.damageMarked shouldBe 2
            result.state.getPlayer(player2Id).life shouldBe initialLife - 4
        }

        test("resolves damage for specific attacker only") {
            var state = createGameInDeclareBlockersStep()
                .advanceToStep(Step.COMBAT_DAMAGE)
            val attacker1 = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val attacker2 = CardInstance.create(giantDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker1).addToTop(attacker2) }
                .updateCombat { it.addAttacker(attacker1.id).addAttacker(attacker2.id) }

            val initialLife = state.getPlayer(player2Id).life

            // Only resolve attacker1's damage
            val action = ResolveCombatDamage(attacker1.id)
            val result = ActionExecutor.execute(state, action)

            // Should only deal bear's 2 damage, not giant's 3
            result.state.getPlayer(player2Id).life shouldBe initialLife - 2
        }

        test("generates DamageDealt events") {
            var state = createGameInDeclareBlockersStep()
                .advanceToStep(Step.COMBAT_DAMAGE)
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker) }
                .updateCombat { it.addAttacker(attacker.id) }

            val action = ResolveCombatDamage()
            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events.any { it is GameEvent.DamageDealt }.shouldBeTrue()
        }

        test("fails when not in combat") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.PRECOMBAT_MAIN)

            val action = ResolveCombatDamage()
            val result = ActionExecutor.execute(state, action)

            result.isFailure.shouldBeTrue()
            (result as ActionResult.Failure).reason shouldBe "Not in combat"
        }
    }

    context("EndCombat action") {
        test("clears combat state") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker) }
                .updateCombat { it.addAttacker(attacker.id) }

            state.combat.shouldNotBeNull()

            val action = EndCombat(player1Id)
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.combat.shouldBeNull()
        }
    }

    context("CheckStateBasedActions action") {
        test("creature with lethal damage dies") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.COMBAT_DAMAGE)
            val bear = CardInstance.create(bearDef, player1Id.value) // 2/2
                .copy(controllerId = player1Id.value, summoningSickness = false)
                .dealDamage(2)
            state = state.updateBattlefield { it.addToTop(bear) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.battlefield.contains(bear.id).shouldBeFalse()
            result.state.getPlayer(player1Id).graveyard.contains(bear.id).shouldBeTrue()
        }

        test("creature with more damage than toughness dies") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.COMBAT_DAMAGE)
            val bear = CardInstance.create(bearDef, player1Id.value) // 2/2
                .copy(controllerId = player1Id.value, summoningSickness = false)
                .dealDamage(5) // Overkill
            state = state.updateBattlefield { it.addToTop(bear) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action)

            result.state.battlefield.contains(bear.id).shouldBeFalse()
            result.state.getPlayer(player1Id).graveyard.contains(bear.id).shouldBeTrue()
        }

        test("creature with less damage than toughness survives") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.COMBAT_DAMAGE)
            val bear = CardInstance.create(bearDef, player1Id.value) // 2/2
                .copy(controllerId = player1Id.value, summoningSickness = false)
                .dealDamage(1)
            state = state.updateBattlefield { it.addToTop(bear) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action)

            result.state.battlefield.contains(bear.id).shouldBeTrue()
        }

        test("player with 0 life loses the game") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.PRECOMBAT_MAIN)
            state = state.updatePlayer(player2Id) { it.setLife(0) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action)

            result.isSuccess.shouldBeTrue()
            result.state.getPlayer(player2Id).hasLost.shouldBeTrue()
        }

        test("player with negative life loses the game") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.PRECOMBAT_MAIN)
            state = state.updatePlayer(player2Id) { it.setLife(-5) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action)

            result.state.getPlayer(player2Id).hasLost.shouldBeTrue()
        }

        test("player with 10 poison counters loses the game") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.PRECOMBAT_MAIN)
            state = state.updatePlayer(player2Id) { it.addPoisonCounters(10) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action)

            result.state.getPlayer(player2Id).hasLost.shouldBeTrue()
        }

        test("player with 9 poison counters does not lose") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.PRECOMBAT_MAIN)
            state = state.updatePlayer(player2Id) { it.addPoisonCounters(9) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action)

            result.state.getPlayer(player2Id).hasLost.shouldBeFalse()
        }

        test("game ends when only one player remains") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.PRECOMBAT_MAIN)
            state = state.updatePlayer(player2Id) { it.setLife(0) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action)

            result.state.isGameOver.shouldBeTrue()
            result.state.winner shouldBe player1Id
        }

        test("generates CreatureDied event when creature dies") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.COMBAT_DAMAGE)
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
                .dealDamage(2)
            state = state.updateBattlefield { it.addToTop(bear) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events.any { it is GameEvent.CreatureDied && it.cardId == bear.id.value }.shouldBeTrue()
        }

        test("generates PlayerLost event when player loses") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.PRECOMBAT_MAIN)
            state = state.updatePlayer(player2Id) { it.setLife(0) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events.any { it is GameEvent.PlayerLost && it.playerId == player2Id.value }.shouldBeTrue()
        }

        test("generates GameEnded event when game ends") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.PRECOMBAT_MAIN)
            state = state.updatePlayer(player2Id) { it.setLife(0) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action) as ActionResult.Success

            result.events.any { it is GameEvent.GameEnded }.shouldBeTrue()
        }

        test("handles multiple creatures dying simultaneously") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.COMBAT_DAMAGE)
            val bear1 = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
                .dealDamage(2)
            val bear2 = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
                .dealDamage(3)
            state = state.updateBattlefield { it.addToTop(bear1).addToTop(bear2) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action)

            result.state.battlefield.contains(bear1.id).shouldBeFalse()
            result.state.battlefield.contains(bear2.id).shouldBeFalse()
            result.state.getPlayer(player1Id).graveyard.contains(bear1.id).shouldBeTrue()
            result.state.getPlayer(player2Id).graveyard.contains(bear2.id).shouldBeTrue()
        }

        test("clears damage when creature dies") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.COMBAT_DAMAGE)
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
                .dealDamage(5)
            state = state.updateBattlefield { it.addToTop(bear) }

            val action = CheckStateBasedActions()
            val result = ActionExecutor.execute(state, action)

            val deadCreature = result.state.getPlayer(player1Id).graveyard.getCard(bear.id)!!
            deadCreature.damageMarked shouldBe 0
        }
    }

    context("full combat flow") {
        test("complete combat: attack, block, damage, death") {
            // Setup game in declare attackers step
            var state = createGameInDeclareAttackersStep()
            val attacker = CardInstance.create(giantDef, player1Id.value) // 3/3
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value) // 2/2
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker) }

            // Declare attacker
            val declareResult = ActionExecutor.execute(state, DeclareAttacker(attacker.id, player1Id))
            declareResult.isSuccess.shouldBeTrue()
            state = declareResult.state.advanceToStep(Step.DECLARE_BLOCKERS)

            // Declare blocker
            val blockResult = ActionExecutor.execute(state, DeclareBlocker(blocker.id, attacker.id, player2Id))
            blockResult.isSuccess.shouldBeTrue()
            state = blockResult.state.advanceToStep(Step.COMBAT_DAMAGE)

            // Resolve combat damage
            val damageResult = ActionExecutor.execute(state, ResolveCombatDamage())
            damageResult.isSuccess.shouldBeTrue()
            state = damageResult.state

            // Attacker took 2 damage, blocker took 3 damage
            state.battlefield.getCard(attacker.id)!!.damageMarked shouldBe 2
            state.battlefield.getCard(blocker.id)!!.damageMarked shouldBe 3

            // Check state-based actions - blocker should die
            val sbaResult = ActionExecutor.execute(state, CheckStateBasedActions())
            sbaResult.isSuccess.shouldBeTrue()
            state = sbaResult.state

            // Blocker is dead (3 damage >= 2 toughness)
            state.battlefield.contains(blocker.id).shouldBeFalse()
            state.getPlayer(player2Id).graveyard.contains(blocker.id).shouldBeTrue()

            // Attacker survives (2 damage < 3 toughness)
            state.battlefield.contains(attacker.id).shouldBeTrue()

            // End combat
            val endResult = ActionExecutor.execute(state, EndCombat(player1Id))
            endResult.isSuccess.shouldBeTrue()
            endResult.state.combat.shouldBeNull()
        }

        test("unblocked attacker deals damage to player") {
            var state = createGameInDeclareAttackersStep()
            val attacker = CardInstance.create(giantDef, player1Id.value) // 3/3
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker) }

            val initialLife = state.getPlayer(player2Id).life

            // Declare attacker
            state = ActionExecutor.execute(state, DeclareAttacker(attacker.id, player1Id)).state
                .advanceToStep(Step.DECLARE_BLOCKERS)
                .advanceToStep(Step.COMBAT_DAMAGE)

            // Resolve combat damage - no blockers
            state = ActionExecutor.execute(state, ResolveCombatDamage()).state

            state.getPlayer(player2Id).life shouldBe initialLife - 3

            // End combat
            state = ActionExecutor.execute(state, EndCombat(player1Id)).state
            state.combat.shouldBeNull()
        }

        test("lethal damage to player ends game") {
            var state = createGameInDeclareAttackersStep()
            val attacker = CardInstance.create(giantDef, player1Id.value) // 3/3
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker) }
                .updatePlayer(player2Id) { it.setLife(3) } // Exactly lethal

            // Declare attacker and resolve damage
            state = ActionExecutor.execute(state, DeclareAttacker(attacker.id, player1Id)).state
                .advanceToStep(Step.DECLARE_BLOCKERS)
                .advanceToStep(Step.COMBAT_DAMAGE)
            state = ActionExecutor.execute(state, ResolveCombatDamage()).state

            state.getPlayer(player2Id).life shouldBe 0

            // Check state-based actions - player should lose
            state = ActionExecutor.execute(state, CheckStateBasedActions()).state

            state.isGameOver.shouldBeTrue()
            state.winner shouldBe player1Id
        }
    }
})
