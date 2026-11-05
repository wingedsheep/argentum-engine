package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A pay-or-suffer cost filter that names the resolving spell's **target** —
 * `otherThan(target)`, i.e. `Not(StatePredicate.IsEntity(..))`.
 *
 * The payment candidates are judged in the resolution's own context (its targets included), with
 * the payer as "you". A source-only context dropped the targets, so the exclusion resolved to
 * nothing and the targeted creature became a legal way to pay for itself.
 */
class PayOrSufferTargetRelativeCostTest : FunSpec({

    /** "Target creature's controller loses 2 life unless they sacrifice another creature." */
    val Exaction = card("Test Exaction") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Target creature's controller loses 2 life unless they sacrifice another creature."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.PayOrSuffer(
                cost = Costs.pay.Sacrifice(filter = GameObjectFilter.Creature.otherThan(creature)),
                suffer = Effects.LoseLife(2, target = EffectTarget.TargetController),
                player = EffectTarget.TargetController
            )
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Exaction)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20, skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.exact(target: EntityId) {
        val spell = putCardInHand(player1, "Test Exaction")
        giveMana(player1, Color.BLACK, 1)
        submit(
            CastSpell(player1, spell, targets = listOf(ChosenTarget.Permanent(target)), paymentStrategy = PaymentStrategy.AutoPay)
        ).error shouldBe null
        bothPass()
    }

    test("the target is not among the payment candidates") {
        val d = driver()
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        d.exact(giant)

        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.playerId shouldBe d.player2
        pick.options shouldContainExactly listOf(bears)
        d.submitCardSelection(d.player2, listOf(bears))

        d.getLifeTotal(d.player2) shouldBe 20
        (giant in d.state.getBattlefield()) shouldBe true
    }

    test("with only the target to offer, the cost is unpayable and the life is lost unprompted") {
        val d = driver()
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")

        d.exact(giant)

        d.pendingDecision shouldBe null
        d.getLifeTotal(d.player2) shouldBe 18
        (giant in d.state.getBattlefield()) shouldBe true
    }
})
