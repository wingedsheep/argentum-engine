package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.player.ControlCombatDeclarationsExecutor
import com.wingedsheep.engine.mechanics.combat.SharedTeamBlockChoices
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.CombatDeclaration
import com.wingedsheep.sdk.scripting.effects.ControlCombatDeclarationsThisTurnEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.ArchangelOfTithes
import com.wingedsheep.mtg.sets.definitions.xln.cards.BondedHorncrest
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SharedTeamBlockingAiTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(BondedHorncrest, ArchangelOfTithes))
        initMultiplayer(List(4) { Deck.of("Plains" to 40) },
            format = Format.TwoHeadedGiant(), teams = listOf(listOf(0, 1), listOf(2, 3)))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("AI declares Horncrests from both teammates together") {
        val game = driver()
        val (a, _, b, partner) = game.state.turnOrder
        val first = game.putCreatureOnBattlefield(a, "Grizzly Bears")
        val second = game.putCreatureOnBattlefield(a, "Grizzly Bears")
        game.removeSummoningSickness(first)
        game.removeSummoningSickness(second)
        val horncrest = game.putCreatureOnBattlefield(b, "Bonded Horncrest")
        val ally = game.putCreatureOnBattlefield(partner, "Bonded Horncrest")
        game.passPriorityUntil(Step.DECLARE_ATTACKERS)
        game.declareAttackers(a, listOf(first, second), partner).error shouldBe null
        game.passPriorityUntil(Step.DECLARE_BLOCKERS)
        val legal = LegalActionEnumerator.create(game.cardRegistry).enumerate(game.state, b)
            .single { it.actionType == "DeclareBlockers" }
        val chosen = AIPlayer.create(game.cardRegistry, b).chooseFrom(game.state, listOf(legal)).action as DeclareBlockers
        chosen.blockers.filterValues { it.isNotEmpty() }.keys shouldBe setOf(horncrest, ally)
        game.submit(chosen).error shouldBe null
    }

    test("tax budget retains a teammate's paid block and drops only the unpaid controller's block") {
        val game = driver()
        val (a, _, b, partner) = game.state.turnOrder
        val angel = game.putCreatureOnBattlefield(a, "Archangel of Tithes")
        val attacker = game.putCreatureOnBattlefield(a, "Centaur Courser")
        game.removeSummoningSickness(angel)
        game.removeSummoningSickness(attacker)
        val expensiveUnpaid = game.putCreatureOnBattlefield(b, "Force of Nature")
        val paid = game.putCreatureOnBattlefield(partner, "Grizzly Bears")
        game.putLandOnBattlefield(partner, "Plains")
        game.passPriorityUntil(Step.DECLARE_ATTACKERS)
        game.declareAttackers(a, listOf(angel, attacker), b).error shouldBe null
        game.passPriorityUntil(Step.DECLARE_BLOCKERS)
        val budget = CombatTaxBudget.affordableBlock(game.state, game.state.projectedState, b,
            game.cardRegistry, mapOf(expensiveUnpaid to listOf(attacker), paid to listOf(attacker)))
        budget shouldBe mapOf(paid to listOf(attacker))
    }

    test("tax budget cannot borrow mana from a teammate") {
        val game = driver()
        val (a, _, b, partner) = game.state.turnOrder
        val angel = game.putCreatureOnBattlefield(a, "Archangel of Tithes")
        game.removeSummoningSickness(angel)
        val blocker = game.putCreatureOnBattlefield(partner, "Serra Angel")
        game.putLandOnBattlefield(b, "Plains")
        game.passPriorityUntil(Step.DECLARE_ATTACKERS)
        game.declareAttackers(a, listOf(angel), b).error shouldBe null
        game.passPriorityUntil(Step.DECLARE_BLOCKERS)
        CombatTaxBudget.affordableBlock(game.state, game.state.projectedState, b,
            game.cardRegistry, mapOf(blocker to listOf(angel))) shouldBe emptyMap()
    }
    fun splitChoiceBoard(): Pair<GameTestDriver, List<com.wingedsheep.sdk.model.EntityId>> {
        val game = driver()
        val (a, _, b, partner) = game.state.turnOrder
        val menace = card("Team AI Menace") {
            typeLine = "Creature"; power = 3; toughness = 3; keywords(Keyword.MENACE)
        }
        game.registerCards(listOf(menace))
        val attacker = game.putCreatureOnBattlefield(a, menace.name)
        game.removeSummoningSickness(attacker)
        val first = game.putCreatureOnBattlefield(b, "Bonded Horncrest")
        val second = game.putCreatureOnBattlefield(partner, "Bonded Horncrest")
        game.passPriorityUntil(Step.DECLARE_ATTACKERS)
        game.declareAttackers(a, listOf(attacker), b).error shouldBe null
        game.passPriorityUntil(Step.DECLARE_BLOCKERS)
        for ((controller, players) in listOf(a to Player.Each, b to Player.You)) {
            game.replaceState(ControlCombatDeclarationsExecutor().execute(game.state,
                ControlCombatDeclarationsThisTurnEffect(setOf(CombatDeclaration.BLOCKERS), players),
                EffectContext(sourceId = null, controllerId = controller)).state)
        }
        return game to listOf(attacker, first, second)
    }

    test("AI completes a saved teammate menace block without resubmitting that teammate's choice") {
        val (game, creatures) = splitChoiceBoard()
        val (_, _, b, partner) = game.state.turnOrder
        val (attacker, first, second) = creatures
        game.declareBlockers(b, mapOf(first to listOf(attacker))).error shouldBe null
        val legal = LegalActionEnumerator.create(game.cardRegistry).enumerate(game.state, partner)
            .single { it.actionType == "DeclareBlockers" }
        legal.proposedBlockerAssignments shouldBe mapOf(first to listOf(attacker))
        val chosen = AIPlayer.create(game.cardRegistry, partner).chooseFrom(game.state, listOf(legal)).action as DeclareBlockers
        chosen.blockers shouldBe mapOf(second to listOf(attacker))
        game.submit(chosen).error shouldBe null
        SharedTeamBlockChoices.problem(game.state, b) shouldBe null
        listOf(first, second).forEach { game.state.getEntity(it)!!.has<BlockingComponent>() shouldBe true }
    }

    test("AI may propose the first half of a legal team menace block") {
        val (game, creatures) = splitChoiceBoard()
        val (_, _, b, partner) = game.state.turnOrder
        val (attacker, first, second) = creatures
        val legal = LegalActionEnumerator.create(game.cardRegistry).enumerate(game.state, b)
            .single { it.actionType == "DeclareBlockers" }
        val chosen = AIPlayer.create(game.cardRegistry, b).chooseFrom(game.state, listOf(legal)).action as DeclareBlockers
        chosen.blockers shouldBe mapOf(first to listOf(attacker))
        game.submit(chosen).error shouldBe null
        game.declareBlockers(partner, mapOf(second to listOf(attacker))).error shouldBe null
        SharedTeamBlockChoices.problem(game.state, b) shouldBe null
        listOf(first, second).forEach { game.state.getEntity(it)!!.has<BlockingComponent>() shouldBe true }
    }

})
