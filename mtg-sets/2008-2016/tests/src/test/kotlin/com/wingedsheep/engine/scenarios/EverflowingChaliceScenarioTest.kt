package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.wwk.cards.EverflowingChalice
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Everflowing Chalice (WWK #123) — {0} Artifact, multikicker {2}. "This artifact enters with a
 * charge counter on it for each time it was kicked. {T}: Add {C} for each charge counter on this
 * artifact."
 *
 * The first card to feed a multikicker *count* into an entering-counters replacement: the counter
 * count must be the number of kicker payments, not a 0/1 kicked flag.
 */
class EverflowingChaliceScenarioTest : FunSpec({

    val manaAbilityId = EverflowingChalice.activatedAbilities.single().id

    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.registerCard(EverflowingChalice)
        it.initMirrorMatch(Deck.of("Plains" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun charge(d: GameTestDriver, id: EntityId): Int =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    fun colorless(d: GameTestDriver, player: EntityId): Int =
        (d.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()).colorless

    test("kicked three times: enters with three charge counters and taps for {C}{C}{C}") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(6) { d.putLandOnBattlefield(player, "Plains") }
        val chalice = d.putCardInHand(player, "Everflowing Chalice")

        d.submit(
            CastSpell(player, chalice, declaredCostSlot = ChoiceSlot.KICKED, declaredCostTimes = 3)
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        charge(d, chalice) shouldBe 3
        d.submit(ActivateAbility(playerId = player, sourceId = chalice, abilityId = manaAbilityId))
            .outcome shouldBe Outcome.Done
        colorless(d, player) shouldBe 3
    }

    test("unkicked: enters with no counters and produces no mana") {
        val d = setup()
        val player = d.activePlayer!!
        val chalice = d.putCardInHand(player, "Everflowing Chalice")

        d.castSpell(player, chalice).outcome shouldBe Outcome.Done
        d.bothPass()

        charge(d, chalice) shouldBe 0
        d.submit(ActivateAbility(playerId = player, sourceId = chalice, abilityId = manaAbilityId))
        colorless(d, player) shouldBe 0
    }
})
