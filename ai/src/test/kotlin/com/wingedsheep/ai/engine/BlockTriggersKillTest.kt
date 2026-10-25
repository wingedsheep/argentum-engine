package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.p02.cards.AlabornZealot
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * [AiProfile.blockTriggersKill]: a blocker whose block trigger destroys the attacker is a killer.
 *
 * Off a 4-player AI-vs-AI playtest (2026-10-25, game 01, turn 29): a hexproof 6/5 Leviathan token
 * attacked alone into an untapped Alaborn Zealot ("When this creature blocks a creature, destroy
 * both creatures") and traded for the 1/1. The attack evaluator predicts blocks with the
 * stats-and-keywords heuristic, which never puts a 1/1 in front of a 6/5 — so the trigger was
 * never simulated.
 */
class BlockTriggersKillTest : FunSpec({

    val leviathan = card("Test Leviathan") {
        manaCost = "{5}{U}{U}"
        typeLine = "Creature — Leviathan"
        power = 6
        toughness = 5
        keywords(Keyword.HEXPROOF)
    }
    val squire = card("Test Squire") {
        manaCost = "{W}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
    }
    // A bare "blocks" trigger: the engine gives it no triggering entity, so the destroy is inert.
    val bareBlocker = card("Test Bare Zealot") {
        manaCost = "{W}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.blocks()
            effect = Effects.Destroy(EffectTarget.TriggeringEntity)
        }
    }
    val cards = TestCards.all + listOf(leviathan, squire, bareBlocker, AlabornZealot)
    val registry = CardRegistry().apply { register(cards) }

    fun advisor(blockTriggersKill: Boolean) = CombatAdvisor(
        GameSimulator(registry), AIPlayer.defaultEvaluator(), registry,
        blockTriggersKill = blockTriggersKill,
    )

    class Board(val driver: GameTestDriver, val me: EntityId, val them: EntityId, val attacker: EntityId, val blocker: EntityId)

    /** Our ready 6/5 hexproof against the opponent's lone untapped [blockerName]; both at 20. */
    fun board(blockerName: String): Board {
        val driver = GameTestDriver()
        driver.registerCards(cards)
        val (me, them) = driver.initMultiplayer(decks = List(2) { Deck.of("Plains" to 40) })
        val attacker = driver.putCreatureOnBattlefield(me, "Test Leviathan").also { driver.removeSummoningSickness(it) }
        val blocker = driver.putCreatureOnBattlefield(them, blockerName)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        return Board(driver, me, them, attacker, blocker)
    }

    fun Board.attackers(advisor: CombatAdvisor): Set<EntityId> {
        val action = driver.legalActions(me).single { it.actionType == "DeclareAttackers" }
        return (advisor.chooseAttackers(driver.state, action, me) as DeclareAttackers).attackers.keys
    }

    /** The heuristic blocks the defender makes once the 6/5 attacks — what the attack evaluator predicts. */
    fun Board.predictedBlocks(advisor: CombatAdvisor): DeclareBlockers {
        driver.submitSuccess(DeclareAttackers(me, mapOf(attacker to them)))
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        val action = driver.legalActions(them).single { it.actionType == "DeclareBlockers" }
        return advisor.chooseBlockers(driver.state, action, them, useSimulation = false) as DeclareBlockers
    }

    test("the 6/5 stays home: an Alaborn Zealot block destroys it") {
        val board = board("Alaborn Zealot")
        board.attackers(advisor(blockTriggersKill = true)) shouldNotContain board.attacker
    }

    test("the defender's heuristic blocks with the Zealot, trading a 1/1 for the 6/5") {
        val board = board("Alaborn Zealot")
        board.predictedBlocks(advisor(blockTriggersKill = true)).blockers shouldBe
            mapOf(board.blocker to listOf(board.attacker))
    }

    test("control: a vanilla 1/1 cannot stop the 6/5, so it attacks") {
        val board = board("Test Squire")
        board.attackers(advisor(blockTriggersKill = true)) shouldContain board.attacker
    }

    test("a bare blocks trigger has no triggering entity, so it is not read as a kill") {
        val board = board("Test Bare Zealot")
        board.attackers(advisor(blockTriggersKill = true)) shouldContain board.attacker
    }

    test("with the flag off, the 6/5 attacks into the Zealot (the playtest misplay)") {
        val board = board("Alaborn Zealot")
        board.attackers(advisor(blockTriggersKill = false)) shouldContain board.attacker
    }

    test("LIVE ships the flag") {
        AiProfile.LIVE.blockTriggersKill shouldBe true
    }
})
