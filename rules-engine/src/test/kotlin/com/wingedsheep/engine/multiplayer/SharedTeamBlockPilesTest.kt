package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.core.PilesSplitResponse
import com.wingedsheep.engine.core.SplitPilesDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.combat.RandomizedBlockerPilesExecutor
import com.wingedsheep.engine.handlers.effects.player.ControlCombatDeclarationsExecutor
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.effects.CombatDeclaration
import com.wingedsheep.sdk.scripting.effects.ControlCombatDeclarationsThisTurnEffect
import com.wingedsheep.sdk.scripting.effects.RandomizedBlockerPilesEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SharedTeamBlockPilesTest : FunSpec({
    val limited = card("Shared Pile Limited") {
        typeLine = "Creature"
        power = 3
        toughness = 3
        staticAbility { ability = CantBeBlockedByMoreThan(1) }
    }

    data class Fixture(
        val driver: GameTestDriver,
        val players: List<EntityId>,
        val attackers: List<EntityId>,
        val blockers: List<EntityId>,
    )

    fun fixture(capped: Boolean = false): Fixture {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + limited)
        val players = d.initMultiplayer(
            decks = List(4) { Deck.of("Mountain" to 40) },
            format = Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val attackers = if (capped) {
            listOf(d.putCreatureOnBattlefield(players[0], limited.name))
        } else {
            List(2) { d.putCreatureOnBattlefield(players[0], "Centaur Courser") }
        }
        attackers.forEach { d.removeSummoningSickness(it) }
        val blockers = players.drop(2).map { d.putCreatureOnBattlefield(it, "Centaur Courser") }
        val declarations = ControlCombatDeclarationsExecutor()
        for ((controller, affected) in listOf(players[0] to Player.Each, players[2] to Player.You)) {
            d.replaceState(declarations.execute(
                d.state, ControlCombatDeclarationsThisTurnEffect(setOf(CombatDeclaration.BLOCKERS), affected),
                EffectContext(sourceId = null, controllerId = controller),
            ).state)
        }
        d.replaceState(RandomizedBlockerPilesExecutor().execute(
            d.state, RandomizedBlockerPilesEffect(), EffectContext(sourceId = null, controllerId = players[0]),
        ).state)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(players[0], attackers, players[2]).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.replaceState(d.state.copy(rng = GameRng.seeded(42)))
        d.declareNoBlockers(players[2]).error shouldBe null
        return Fixture(d, players, attackers, blockers)
    }

    fun Fixture.assertUncommitted() {
        blockers.forEach { driver.state.getEntity(it)!!.has<BlockingComponent>() shouldBe false }
        players.drop(2).forEach {
            driver.state.getEntity(it)!!.has<BlockersDeclaredThisCombatComponent>() shouldBe false
        }
        driver.events.filterIsInstance<BlockersDeclaredEvent>() shouldBe emptyList()
    }

    fun GameTestDriver.answer(piles: List<List<EntityId>>) =
        (pendingDecision as SplitPilesDecision).let { submitDecision(it.playerId, PilesSplitResponse(it.id, piles)) }

    test("split choosers submit only their own creatures before a single combined random assignment") {
        val f = fixture()
        val d = f.driver
        val first = d.pendingDecision as SplitPilesDecision
        first.playerId shouldBe f.players[2]
        first.cards shouldBe listOf(f.blockers[0])
        first.numberOfPiles shouldBe 2
        val rng = d.state.rng
        val beforeInvalid = d.state
        d.answer(listOf(listOf(f.blockers[1]), emptyList())).error shouldNotBe null
        d.state shouldBe beforeInvalid

        d.answer(listOf(listOf(f.blockers[0]), emptyList())).error shouldBe null
        d.state.rng shouldBe rng
        f.assertUncommitted()
        val second = d.pendingDecision as SplitPilesDecision
        second.playerId shouldBe f.players[0]
        second.cards shouldBe listOf(f.blockers[1])
        second.numberOfPiles shouldBe 2
        d.answer(listOf(listOf(f.blockers[0]), emptyList())).error shouldNotBe null
        d.state.rng shouldBe rng
        d.answer(listOf(listOf(f.blockers[1]), emptyList())).error shouldBe null

        val firstTargets = d.state.getEntity(f.blockers[0])!!.get<BlockingComponent>()!!.blockedAttackerIds
        firstTargets.size shouldBe 1
        d.state.getEntity(f.blockers[1])!!.get<BlockingComponent>()!!.blockedAttackerIds shouldBe firstTargets
        d.state.getEntity(firstTargets.single())!!.get<BlockedComponent>()!!.blockerIds.toSet() shouldBe f.blockers.toSet()
        f.players.drop(2).forEach {
            d.state.getEntity(it)!!.has<BlockersDeclaredThisCombatComponent>() shouldBe true
        }
        d.events.filterIsInstance<BlockersDeclaredEvent>().isNotEmpty() shouldBe true
    }

    test("split trimming preserves ownership and retries an illegal aggregate without rerolling") {
        val f = fixture(capped = true)
        val d = f.driver
        d.answer(listOf(listOf(f.blockers[0]))).error shouldBe null
        f.assertUncommitted()
        d.answer(listOf(listOf(f.blockers[1]))).error shouldBe null
        val rngAfterAssignment = d.state.rng
        val firstTrim = d.pendingDecision as SplitPilesDecision
        firstTrim.playerId shouldBe f.players[2]
        firstTrim.cards shouldBe listOf(f.blockers[0])
        firstTrim.pileOptions.values.flatten().toSet() shouldBe setOf(f.blockers[0])
        f.assertUncommitted()
        d.answer(listOf(listOf(f.blockers[1]))).error shouldNotBe null
        d.state.rng shouldBe rngAfterAssignment

        d.answer(listOf(listOf(f.blockers[0]))).error shouldBe null
        val secondTrim = d.pendingDecision as SplitPilesDecision
        secondTrim.playerId shouldBe f.players[0]
        secondTrim.cards shouldBe listOf(f.blockers[1])
        f.assertUncommitted()
        d.answer(listOf(listOf(f.blockers[1]))).error shouldBe null
        d.state.rng shouldBe rngAfterAssignment
        f.assertUncommitted()

        val retry = d.pendingDecision as SplitPilesDecision
        retry.playerId shouldBe f.players[2]
        retry.cards shouldBe listOf(f.blockers[0])
        d.answer(listOf(emptyList())).error shouldBe null
        d.state.rng shouldBe rngAfterAssignment
        d.answer(listOf(listOf(f.blockers[1]))).error shouldBe null
        d.state.rng shouldBe rngAfterAssignment
        d.state.getEntity(f.blockers[0])!!.has<BlockingComponent>() shouldBe false
        d.state.getEntity(f.blockers[1])!!.get<BlockingComponent>()!!.blockedAttackerIds shouldBe f.attackers
        d.state.getEntity(f.attackers.single())!!.get<BlockedComponent>()!!.blockerIds shouldBe listOf(f.blockers[1])
        f.players.drop(2).forEach {
            d.state.getEntity(it)!!.has<BlockersDeclaredThisCombatComponent>() shouldBe true
        }
    }
})
