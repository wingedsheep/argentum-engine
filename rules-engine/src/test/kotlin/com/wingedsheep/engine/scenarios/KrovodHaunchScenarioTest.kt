package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.KrovodHaunch
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Krovod Haunch (MKM #21) — {W} Artifact — Food Equipment.
 *
 * "Equipped creature gets +2/+0.
 *  {2}, {T}, Sacrifice this Equipment: You gain 3 life.
 *  When this Equipment is put into a graveyard from the battlefield, you may pay {1}{W}. If you
 *  do, create two 1/1 white Dog creature tokens.
 *  Equip {2}"
 *
 * The shape worth pinning is that the two halves *chain*: the sacrifice ability's own cost puts
 * the Haunch into the graveyard from the battlefield, which is exactly what the second ability
 * triggers on — so cashing it in for life also offers the Dogs. The trigger is
 * [com.wingedsheep.sdk.dsl.Triggers.Dies] (battlefield → graveyard, SELF binding) rather than
 * anything sacrifice-specific, and the optional {1}{W} is a resolution-time `MayPayManaEffect`
 * (CR 603.12 style "you may pay … if you do"), not an additional cost — so declining still leaves
 * the life gain intact, and the Haunch already sitting in the graveyard when the trigger resolves
 * is harmless.
 */
class KrovodHaunchScenarioTest : FunSpec({

    val equipAbilityId = KrovodHaunch.activatedAbilities.single { it.isEquipAbility }.id
    val sacAbilityId = KrovodHaunch.activatedAbilities.single { !it.isEquipAbility }.id

    fun GameTestDriver.dogCount(playerId: EntityId): Int =
        getPermanents(playerId).count { state.getEntity(it)?.get<CardComponent>()?.name == "Dog Token" }

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20, skipMulligans = true)
    }

    test("equipped creature gets +2/+0") {
        val d = setup()
        val p1 = d.activePlayer!!

        val courser = d.putCreatureOnBattlefield(p1, "Centaur Courser") // 3/3
        d.removeSummoningSickness(courser)
        val haunch = d.putPermanentOnBattlefield(p1, "Krovod Haunch")

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveColorlessMana(p1, 2)
        d.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = haunch,
                abilityId = equipAbilityId,
                targets = listOf(ChosenTarget.Permanent(courser))
            )
        ).isSuccess shouldBe true
        d.bothPass()
        d.state.getEntity(haunch)?.get<AttachedToComponent>()?.targetId shouldBe courser

        d.state.projectedState.getPower(courser) shouldBe 5
        d.state.projectedState.getToughness(courser) shouldBe 3
    }

    test("sacrificing for life gains 3 and offers the Dogs; paying {1}{W} makes two of them") {
        val d = setup()
        val p1 = d.activePlayer!!

        val haunch = d.putPermanentOnBattlefield(p1, "Krovod Haunch")
        d.removeSummoningSickness(haunch)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveColorlessMana(p1, 2)                        // {2} for the sacrifice ability
        repeat(2) { d.putLandOnBattlefield(p1, "Plains") } // {1}{W} for the optional payment

        d.submit(
            ActivateAbility(playerId = p1, sourceId = haunch, abilityId = sacAbilityId)
        ).isSuccess shouldBe true

        var guard = 0
        while (guard++ < 12) {
            while (d.pendingDecision == null && d.state.stack.isNotEmpty()) d.bothPass()
            when (d.pendingDecision) {
                is YesNoDecision -> d.submitYesNo(p1, true).error shouldBe null
                is SelectManaSourcesDecision -> d.submitManaAutoPayOrDecline(p1, true)
                else -> break
            }
        }
        while (d.pendingDecision == null && d.state.stack.isNotEmpty()) d.bothPass()

        d.getLifeTotal(p1) shouldBe 23
        d.dogCount(p1) shouldBe 2
    }

    test("declining the {1}{W} makes no Dogs but still gains the life") {
        val d = setup()
        val p1 = d.activePlayer!!

        val haunch = d.putPermanentOnBattlefield(p1, "Krovod Haunch")
        d.removeSummoningSickness(haunch)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveColorlessMana(p1, 2)
        repeat(2) { d.putLandOnBattlefield(p1, "Plains") }

        d.submit(
            ActivateAbility(playerId = p1, sourceId = haunch, abilityId = sacAbilityId)
        ).isSuccess shouldBe true

        var guard = 0
        while (guard++ < 12) {
            while (d.pendingDecision == null && d.state.stack.isNotEmpty()) d.bothPass()
            when (d.pendingDecision) {
                is YesNoDecision -> {
                    d.submitYesNo(p1, false).error shouldBe null
                    break
                }
                else -> break
            }
        }
        while (d.pendingDecision == null && d.state.stack.isNotEmpty()) d.bothPass()

        d.getLifeTotal(p1) shouldBe 23
        d.dogCount(p1) shouldBe 0
    }
})
