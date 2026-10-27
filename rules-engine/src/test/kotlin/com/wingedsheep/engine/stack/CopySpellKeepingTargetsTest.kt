package com.wingedsheep.engine.stack

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A bare "copy that spell" — no "you may choose new targets" — copies every decision made for the
 * original, its modes and targets included (CR 707.10), so the copy goes on the stack aimed where
 * the original was and nobody is asked anything. Only an effect that says so lets the copy's
 * controller choose new targets (CR 707.10c), which stays the default of
 * [com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect.mayChooseNewTargets].
 */
class CopySpellKeepingTargetsTest : FunSpec({

    fun copier(name: String, mayChooseNewTargets: Boolean, copies: Int = 1) = card(name) {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.casts(GameObjectFilter.Sorcery)
            effect = Effects.CopyTargetSpell(
                target = EffectTarget.TriggeringEntity,
                copies = DynamicAmount.Fixed(copies),
                mayChooseNewTargets = mayChooseNewTargets,
            )
        }
    }

    val keeper = copier("Keeping Mirror", mayChooseNewTargets = false)
    val doubleKeeper = copier("Twin Keeping Mirror", mayChooseNewTargets = false, copies = 2)
    val retargeter = copier("Retargeting Mirror", mayChooseNewTargets = true)

    val shock = card("Sorcery Shock") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(DynamicAmounts.fixed(2), t)
        }
    }

    val choice = card("Sorcery Choice") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            modal {
                mode("Target player gains 3 life") {
                    val p = target(Targets.Player)
                    effect = Effects.GainLife(3, p)
                }
                mode("Sorcery Choice deals 3 damage to any target") {
                    val t = target(Targets.Any)
                    effect = Effects.DealDamage(DynamicAmounts.fixed(3), t)
                }
            }
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(keeper, doubleKeeper, retargeter, shock, choice))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Pass until the stack is empty; any target prompt fails the keep-targets tests. */
    fun GameTestDriver.settleWithoutPrompts() {
        var guard = 0
        while (guard++ < 30 && state.stack.isNotEmpty()) {
            pendingDecision shouldBe null
            bothPass()
        }
        pendingDecision shouldBe null
    }

    test("the copy keeps the original's target and asks nothing") {
        val d = driver()
        val me = d.player1
        d.putPermanentOnBattlefield(me, "Keeping Mirror")
        d.putLandOnBattlefield(me, "Mountain")
        val bolt = d.putCardInHand(me, "Sorcery Shock")

        d.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Player(d.player2))).error shouldBe null
        d.settleWithoutPrompts()

        d.getLifeTotal(d.player2) shouldBe 16
        d.getLifeTotal(me) shouldBe 20
    }

    test("each of several copies keeps the original's target") {
        val d = driver()
        val me = d.player1
        d.putPermanentOnBattlefield(me, "Twin Keeping Mirror")
        d.putLandOnBattlefield(me, "Mountain")
        val bolt = d.putCardInHand(me, "Sorcery Shock")

        d.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Player(d.player2))).error shouldBe null
        d.settleWithoutPrompts()

        d.getLifeTotal(d.player2) shouldBe 14
    }

    test("a modal spell's copy keeps its chosen mode and that mode's target") {
        val d = driver()
        val me = d.player1
        d.putPermanentOnBattlefield(me, "Keeping Mirror")
        d.putLandOnBattlefield(me, "Mountain")
        val modal = d.putCardInHand(me, "Sorcery Choice")

        d.submit(
            CastSpell(
                playerId = me,
                cardId = modal,
                targets = listOf(ChosenTarget.Player(d.player2)),
                paymentStrategy = PaymentStrategy.AutoPay,
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(d.player2))),
            )
        ).error shouldBe null
        d.settleWithoutPrompts()

        d.getLifeTotal(d.player2) shouldBe 14
        d.getLifeTotal(me) shouldBe 20
    }

    test("without the axis the copy still lets its controller choose new targets") {
        val d = driver()
        val me = d.player1
        d.putPermanentOnBattlefield(me, "Retargeting Mirror")
        d.putLandOnBattlefield(me, "Mountain")
        val bolt = d.putCardInHand(me, "Sorcery Shock")

        d.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Player(d.player2))).error shouldBe null
        d.bothPass() // the copy trigger resolves and asks for the copy's targets
        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(decision.playerId, listOf(me)).error shouldBe null
        d.settleWithoutPrompts()

        d.getLifeTotal(me) shouldBe 18
        d.getLifeTotal(d.player2) shouldBe 18
    }
})
