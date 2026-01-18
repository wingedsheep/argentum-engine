package com.wingedsheep.rulesengine.combat

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

class CombatValidatorTest : FunSpec({

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

    val hastyCreatureDef = CardDefinition.creature(
        name = "Goblin Guide",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype(value = "Goblin")),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.HASTE)
    )

    val defenderDef = CardDefinition.creature(
        name = "Wall of Stone",
        manaCost = ManaCost.parse("{1}{W}{W}"),
        subtypes = setOf(Subtype(value = "Wall")),
        power = 0,
        toughness = 4,
        keywords = setOf(Keyword.DEFENDER)
    )

    val flyingCreatureDef = CardDefinition.creature(
        name = "Air Elemental",
        manaCost = ManaCost.parse("{3}{U}{U}"),
        subtypes = setOf(Subtype(value = "Elemental")),
        power = 4,
        toughness = 4,
        keywords = setOf(Keyword.FLYING)
    )

    val reachCreatureDef = CardDefinition.creature(
        name = "Giant Spider",
        manaCost = ManaCost.parse("{3}{G}"),
        subtypes = setOf(Subtype(value = "Spider")),
        power = 2,
        toughness = 4,
        keywords = setOf(Keyword.REACH)
    )

    val trampleCreatureDef = CardDefinition.creature(
        name = "Craw Wurm",
        manaCost = ManaCost.parse("{4}{G}{G}"),
        subtypes = setOf(Subtype(value = "Wurm")),
        power = 6,
        toughness = 4,
        keywords = setOf(Keyword.TRAMPLE)
    )

    val deathtouchCreatureDef = CardDefinition.creature(
        name = "Typhoid Rats",
        manaCost = ManaCost.parse("{B}"),
        subtypes = setOf(Subtype(value = "Rat")),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.DEATHTOUCH)
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

    context("canDeclareAttacker") {
        test("valid attacker returns Valid") {
            var state = createGameInDeclareAttackersStep()
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(bear) }

            val result = CombatValidator.canDeclareAttacker(state, bear.id, player1Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Valid>()
        }

        test("fails when not in declare attackers step") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.PRECOMBAT_MAIN)
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(bear) }

            val result = CombatValidator.canDeclareAttacker(state, bear.id, player1Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Can only declare attackers during declare attackers step"
        }

        test("fails when not active player") {
            var state = createGameInDeclareAttackersStep()
            val bear = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(bear) }

            val result = CombatValidator.canDeclareAttacker(state, bear.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Only the active player can declare attackers"
        }

        test("fails when creature not on battlefield") {
            val state = createGameInDeclareAttackersStep()
            val bear = CardInstance.create(bearDef, player1Id.value)

            val result = CombatValidator.canDeclareAttacker(state, bear.id, player1Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Creature not found on battlefield"
        }

        test("fails when card is not a creature") {
            var state = createGameInDeclareAttackersStep()
            val forest = CardInstance.create(CardDefinition.basicLand("Forest", Subtype.FOREST), player1Id.value)
                .copy(controllerId = player1Id.value)
            state = state.updateBattlefield { it.addToTop(forest) }

            val result = CombatValidator.canDeclareAttacker(state, forest.id, player1Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Only creatures can attack"
        }

        test("fails when not controlling creature") {
            var state = createGameInDeclareAttackersStep()
            val bear = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(bear) }

            val result = CombatValidator.canDeclareAttacker(state, bear.id, player1Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "You don't control this creature"
        }

        test("fails when creature is tapped") {
            var state = createGameInDeclareAttackersStep()
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false, isTapped = true)
            state = state.updateBattlefield { it.addToTop(bear) }

            val result = CombatValidator.canDeclareAttacker(state, bear.id, player1Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Tapped creatures cannot attack"
        }

        test("fails when creature has summoning sickness") {
            var state = createGameInDeclareAttackersStep()
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = true)
            state = state.updateBattlefield { it.addToTop(bear) }

            val result = CombatValidator.canDeclareAttacker(state, bear.id, player1Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Creature has summoning sickness"
        }

        test("creature with haste can attack with summoning sickness") {
            var state = createGameInDeclareAttackersStep()
            val hastyCreature = CardInstance.create(hastyCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = true)
            state = state.updateBattlefield { it.addToTop(hastyCreature) }

            val result = CombatValidator.canDeclareAttacker(state, hastyCreature.id, player1Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Valid>()
        }

        test("fails when creature has defender") {
            var state = createGameInDeclareAttackersStep()
            val wall = CardInstance.create(defenderDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(wall) }

            val result = CombatValidator.canDeclareAttacker(state, wall.id, player1Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Creatures with defender cannot attack"
        }

        test("fails when creature is already attacking") {
            var state = createGameInDeclareAttackersStep()
            val bear = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(bear) }
                .updateCombat { it.addAttacker(bear.id) }

            val result = CombatValidator.canDeclareAttacker(state, bear.id, player1Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Creature is already attacking"
        }
    }

    context("canDeclareBlocker") {
        test("valid blocker returns Valid") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker) }
                .updateCombat { it.addAttacker(attacker.id) }

            val result = CombatValidator.canDeclareBlocker(state, blocker.id, attacker.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Valid>()
        }

        test("fails when not in declare blockers step") {
            var state = createGameInDeclareAttackersStep() // Wrong step
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker) }
                .updateCombat { it.addAttacker(attacker.id) }

            val result = CombatValidator.canDeclareBlocker(state, blocker.id, attacker.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Can only declare blockers during declare blockers step"
        }

        test("fails when not defending player") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker) }
                .updateCombat { it.addAttacker(attacker.id) }

            val result = CombatValidator.canDeclareBlocker(state, blocker.id, attacker.id, player1Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Only the defending player can declare blockers"
        }

        test("fails when blocker not on battlefield") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value)
            state = state.updateBattlefield { it.addToTop(attacker) }
                .updateCombat { it.addAttacker(attacker.id) }

            val result = CombatValidator.canDeclareBlocker(state, blocker.id, attacker.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Blocking creature not found on battlefield"
        }

        test("fails when attacker not on battlefield") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
            val blocker = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(blocker) }

            val result = CombatValidator.canDeclareBlocker(state, blocker.id, attacker.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Attacking creature not found on battlefield"
        }

        test("fails when blocker is not a creature") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val forest = CardInstance.create(CardDefinition.basicLand("Forest", Subtype.FOREST), player2Id.value)
                .copy(controllerId = player2Id.value)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(forest) }
                .updateCombat { it.addAttacker(attacker.id) }

            val result = CombatValidator.canDeclareBlocker(state, forest.id, attacker.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Only creatures can block"
        }

        test("fails when not controlling blocker") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker) }
                .updateCombat { it.addAttacker(attacker.id) }

            val result = CombatValidator.canDeclareBlocker(state, blocker.id, attacker.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "You don't control this creature"
        }

        test("fails when blocker is tapped") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false, isTapped = true)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker) }
                .updateCombat { it.addAttacker(attacker.id) }

            val result = CombatValidator.canDeclareBlocker(state, blocker.id, attacker.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Tapped creatures cannot block"
        }

        test("fails when target is not attacking") {
            var state = createGameInDeclareBlockersStep()
            val notAttacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(notAttacker).addToTop(blocker) }

            val result = CombatValidator.canDeclareBlocker(state, blocker.id, notAttacker.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Target creature is not attacking"
        }

        test("fails when non-flying creature tries to block flying") {
            var state = createGameInDeclareBlockersStep()
            val flyer = CardInstance.create(flyingCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val bear = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(flyer).addToTop(bear) }
                .updateCombat { it.addAttacker(flyer.id) }

            val result = CombatValidator.canDeclareBlocker(state, bear.id, flyer.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Cannot block a creature with flying unless blocker has flying or reach"
        }

        test("flying creature can block flying creature") {
            var state = createGameInDeclareBlockersStep()
            val flyer1 = CardInstance.create(flyingCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val flyer2 = CardInstance.create(flyingCreatureDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(flyer1).addToTop(flyer2) }
                .updateCombat { it.addAttacker(flyer1.id) }

            val result = CombatValidator.canDeclareBlocker(state, flyer2.id, flyer1.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Valid>()
        }

        test("reach creature can block flying creature") {
            var state = createGameInDeclareBlockersStep()
            val flyer = CardInstance.create(flyingCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val reacher = CardInstance.create(reachCreatureDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(flyer).addToTop(reacher) }
                .updateCombat { it.addAttacker(flyer.id) }

            val result = CombatValidator.canDeclareBlocker(state, reacher.id, flyer.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Valid>()
        }

        test("fails when creature is already blocking this attacker") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker) }
                .updateCombat { it.addAttacker(attacker.id).addBlocker(blocker.id, attacker.id) }

            val result = CombatValidator.canDeclareBlocker(state, blocker.id, attacker.id, player2Id)

            result.shouldBeInstanceOf<CombatValidator.ValidationResult.Invalid>()
            (result as CombatValidator.ValidationResult.Invalid).reason shouldBe
                "Creature is already blocking this attacker"
        }
    }

    context("calculateCombatDamage") {
        test("unblocked attacker deals full damage to defending player") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker) }
                .updateCombat { it.addAttacker(attacker.id) }

            val result = CombatValidator.calculateCombatDamage(state, attacker.id)

            result.shouldBeInstanceOf<CombatDamageResult.UnblockedDamage>()
            (result as CombatDamageResult.UnblockedDamage).damage shouldBe 2
        }

        test("blocked attacker deals damage to blocker") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value)
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(attacker).addToTop(blocker) }
                .updateCombat { it.addAttacker(attacker.id).addBlocker(blocker.id, attacker.id) }

            val result = CombatValidator.calculateCombatDamage(state, attacker.id)

            result.shouldBeInstanceOf<CombatDamageResult.BlockedDamage>()
            (result as CombatDamageResult.BlockedDamage).damageToBlockers[blocker.id] shouldBe 2
            result.trampleDamage shouldBe 0
        }

        test("trample damage goes through to player") {
            var state = createGameInDeclareBlockersStep()
            val trampler = CardInstance.create(trampleCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value) // 2/2
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(trampler).addToTop(blocker) }
                .updateCombat { it.addAttacker(trampler.id).addBlocker(blocker.id, trampler.id) }

            val result = CombatValidator.calculateCombatDamage(state, trampler.id)

            result.shouldBeInstanceOf<CombatDamageResult.BlockedDamage>()
            // 6 power - 2 toughness = 4 trample damage
            (result as CombatDamageResult.BlockedDamage).damageToBlockers[blocker.id] shouldBe 2
            result.trampleDamage shouldBe 4
        }

        test("deathtouch only needs 1 damage to be lethal") {
            var state = createGameInDeclareBlockersStep()
            val deathtouch = CardInstance.create(deathtouchCreatureDef, player1Id.value)
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker = CardInstance.create(bearDef, player2Id.value) // 2/2
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(deathtouch).addToTop(blocker) }
                .updateCombat { it.addAttacker(deathtouch.id).addBlocker(blocker.id, deathtouch.id) }

            val result = CombatValidator.calculateCombatDamage(state, deathtouch.id)

            result.shouldBeInstanceOf<CombatDamageResult.BlockedDamage>()
            // Deathtouch means 1 damage is lethal
            (result as CombatDamageResult.BlockedDamage).damageToBlockers[blocker.id] shouldBe 1
        }

        test("creature with 0 power deals no damage") {
            var state = createGameInDeclareBlockersStep()
            val wall = CardInstance.create(defenderDef, player1Id.value) // 0/4
                .copy(controllerId = player1Id.value, summoningSickness = false)
            // Pretend wall can attack for this test
            state = state.updateBattlefield { it.addToTop(wall) }
                .updateCombat { it.addAttacker(wall.id) }

            val result = CombatValidator.calculateCombatDamage(state, wall.id)

            result.shouldBeInstanceOf<CombatDamageResult.NoDamage>()
        }

        test("damage assignment follows order for multiple blockers") {
            var state = createGameInDeclareBlockersStep()
            val trampler = CardInstance.create(trampleCreatureDef, player1Id.value) // 6/4
                .copy(controllerId = player1Id.value, summoningSickness = false)
            val blocker1 = CardInstance.create(bearDef, player2Id.value) // 2/2
                .copy(controllerId = player2Id.value, summoningSickness = false)
            val blocker2 = CardInstance.create(bearDef, player2Id.value) // 2/2
                .copy(controllerId = player2Id.value, summoningSickness = false)
            state = state.updateBattlefield { it.addToTop(trampler).addToTop(blocker1).addToTop(blocker2) }
                .updateCombat {
                    it.addAttacker(trampler.id)
                        .addBlocker(blocker1.id, trampler.id)
                        .addBlocker(blocker2.id, trampler.id)
                        .setDamageAssignmentOrder(trampler.id, listOf(blocker1.id, blocker2.id))
                }

            val result = CombatValidator.calculateCombatDamage(state, trampler.id)

            result.shouldBeInstanceOf<CombatDamageResult.BlockedDamage>()
            // 6 power, 2+2 to blockers = 2 trample
            (result as CombatDamageResult.BlockedDamage).damageToBlockers[blocker1.id] shouldBe 2
            result.damageToBlockers[blocker2.id] shouldBe 2
            result.trampleDamage shouldBe 2
        }

        test("returns Invalid when attacker not found") {
            var state = createGameInDeclareBlockersStep()
            val attacker = CardInstance.create(bearDef, player1Id.value)
            state = state.updateCombat { it.addAttacker(attacker.id) }

            val result = CombatValidator.calculateCombatDamage(state, attacker.id)

            result.shouldBeInstanceOf<CombatDamageResult.Invalid>()
        }

        test("returns Invalid when not in combat") {
            var state = GameState.newGame(createPlayer1(), createPlayer2())
                .advanceToStep(Step.PRECOMBAT_MAIN)
            val attacker = CardInstance.create(bearDef, player1Id.value)
                .copy(controllerId = player1Id.value)
            state = state.updateBattlefield { it.addToTop(attacker) }

            val result = CombatValidator.calculateCombatDamage(state, attacker.id)

            result.shouldBeInstanceOf<CombatDamageResult.Invalid>()
        }
    }

    context("hasLethalDamage") {
        test("returns true when damage equals toughness") {
            val bear = CardInstance.create(bearDef, player1Id.value)
                .dealDamage(2) // 2/2 with 2 damage

            val result = CombatValidator.hasLethalDamage(bear)

            result shouldBe true
        }

        test("returns true when damage exceeds toughness") {
            val bear = CardInstance.create(bearDef, player1Id.value)
                .dealDamage(5) // 2/2 with 5 damage

            val result = CombatValidator.hasLethalDamage(bear)

            result shouldBe true
        }

        test("returns false when damage is less than toughness") {
            val bear = CardInstance.create(bearDef, player1Id.value)
                .dealDamage(1) // 2/2 with 1 damage

            val result = CombatValidator.hasLethalDamage(bear)

            result shouldBe false
        }

        test("returns false when no damage") {
            val bear = CardInstance.create(bearDef, player1Id.value)

            val result = CombatValidator.hasLethalDamage(bear)

            result shouldBe false
        }
    }
})
