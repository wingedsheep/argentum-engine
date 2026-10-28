package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c15.cards.BladeOfSelves
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Blade of Selves {2} — Artifact — Equipment
 *   Equipped creature has myriad.
 *   Equip {4}
 *
 * The equipped creature gets the engine's myriad trigger; the copy it makes is a plain copy of the
 * creature — no Equipment, so no myriad of its own.
 */
class BladeOfSelvesScenarioTest : FunSpec({

    fun GameTestDriver.tokensOf(player: EntityId): List<EntityId> =
        getCreatures(player).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    fun GameTestDriver.attach(equipment: EntityId, creature: EntityId) {
        replaceState(
            state
                .updateEntity(equipment) { it.with(AttachedToComponent(creature)) }
                .updateEntity(creature) { it.with(AttachmentsComponent(listOf(equipment))) }
        )
    }

    fun setup(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + BladeOfSelves)
        val players = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to players
    }

    test("the equipped creature's attack offers a copy attacking the other opponent") {
        val (d, p) = setup()
        val (p1, p2, p3) = p
        val blade = d.putPermanentOnBattlefield(p1, "Blade of Selves")
        val courser = d.putCreatureOnBattlefield(p1, "Centaur Courser").also { d.removeSummoningSickness(it) }
        d.attach(blade, courser)
        d.state.projectedState.hasKeyword(courser, Keyword.MYRIAD) shouldBe true

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(courser), p2).error shouldBe null
        var guard = 0
        while (guard++ < 30 && d.tokensOf(p1).isEmpty()) {
            when (val decision = d.pendingDecision) {
                is YesNoDecision -> d.submitYesNo(p1, true)
                null -> d.passPriority(d.priorityPlayer!!)
                else -> error("unexpected decision $decision")
            }
        }

        val token = d.tokensOf(p1).single()
        d.getCardName(token) shouldBe "Centaur Courser"
        d.state.getEntity(token)!!.get<AttackingComponent>()!!.defenderId shouldBe p3
        withClue("the copy isn't equipped, so it has no myriad") {
            d.state.projectedState.hasKeyword(token, Keyword.MYRIAD) shouldBe false
        }

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("the copy is exiled at end of combat") { d.tokensOf(p1).shouldBeEmpty() }
    }

    test("an unequipped creature has no myriad") {
        val (d, p) = setup()
        val (p1, p2, _) = p
        d.putPermanentOnBattlefield(p1, "Blade of Selves")
        val courser = d.putCreatureOnBattlefield(p1, "Centaur Courser").also { d.removeSummoningSickness(it) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(courser), p2).error shouldBe null
        var guard = 0
        while (guard++ < 20 && d.pendingDecision == null && d.stackSize > 0) d.passPriority(d.priorityPlayer!!)

        (d.pendingDecision is YesNoDecision) shouldBe false
        d.tokensOf(p1).shouldBeEmpty()
    }
})
