package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blc.cards.ScurryOfSquirrels
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Scurry of Squirrels {2}{G} — Creature — Squirrel Scout 2/2
 *   Myriad, myriad
 *   Whenever this creature deals combat damage to a player, put a +1/+1 counter on target creature
 *   you control.
 *
 * In a three-player game an attack on one opponent offers two copies (one per myriad instance)
 * attacking the other; every copy carries the combat-damage trigger, and all of them are exiled at
 * end of combat.
 */
class ScurryOfSquirrelsScenarioTest : FunSpec({

    fun GameTestDriver.tokensOf(player: EntityId): List<EntityId> =
        getCreatures(player).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    test("two myriad instances make two copies attacking the other opponent; each connects and grows the original") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ScurryOfSquirrels)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Forest" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val scurry = d.putCreatureOnBattlefield(p1, "Scurry of Squirrels").also { d.removeSummoningSickness(it) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(scurry), p2).error shouldBe null

        var guard = 0
        while (guard++ < 100 && d.state.step != Step.POSTCOMBAT_MAIN) {
            when (val decision = d.pendingDecision) {
                is YesNoDecision -> d.submitYesNo(p1, true)
                // Every combat-damage trigger grows the original Scurry.
                is ChooseTargetsDecision -> d.submitTargetSelection(decision.playerId, listOf(scurry))
                null -> {
                    if (d.state.step == Step.COMBAT_DAMAGE && d.stackSize == 0 && d.tokensOf(p1).size == 2) {
                        withClue("both copies attack the opponent who isn't defending") {
                            d.tokensOf(p1).forEach {
                                d.state.getEntity(it)!!.get<AttackingComponent>()!!.defenderId shouldBe p3
                            }
                        }
                    }
                    d.bothPass()
                }
                else -> error("unexpected decision $decision")
            }
        }

        withClue("the original hit the defending player, both copies hit the other opponent") {
            d.getLifeTotal(p2) shouldBe 18
            d.getLifeTotal(p3) shouldBe 16
        }
        withClue("three combat-damage triggers, each a +1/+1 counter on the original") {
            d.state.getEntity(scurry)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        }
        withClue("the copies are exiled at end of combat") { d.tokensOf(p1).shouldBeEmpty() }
    }

    test("two-player game: myriad has no other opponent to copy toward") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ScurryOfSquirrels)
        val (p1, p2) = d.initMultiplayer(List(2) { Deck.of("Forest" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val scurry = d.putCreatureOnBattlefield(p1, "Scurry of Squirrels").also { d.removeSummoningSickness(it) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(scurry), p2)
        var guard = 0
        while (guard++ < 20 && d.pendingDecision == null && d.stackSize > 0) d.passPriority(d.priorityPlayer!!)

        (d.pendingDecision is YesNoDecision) shouldBe false
        d.tokensOf(p1) shouldHaveSize 0
    }
})
