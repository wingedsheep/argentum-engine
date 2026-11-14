package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.RenownedComponent
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.HonoredHierarch
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Honored Hierarch (ORI #182) — {G} Creature — Human Druid 1/1.
 *
 *   Renown 1
 *   As long as this creature is renowned, it has vigilance and "{T}: Add one mana of any color."
 *
 * The first card to grant *itself* a mana ability through a conditional static
 * (`GrantActivatedAbility` scoped to `GroupFilter.source()` under `Conditions.SourceIsRenowned`).
 * Pins that the auto-payer sees the granted ability only while renowned, that vigilance follows
 * renown, and that combat damage to a player turns it on.
 */
class HonoredHierarchScenarioTest : FunSpec({

    // Islands only: no land on the battlefield can make red, so the Hierarch is the only red source.
    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(HonoredHierarch))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.hierarch(controller: EntityId, renowned: Boolean): EntityId {
        val hierarch = putCreatureOnBattlefield(controller, "Honored Hierarch")
        removeSummoningSickness(hierarch)
        if (renowned) replaceState(state.updateEntity(hierarch) { it.with(RenownedComponent) })
        return hierarch
    }

    fun GameTestDriver.plusOneCounters(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("the mana ability exists only while it is renowned") {
        val d = driver()
        val me = d.activePlayer!!
        val hierarch = d.hierarch(me, renowned = false)

        fun offersAbility() = d.legalActions(me).any { (it.action as? ActivateAbility)?.sourceId == hierarch }

        withClue("not renowned: no ability to activate") { offersAbility() shouldBe false }
        d.replaceState(d.state.updateEntity(hierarch) { it.with(RenownedComponent) })
        withClue("renowned: the granted mana ability is offered") { offersAbility() shouldBe true }
    }

    test("renowned: it taps for one mana of any color, paying for a red spell") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val hierarch = d.hierarch(me, renowned = true)

        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.castSpell(me, bolt, targets = listOf(opponent)).error shouldBe null
        withClue("the granted mana ability tapped the Hierarch") { d.isTapped(hierarch) shouldBe true }

        d.bothPass()
        d.getLifeTotal(opponent) shouldBe 17
    }

    test("not renowned: attacking taps it; dealing combat damage to a player makes it renowned") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val hierarch = d.hierarch(me, renowned = false)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(hierarch), opponent).error shouldBe null
        withClue("no vigilance before renown") { d.isTapped(hierarch) shouldBe true }

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(opponent) shouldBe 19
        withClue("renown 1 resolved") {
            d.state.getEntity(hierarch)?.has<RenownedComponent>() shouldBe true
            d.plusOneCounters(hierarch) shouldBe 1
            d.state.projectedState.getPower(hierarch) shouldBe 2
            d.state.projectedState.getToughness(hierarch) shouldBe 2
        }
    }

    test("renowned: it has vigilance, so attacking doesn't tap it") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)
        val hierarch = d.hierarch(me, renowned = true)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(hierarch), opponent).error shouldBe null
        d.isTapped(hierarch) shouldBe false
    }
})
