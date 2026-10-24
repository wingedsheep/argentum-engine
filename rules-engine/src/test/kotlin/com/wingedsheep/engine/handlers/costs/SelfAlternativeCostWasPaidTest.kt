package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [Conditions.SelfAlternativeCostWasPaid] — "If the {1}{B} cost was paid, …" (the Strixhaven
 * Mastery cycle). True only when the spell was cast by paying the card's own printed alternative
 * cost (CR 118.9): not for a normal cast, not for a cast through another alternative cost, and a
 * copy of the spell answers as its original did (CR 707.10 copies alternative-cost decisions;
 * Mastery ruling 2021-04-16). A permanent the spell becomes keeps the answer.
 */
class SelfAlternativeCostWasPaidTest : FunSpec({

    // Gains 5 life if the {R} cost was paid, otherwise 1.
    val mastery = card("Test Mastery") {
        manaCost = "{3}{R}"
        typeLine = "Instant"
        selfAlternativeCost = SelfAlternativeCost(ManaCost.parse("{R}"))
        keywordAbility(KeywordAbility.flashback("{R}"))
        spell {
            effect = Effects.If(Conditions.SelfAlternativeCostWasPaid, Effects.GainLife(5), Effects.GainLife(1))
        }
    }

    // "When this enters, if its {G} cost was paid, you gain 7 life."
    val masteryBear = card("Mastery Bear") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        selfAlternativeCost = SelfAlternativeCost(ManaCost.parse("{G}"))
        triggeredAbility {
            trigger = Triggers.self.enters()
            interveningIf = Conditions.SelfAlternativeCostWasPaid
            effect = Effects.GainLife(7)
        }
    }

    val copier = card("Alt Spell Copier") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val original = target(TargetFilter.SpellOnStack)
            effect = Effects.CopyTargetSpell(original)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(mastery, masteryBear, copier))
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveAll() {
        repeat(30) {
            val decision = state.pendingDecision
            if (decision is ChooseTargetsDecision) {
                submitDecision(decision.playerId, TargetsResponse(decision.id,
                    decision.legalTargets.mapValues { (_, targets) -> targets.take(1) })).error shouldBe null
            } else if (decision != null) autoResolveDecision()
            else if (stackSize > 0) bothPass()
            else return
        }
        error("stack did not finish resolving")
    }

    fun altCast(player: EntityId, card: EntityId, type: AlternativeCostType? = AlternativeCostType.SELF_ALTERNATIVE) =
        CastSpell(player, card, useAlternativeCost = true, alternativeCostType = type)

    test("paying the card's own alternative cost makes the rider true at resolution") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, mastery.name)
        d.giveMana(me, Color.RED, 1)

        d.submit(altCast(me, spell)).error shouldBe null
        d.resolveAll()

        d.getLifeTotal(me) shouldBe 25
    }

    test("a normal cast for the mana cost leaves the rider false") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, mastery.name)
        d.giveMana(me, Color.RED, 4)

        d.castSpell(me, spell).outcome shouldBe Outcome.Done
        d.resolveAll()

        d.getLifeTotal(me) shouldBe 21
    }

    test("an alternative-cost cast that doesn't name its mechanic still records the cost it paid") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, mastery.name)
        d.giveMana(me, Color.RED, 1)

        d.submit(altCast(me, spell, type = null)).error shouldBe null
        d.resolveAll()

        d.getLifeTotal(me) shouldBe 25
    }

    test("a cast through a different alternative cost (flashback) is not the card's own cost") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInGraveyard(me, mastery.name)
        d.giveMana(me, Color.RED, 1)

        d.submit(altCast(me, spell, type = AlternativeCostType.FLASHBACK)).error shouldBe null
        d.resolveAll()

        d.getLifeTotal(me) shouldBe 21
    }

    test("a free cast can't also pay the card's own alternative cost, and records none (CR 118.9a)") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, mastery.name)
        d.replaceState(d.state.updateEntity(spell) { it.with(PlayWithoutPayingCostComponent(me)) })
        d.giveMana(me, Color.RED, 1)

        d.submitExpectFailure(altCast(me, spell)).error shouldNotBe null
        d.submit(altCast(me, spell, type = null)).error shouldBe null
        d.resolveAll()

        d.getLifeTotal(me) shouldBe 21
    }

    test("a copy of a spell whose alternative cost was paid resolves as though it was paid (CR 707.10)") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, mastery.name)
        d.giveMana(me, Color.RED, 1)
        d.submit(altCast(me, spell)).error shouldBe null

        d.castSpellWithTargets(me, d.putCardInHand(me, copier.name), listOf(ChosenTarget.Spell(spell)))
            .outcome shouldBe Outcome.Done
        d.resolveAll()

        d.getLifeTotal(me) shouldBe 30
    }

    test("a copy of a normally cast spell has nothing paid") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, mastery.name)
        d.giveMana(me, Color.RED, 4)
        d.castSpell(me, spell).outcome shouldBe Outcome.Done

        d.castSpellWithTargets(me, d.putCardInHand(me, copier.name), listOf(ChosenTarget.Spell(spell)))
            .outcome shouldBe Outcome.Done
        d.resolveAll()

        d.getLifeTotal(me) shouldBe 22
    }

    test("a permanent cast for its own alternative cost carries the answer onto the battlefield") {
        val d = driver()
        val me = d.activePlayer!!
        val bear = d.putCardInHand(me, masteryBear.name)
        d.giveMana(me, Color.GREEN, 1)

        d.submit(altCast(me, bear)).error shouldBe null
        d.resolveAll()

        d.getLifeTotal(me) shouldBe 27
    }

    test("a permanent cast for its mana cost has no alternative cost paid") {
        val d = driver()
        val me = d.activePlayer!!
        val bear = d.putCardInHand(me, masteryBear.name)
        d.giveMana(me, Color.GREEN, 4)

        d.castSpell(me, bear).outcome shouldBe Outcome.Done
        d.resolveAll()

        d.getLifeTotal(me) shouldBe 20
    }
})
