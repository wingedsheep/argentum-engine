package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.combat.MustAttackGroupExecutor
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.MustAttackDefender
import com.wingedsheep.sdk.scripting.effects.MustAttackGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Attacks a player other than you if able" (Kardur, Doomscourge; goad's second half) in a pod.
 * The engine drops the player the requirement rules out from each bound attacker's
 * `attackTargetsByAttacker`, so the AI — which picks from that list — no longer aims a bound
 * creature at that player and gets its whole declaration rejected, however tempting their life
 * total.
 */
class AttackOtherThanRequirementAiTest : FunSpec({

    test("a creature bound by Kardur's requirement attacks another player, not the low-life Kardur seat") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        val (me, kardurSeat, other) = driver.initMultiplayer(decks = List(3) { Deck.of("Plains" to 40) })
        val giant = driver.putCreatureOnBattlefield(me, "Hill Giant").also { driver.removeSummoningSickness(it) }
        driver.replaceState(driver.state.withLifeTotal(kardurSeat, 2))
        val effect = MustAttackGroupEffect(
            GroupFilter.AllCreaturesOpponentsControl, MustAttackDefender.PlayerOtherThanYou, Duration.UntilYourNextTurn
        )
        driver.replaceState(
            MustAttackGroupExecutor(driver.services.predicateEvaluator.amounts).execute(driver.state, effect, EffectContext(sourceId = null, controllerId = kardurSeat)).state
        )
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val declare: LegalAction = driver.legalActions(me).single { it.actionType == "DeclareAttackers" }
        declare.attackTargetsByAttacker.orEmpty()[giant].orEmpty() shouldNotContain kardurSeat

        val registry = CardRegistry().apply { register(TestCards.all) }
        val advisor = CombatAdvisor(GameSimulator(registry), AIPlayer.defaultEvaluator())
        val plan = advisor.chooseAttackers(driver.state, declare, me) as DeclareAttackers
        plan.attackers shouldBe mapOf(giant to other)
        driver.submitSuccess(plan)
    }
})
