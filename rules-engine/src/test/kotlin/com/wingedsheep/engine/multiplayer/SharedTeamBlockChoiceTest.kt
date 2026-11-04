package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.player.ControlCombatDeclarationsExecutor
import com.wingedsheep.engine.mechanics.combat.CombatDeclarationControl
import com.wingedsheep.engine.mechanics.combat.SharedTeamBlockChoices
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CombatDeclaration
import com.wingedsheep.sdk.scripting.effects.ControlCombatDeclarationsThisTurnEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SharedTeamBlockChoiceTest : FunSpec({
    val menace = card("Shared Choice Menace") {
        typeLine = "Creature"
        power = 3
        toughness = 3
        keywords(Keyword.MENACE)
    }

    data class Fixture(
        val driver: GameTestDriver,
        val players: List<EntityId>,
        val attacker: EntityId,
        val blockers: List<EntityId>,
    )

    fun GameTestDriver.grant(controller: EntityId, players: Player) {
        replaceState(ControlCombatDeclarationsExecutor().execute(
            state,
            ControlCombatDeclarationsThisTurnEffect(setOf(CombatDeclaration.BLOCKERS), players),
            EffectContext(sourceId = null, controllerId = controller),
        ).state)
    }

    fun fixture(): Fixture {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + menace)
        val players = d.initMultiplayer(
            decks = List(4) { Deck.of("Mountain" to 40) },
            format = Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val attacker = d.putCreatureOnBattlefield(players[0], menace.name)
        d.removeSummoningSickness(attacker)
        val blockers = players.drop(2).map { d.putCreatureOnBattlefield(it, "Centaur Courser") }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(players[0], listOf(attacker), players[2]).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        return Fixture(d, players, attacker, blockers)
    }

    fun Fixture.assertNoCommittedBlocks() {
        blockers.forEach { driver.state.getEntity(it)!!.has<BlockingComponent>() shouldBe false }
        players.drop(2).forEach {
            driver.state.getEntity(it)!!.has<BlockersDeclaredThisCombatComponent>() shouldBe false
        }
        driver.events.filterIsInstance<BlockersDeclaredEvent>() shouldBe emptyList()
    }

    test("a later own-player grant splits choices and cannot commandeer the teammate's blockers") {
        val f = fixture()
        val d = f.driver
        d.grant(f.players[0], Player.Each)
        d.grant(f.players[2], Player.You)
        SharedTeamBlockChoices.requiresSeparateChoices(d.state, f.players[2]) shouldBe true
        CombatDeclarationControl.inputActorFor(d.state, f.players[2]) shouldBe f.players[2]
        CombatDeclarationControl.inputActorFor(d.state, f.players[3]) shouldBe f.players[0]

        d.declareBlockers(f.players[2], f.blockers.associateWith { listOf(f.attacker) }).error shouldNotBe null
        f.assertNoCommittedBlocks()
        SharedTeamBlockChoices.proposals(d.state, f.players[2]) shouldBe emptyMap()

        d.declareBlockers(f.players[2], mapOf(f.blockers[0] to listOf(f.attacker))).error shouldBe null
        f.assertNoCommittedBlocks()
        SharedTeamBlockChoices.proposals(d.state, f.players[2]) shouldBe mapOf(f.blockers[0] to listOf(f.attacker))
        d.state.priorityPlayerId shouldBe f.players[3]
        d.declareBlockers(f.players[3], mapOf(f.blockers[1] to listOf(f.attacker))).error shouldBe null
        f.blockers.forEach { d.state.getEntity(it)!!.has<BlockingComponent>() shouldBe true }
        f.players.drop(2).forEach {
            d.state.getEntity(it)!!.has<BlockersDeclaredThisCombatComponent>() shouldBe true
        }
        d.events.filterIsInstance<BlockersDeclaredEvent>().isNotEmpty() shouldBe true
    }

    test("an illegal combined choice resets both proposals and allows a legal joint retry") {
        val f = fixture()
        val d = f.driver
        d.grant(f.players[0], Player.Each)
        d.grant(f.players[2], Player.You)
        d.declareBlockers(f.players[2], mapOf(f.blockers[0] to listOf(f.attacker))).error shouldBe null
        d.declareNoBlockers(f.players[3]).error shouldBe null
        f.assertNoCommittedBlocks()
        SharedTeamBlockChoices.proposals(d.state, f.players[2]) shouldBe emptyMap()
        SharedTeamBlockChoices.problem(d.state, f.players[2]) shouldNotBe null
        d.state.priorityPlayerId shouldBe f.players[2]

        d.declareBlockers(f.players[2], mapOf(f.blockers[0] to listOf(f.attacker))).error shouldBe null
        f.assertNoCommittedBlocks()
        d.declareBlockers(f.players[3], mapOf(f.blockers[1] to listOf(f.attacker))).error shouldBe null
        SharedTeamBlockChoices.problem(d.state, f.players[2]) shouldBe null
        f.blockers.forEach { d.state.getEntity(it)!!.has<BlockingComponent>() shouldBe true }
    }

    test("each-other-player grant leaves its holder under the earlier grant while changing the teammate's chooser") {
        val f = fixture()
        val d = f.driver
        d.grant(f.players[0], Player.Each)
        d.grant(f.players[3], Player.EachOtherPlayer)
        SharedTeamBlockChoices.requiresSeparateChoices(d.state, f.players[2]) shouldBe true
        CombatDeclarationControl.inputActorFor(d.state, f.players[2]) shouldBe f.players[3]
        CombatDeclarationControl.inputActorFor(d.state, f.players[3]) shouldBe f.players[0]
        d.declareBlockers(f.players[2], mapOf(f.blockers[0] to listOf(f.attacker))).error shouldBe null
        f.assertNoCommittedBlocks()
        d.declareBlockers(f.players[3], mapOf(f.blockers[1] to listOf(f.attacker))).error shouldBe null
        f.blockers.forEach { d.state.getEntity(it)!!.has<BlockingComponent>() shouldBe true }
    }

    for (affected in listOf(Player.Each, Player.EachOpponent)) {
        test("one chooser for $affected retains a single combined team declaration") {
            val f = fixture()
            val d = f.driver
            d.grant(f.players[0], affected)
            SharedTeamBlockChoices.requiresSeparateChoices(d.state, f.players[2]) shouldBe false
            f.players.drop(2).forEach {
                CombatDeclarationControl.inputActorFor(d.state, it) shouldBe f.players[0]
            }
            d.declareBlockers(f.players[2], f.blockers.associateWith { listOf(f.attacker) }).error shouldBe null
            f.blockers.forEach { d.state.getEntity(it)!!.has<BlockingComponent>() shouldBe true }
            f.players.drop(2).forEach {
                d.state.getEntity(it)!!.has<BlockersDeclaredThisCombatComponent>() shouldBe true
            }
        }
    }
})
