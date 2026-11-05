package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Act of Heroism: untap target creature, +2/+2 until end of turn, and it can block an additional
 * creature this turn. Proves the temporarily granted extra-block permission reaches block
 * validation, stacks across copies, and ends with the turn.
 */
class ActOfHeroismScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun castFromDefender(d: GameTestDriver, caster: EntityId, target: EntityId) {
        d.replaceState(d.state.updateEntity(caster) { it.with(ManaPoolComponent(white = 2)) })
        val spell = d.putCardInHand(caster, "Act of Heroism")
        d.castSpell(caster, spell, listOf(target)).error shouldBe null
        d.bothPass().error shouldBe null
        d.state.stack shouldBe emptyList()
    }

    fun attackWithBears(d: GameTestDriver, me: EntityId, opp: EntityId, count: Int): List<EntityId> {
        val attackers = (1..count).map {
            d.putCreatureOnBattlefield(me, "Grizzly Bears").also(d::removeSummoningSickness)
        }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, attackers, opp).error shouldBe null
        d.passPriority(me).error shouldBe null
        return attackers
    }

    test("untaps a tapped blocker, pumps it, and lets it block two attackers") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.addComponent(blocker, TappedComponent)
        val (a, b) = attackWithBears(d, me, opp, 2)

        castFromDefender(d, opp, blocker)
        d.state.getEntity(blocker)!!.has<TappedComponent>() shouldBe false
        d.state.projectedState.getPower(blocker) shouldBe 4
        d.state.projectedState.getToughness(blocker) shouldBe 4

        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.legalActions(opp).single().blockerMaxBlockCounts!![blocker] shouldBe 2
        d.declareBlockers(opp, mapOf(blocker to listOf(a, b))).error shouldBe null
        d.state.getEntity(blocker)!!.get<BlockingComponent>()!!.blockedAttackerIds.toSet() shouldBe setOf(a, b)
    }

    test("one copy does not allow blocking three attackers") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val attackers = attackWithBears(d, me, opp, 3)

        castFromDefender(d, opp, blocker)
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, mapOf(blocker to attackers)).error shouldNotBe null
    }

    test("two copies are cumulative") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val attackers = attackWithBears(d, me, opp, 3)

        castFromDefender(d, opp, blocker)
        d.passPriority(me).error shouldBe null
        castFromDefender(d, opp, blocker)
        d.state.projectedState.getPower(blocker) shouldBe 6

        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.legalActions(opp).single().blockerMaxBlockCounts!![blocker] shouldBe 3
        d.declareBlockers(opp, mapOf(blocker to attackers)).error shouldBe null
    }

    test("the extra block ends with the turn") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val blocker = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.passPriority(me).error shouldBe null
        castFromDefender(d, opp, blocker)
        d.state.grantedStaticAbilities.size shouldBe 1

        d.passPriorityUntil(Step.UPKEEP)
        d.state.grantedStaticAbilities shouldBe emptyList()
        d.state.projectedState.getPower(blocker) shouldBe 2
    }
})
