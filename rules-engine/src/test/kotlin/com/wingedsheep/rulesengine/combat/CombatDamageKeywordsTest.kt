package com.wingedsheep.rulesengine.combat

import com.wingedsheep.rulesengine.action.ActionExecutor
import com.wingedsheep.rulesengine.action.ActionResult
import com.wingedsheep.rulesengine.action.ResolveCombatDamage
import com.wingedsheep.rulesengine.action.CheckStateBasedActions
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.game.Step
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CombatDamageKeywordsTest : FunSpec({

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

    val firstStrikeCreatureDef = CardDefinition.creature(
        name = "White Knight",
        manaCost = ManaCost.parse("{W}{W}"),
        subtypes = setOf(Subtype(value = "Knight")),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.FIRST_STRIKE)
    )

    val doubleStrikeCreatureDef = CardDefinition.creature(
        name = "Mirran Crusader",
        manaCost = ManaCost.parse("{1}{W}{W}"),
        subtypes = setOf(Subtype(value = "Knight")),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.DOUBLE_STRIKE)
    )

    val lifelinkCreatureDef = CardDefinition.creature(
        name = "Vampire Nighthawk",
        manaCost = ManaCost.parse("{1}{B}{B}"),
        subtypes = setOf(Subtype(value = "Vampire")),
        power = 2,
        toughness = 3,
        keywords = setOf(Keyword.LIFELINK, Keyword.FLYING)
    )

    val bigCreatureDef = CardDefinition.creature(
        name = "Giant",
        manaCost = ManaCost.parse("{3}{R}{R}"),
        subtypes = setOf(Subtype(value = "Giant")),
        power = 4,
        toughness = 4
    )

    fun createGameInFirstStrikeDamageStep(): GameState {
        val state = GameState.newGame(createPlayer1(), createPlayer2())
        return state
            .advanceToStep(Step.FIRST_STRIKE_COMBAT_DAMAGE)
            .startCombat(player2Id)
    }

    fun createGameInCombatDamageStep(): GameState {
        val state = GameState.newGame(createPlayer1(), createPlayer2())
        return state
            .advanceToStep(Step.COMBAT_DAMAGE)
            .startCombat(player2Id)
    }

    context("First Strike") {
        test("first strike creature deals damage in first strike step") {
            var state = createGameInFirstStrikeDamageStep()
            val firstStriker = CardInstance.create(firstStrikeCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(firstStriker) }
                .updateCombat { it.addAttacker(firstStriker.id) }

            val result = ActionExecutor.execute(state, ResolveCombatDamage())

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            // Defender started at 20, takes 2 damage
            newState.getPlayer(player2Id).life shouldBe 18
        }

        test("first strike creature does not deal damage in regular damage step") {
            var state = createGameInCombatDamageStep()
            val firstStriker = CardInstance.create(firstStrikeCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(firstStriker) }
                .updateCombat { it.addAttacker(firstStriker.id) }

            val result = ActionExecutor.execute(state, ResolveCombatDamage())

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            // First striker doesn't deal damage in regular damage step
            newState.getPlayer(player2Id).life shouldBe 20
        }

        test("regular creature does not deal damage in first strike step") {
            var state = createGameInFirstStrikeDamageStep()
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(bear) }
                .updateCombat { it.addAttacker(bear.id) }

            val result = ActionExecutor.execute(state, ResolveCombatDamage())

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            // Regular creature doesn't deal damage in first strike step
            newState.getPlayer(player2Id).life shouldBe 20
        }

        test("regular creature deals damage in regular damage step") {
            var state = createGameInCombatDamageStep()
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(bear) }
                .updateCombat { it.addAttacker(bear.id) }

            val result = ActionExecutor.execute(state, ResolveCombatDamage())

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            newState.getPlayer(player2Id).life shouldBe 18
        }

        test("first strike attacker can kill blocker before it deals damage") {
            var state = createGameInFirstStrikeDamageStep()
            val firstStriker = CardInstance.create(firstStrikeCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val bear = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(firstStriker).addToTop(bear) }
                .updateCombat { it.addAttacker(firstStriker.id).addBlocker(bear.id, firstStriker.id) }

            // First strike damage step
            val result1 = ActionExecutor.execute(state, ResolveCombatDamage())
            result1.shouldBeInstanceOf<ActionResult.Success>()
            var newState = (result1 as ActionResult.Success).state

            // Bear takes 2 damage (lethal)
            val bearAfterFirstStrike = newState.battlefield.getCard(bear.id)!!
            bearAfterFirstStrike.damageMarked shouldBe 2

            // First striker takes no damage (bear hasn't dealt damage yet)
            val firstStrikerAfterFirstStrike = newState.battlefield.getCard(firstStriker.id)!!
            firstStrikerAfterFirstStrike.damageMarked shouldBe 0

            // Check state-based actions - bear dies
            val sbaResult = ActionExecutor.execute(newState, CheckStateBasedActions())
            sbaResult.shouldBeInstanceOf<ActionResult.Success>()
            newState = (sbaResult as ActionResult.Success).state

            // Bear is dead (in graveyard)
            newState.battlefield.getCard(bear.id) shouldBe null

            // Move to regular damage step
            newState = newState.advanceToStep(Step.COMBAT_DAMAGE)

            // Regular damage step - bear is already dead, can't deal damage back
            val result2 = ActionExecutor.execute(newState, ResolveCombatDamage())
            result2.shouldBeInstanceOf<ActionResult.Success>()
            val finalState = (result2 as ActionResult.Success).state

            // First striker survives unscathed
            val firstStrikerFinal = finalState.battlefield.getCard(firstStriker.id)!!
            firstStrikerFinal.damageMarked shouldBe 0
        }

        test("first strike blocker deals damage before attacker") {
            var state = createGameInFirstStrikeDamageStep()
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val firstStriker = CardInstance.create(firstStrikeCreatureDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(bear).addToTop(firstStriker) }
                .updateCombat { it.addAttacker(bear.id).addBlocker(firstStriker.id, bear.id) }

            // First strike damage step - first striker blocker deals damage
            val result1 = ActionExecutor.execute(state, ResolveCombatDamage())
            result1.shouldBeInstanceOf<ActionResult.Success>()
            var newState = (result1 as ActionResult.Success).state

            // Bear takes 2 damage (lethal)
            val bearAfterFirstStrike = newState.battlefield.getCard(bear.id)!!
            bearAfterFirstStrike.damageMarked shouldBe 2

            // First striker takes no damage yet
            val firstStrikerAfterFirstStrike = newState.battlefield.getCard(firstStriker.id)!!
            firstStrikerAfterFirstStrike.damageMarked shouldBe 0

            // Check state-based actions - bear dies
            val sbaResult = ActionExecutor.execute(newState, CheckStateBasedActions())
            sbaResult.shouldBeInstanceOf<ActionResult.Success>()
            newState = (sbaResult as ActionResult.Success).state

            // Bear is dead
            newState.battlefield.getCard(bear.id) shouldBe null
        }
    }

    context("Double Strike") {
        test("double strike creature deals damage in first strike step") {
            var state = createGameInFirstStrikeDamageStep()
            val doubleStriker = CardInstance.create(doubleStrikeCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(doubleStriker) }
                .updateCombat { it.addAttacker(doubleStriker.id) }

            val result = ActionExecutor.execute(state, ResolveCombatDamage())

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            newState.getPlayer(player2Id).life shouldBe 18
        }

        test("double strike creature deals damage in regular damage step too") {
            var state = createGameInCombatDamageStep()
            val doubleStriker = CardInstance.create(doubleStrikeCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(doubleStriker) }
                .updateCombat { it.addAttacker(doubleStriker.id) }

            val result = ActionExecutor.execute(state, ResolveCombatDamage())

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            newState.getPlayer(player2Id).life shouldBe 18
        }

        test("double strike creature deals total of double damage across both steps") {
            var state = createGameInFirstStrikeDamageStep()
            val doubleStriker = CardInstance.create(doubleStrikeCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(doubleStriker) }
                .updateCombat { it.addAttacker(doubleStriker.id) }

            // First strike damage
            val result1 = ActionExecutor.execute(state, ResolveCombatDamage())
            result1.shouldBeInstanceOf<ActionResult.Success>()
            var newState = (result1 as ActionResult.Success).state
            newState.getPlayer(player2Id).life shouldBe 18

            // Move to regular damage step
            newState = newState.advanceToStep(Step.COMBAT_DAMAGE)

            // Regular damage
            val result2 = ActionExecutor.execute(newState, ResolveCombatDamage())
            result2.shouldBeInstanceOf<ActionResult.Success>()
            val finalState = (result2 as ActionResult.Success).state

            // Total 4 damage dealt (2 + 2)
            finalState.getPlayer(player2Id).life shouldBe 16
        }

        test("double strike creature deals double damage to blocker") {
            var state = createGameInFirstStrikeDamageStep()
            val doubleStriker = CardInstance.create(doubleStrikeCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val bigCreature = CardInstance.create(bigCreatureDef, player2Id.value) // 4/4
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(doubleStriker).addToTop(bigCreature) }
                .updateCombat { it.addAttacker(doubleStriker.id).addBlocker(bigCreature.id, doubleStriker.id) }

            // First strike damage - 2 damage to 4/4
            val result1 = ActionExecutor.execute(state, ResolveCombatDamage())
            result1.shouldBeInstanceOf<ActionResult.Success>()
            var newState = (result1 as ActionResult.Success).state

            val bigCreatureAfterFirstStrike = newState.battlefield.getCard(bigCreature.id)!!
            bigCreatureAfterFirstStrike.damageMarked shouldBe 2

            // Move to regular damage step
            newState = newState.advanceToStep(Step.COMBAT_DAMAGE)

            // Regular damage - another 2 damage, plus 4/4 deals back
            val result2 = ActionExecutor.execute(newState, ResolveCombatDamage())
            result2.shouldBeInstanceOf<ActionResult.Success>()
            val finalState = (result2 as ActionResult.Success).state

            // 4/4 takes total 4 damage (lethal)
            val bigCreatureFinal = finalState.battlefield.getCard(bigCreature.id)!!
            bigCreatureFinal.damageMarked shouldBe 4

            // Double striker takes 4 damage from the 4/4 (lethal)
            val doubleStrikerFinal = finalState.battlefield.getCard(doubleStriker.id)!!
            doubleStrikerFinal.damageMarked shouldBe 4
        }
    }

    context("Lifelink") {
        test("lifelink creature controller gains life when dealing combat damage to player") {
            var state = createGameInCombatDamageStep()
            val lifelinker = CardInstance.create(lifelinkCreatureDef, player1Id.value) // 2/3 lifelink
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(lifelinker) }
                .updateCombat { it.addAttacker(lifelinker.id) }

            val result = ActionExecutor.execute(state, ResolveCombatDamage())

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            // Defender takes 2 damage
            newState.getPlayer(player2Id).life shouldBe 18
            // Attacker gains 2 life
            newState.getPlayer(player1Id).life shouldBe 22
        }

        test("lifelink creature controller gains life when dealing damage to blocker") {
            var state = createGameInCombatDamageStep()
            val lifelinker = CardInstance.create(lifelinkCreatureDef, player1Id.value) // 2/3 lifelink
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val bear = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(lifelinker).addToTop(bear) }
                .updateCombat { it.addAttacker(lifelinker.id).addBlocker(bear.id, lifelinker.id) }

            val result = ActionExecutor.execute(state, ResolveCombatDamage())

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            // Controller gains life equal to damage dealt to blocker
            newState.getPlayer(player1Id).life shouldBe 22
        }

        test("blocking lifelink creature controller gains life") {
            var state = createGameInCombatDamageStep()
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val lifelinker = CardInstance.create(lifelinkCreatureDef, player2Id.value) // 2/3 lifelink
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(bear).addToTop(lifelinker) }
                .updateCombat { it.addAttacker(bear.id).addBlocker(lifelinker.id, bear.id) }

            val result = ActionExecutor.execute(state, ResolveCombatDamage())

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            // Lifelink blocker's controller gains 2 life
            newState.getPlayer(player2Id).life shouldBe 22
        }

        test("lifelink with trample grants life for all damage dealt") {
            val lifelinkTrampleDef = CardDefinition.creature(
                name = "Wurmcoil Engine",
                manaCost = ManaCost.parse("{6}"),
                subtypes = setOf(Subtype(value = "Wurm")),
                power = 6,
                toughness = 6,
                keywords = setOf(Keyword.LIFELINK, Keyword.TRAMPLE)
            )

            var state = createGameInCombatDamageStep()
            val lifelinkTrampler = CardInstance.create(lifelinkTrampleDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val bear = CardInstance.create(bearDef, player2Id.value) // 2/2
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(lifelinkTrampler).addToTop(bear) }
                .updateCombat { it.addAttacker(lifelinkTrampler.id).addBlocker(bear.id, lifelinkTrampler.id) }

            val result = ActionExecutor.execute(state, ResolveCombatDamage())

            result.shouldBeInstanceOf<ActionResult.Success>()
            val newState = (result as ActionResult.Success).state
            // 6 power - 2 toughness = 4 trample damage to player
            newState.getPlayer(player2Id).life shouldBe 16
            // Controller gains 6 life (2 to blocker + 4 trample)
            newState.getPlayer(player1Id).life shouldBe 26
        }

        test("double strike with lifelink gains life twice") {
            val doubleStrikeLifelinkDef = CardDefinition.creature(
                name = "Silverblade Paladin",
                manaCost = ManaCost.parse("{1}{W}{W}"),
                subtypes = setOf(Subtype(value = "Knight")),
                power = 2,
                toughness = 2,
                keywords = setOf(Keyword.DOUBLE_STRIKE, Keyword.LIFELINK)
            )

            var state = createGameInFirstStrikeDamageStep()
            val doubleStrikeLifelinker = CardInstance.create(doubleStrikeLifelinkDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(doubleStrikeLifelinker) }
                .updateCombat { it.addAttacker(doubleStrikeLifelinker.id) }

            // First strike damage
            val result1 = ActionExecutor.execute(state, ResolveCombatDamage())
            result1.shouldBeInstanceOf<ActionResult.Success>()
            var newState = (result1 as ActionResult.Success).state
            newState.getPlayer(player2Id).life shouldBe 18
            newState.getPlayer(player1Id).life shouldBe 22 // Gained 2 life

            // Move to regular damage step
            newState = newState.advanceToStep(Step.COMBAT_DAMAGE)

            // Regular damage
            val result2 = ActionExecutor.execute(newState, ResolveCombatDamage())
            result2.shouldBeInstanceOf<ActionResult.Success>()
            val finalState = (result2 as ActionResult.Success).state

            finalState.getPlayer(player2Id).life shouldBe 16 // Total 4 damage
            finalState.getPlayer(player1Id).life shouldBe 24 // Gained 4 life total
        }
    }

    context("Step progression") {
        test("steps progress from declare blockers to first strike damage to regular damage") {
            val state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.DECLARE_BLOCKERS)

            val afterDeclareBlockers = state.advanceStep()
            afterDeclareBlockers.currentStep shouldBe Step.FIRST_STRIKE_COMBAT_DAMAGE

            val afterFirstStrike = afterDeclareBlockers.advanceStep()
            afterFirstStrike.currentStep shouldBe Step.COMBAT_DAMAGE

            val afterCombatDamage = afterFirstStrike.advanceStep()
            afterCombatDamage.currentStep shouldBe Step.END_COMBAT
        }
    }
})
