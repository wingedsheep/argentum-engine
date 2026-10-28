package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdc.cards.IronwillForger
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ironwill Forger {3}{W} — Creature — Orc Artificer 3/3
 *   Lieutenant — At the beginning of combat on your turn, if you control your commander, target
 *   nonlegendary creature you control gains myriad until end of turn.
 *
 * The granted myriad is the engine's own: attacking one opponent of three offers a copy attacking
 * each of the other two, exiled at end of combat. No commander, no trigger.
 */
class IronwillForgerScenarioTest : FunSpec({

    fun GameTestDriver.tokensOf(player: EntityId): List<EntityId> =
        getCreatures(player).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    fun setup(withCommander: Boolean): Triple<GameTestDriver, List<EntityId>, Pair<EntityId, EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + IronwillForger)
        val players = d.initMultiplayer(List(4) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p1 = players[0]
        d.putCreatureOnBattlefield(p1, "Ironwill Forger").also { d.removeSummoningSickness(it) }
        val commander = d.putCreatureOnBattlefield(p1, "Centaur Courser").also { d.removeSummoningSickness(it) }
        if (withCommander) {
            d.replaceState(d.state.updateEntity(commander) { it.with(CommanderComponent(ownerId = p1)) })
        }
        val lions = d.putCreatureOnBattlefield(p1, "Savannah Lions").also { d.removeSummoningSickness(it) }
        return Triple(d, players, commander to lions)
    }

    test("with your commander out, the target gains myriad and copies attack the other two opponents") {
        val (d, p, creatures) = setup(withCommander = true)
        val (p1, p2, p3, p4) = p
        val lions = creatures.second

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        val target = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        target.playerId shouldBe p1
        d.submitTargetSelection(p1, listOf(lions)).error shouldBe null
        var guard = 0
        while (guard++ < 20 && d.pendingDecision == null && d.stackSize > 0) d.passPriority(d.priorityPlayer!!)
        withClue("Savannah Lions has myriad until end of turn") {
            d.state.projectedState.hasKeyword(lions, Keyword.MYRIAD) shouldBe true
        }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(lions), p2).error shouldBe null
        guard = 0
        while (guard++ < 50 && d.tokensOf(p1).size < 2) {
            when (val decision = d.pendingDecision) {
                is YesNoDecision -> d.submitYesNo(p1, true)
                null -> d.passPriority(d.priorityPlayer!!)
                else -> error("unexpected decision $decision")
            }
        }

        val tokens = d.tokensOf(p1)
        tokens shouldHaveSize 2
        tokens.map { d.state.getEntity(it)!!.get<AttackingComponent>()!!.defenderId } shouldContainExactlyInAnyOrder
            listOf(p3, p4)
        tokens.forEach { d.getCardName(it) shouldBe "Savannah Lions" }

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("the copies are exiled at end of combat") { d.tokensOf(p1).shouldBeEmpty() }
    }

    test("without your commander on the battlefield nothing gains myriad") {
        val (d, _, creatures) = setup(withCommander = false)

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        withClue("the intervening-if keeps the trigger off the stack") {
            (d.pendingDecision is ChooseTargetsDecision) shouldBe false
        }
        d.state.projectedState.hasKeyword(creatures.second, Keyword.MYRIAD) shouldBe false
    }
})
