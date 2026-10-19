package com.wingedsheep.engine.stack

import com.wingedsheep.engine.core.ChooseTargetsDecision
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
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe

/**
 * "Copy that spell" after the spell left the stack without resolving. An effect that refers to a
 * specific object uses its last-known information once it is no longer in the zone it was
 * expected to be in (CR 608.2h), so the copy is made from the spell as it last existed on the
 * stack — modes, targets and X included (CR 707.10) — whether it was countered or bounced
 * (Jin-Gitaxias, Progress Tyrant's ruling: "A copy is created even if the spell that caused the
 * ability to trigger has been countered by the time that ability resolves").
 *
 * The last-known spell is kept in [com.wingedsheep.engine.state.GameState.departedSpells]: written
 * when a spell leaves the stack without resolving, dropped when the card is put on the stack again
 * and at every turn boundary.
 */
class CopyDepartedSpellTest : FunSpec({

    // Copies sorceries only, so the instants used to counter or bounce never trigger it.
    val echoEngine = card("Echo Engine") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.casts(GameObjectFilter.Sorcery)
            effect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity)
        }
    }

    val xBlast = card("X Blast") {
        manaCost = "{X}"
        typeLine = "Sorcery"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(DynamicAmounts.xValue(), t)
        }
    }

    val spellBounce = card("Spell Bounce") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.ReturnSpellToOwnersHand()
        }
    }

    val freeCounter = card("Free Counter") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.CounterSpell()
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(echoEngine, xBlast, spellBounce, freeCounter))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Resolve the stack, keeping the original targets whenever a copy asks for new ones. */
    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 30 && (state.stack.isNotEmpty() || pendingDecision != null)) {
            when (val decision = pendingDecision) {
                null -> bothPass()
                is ChooseTargetsDecision -> submitTargetSelection(decision.playerId, emptyList()).error shouldBe null
                else -> error("unexpected decision $decision")
            }
        }
    }

    test("a countered spell is still copied, with its X and target, from last-known information") {
        val d = driver()
        val me = d.player1
        d.putPermanentOnBattlefield(me, "Echo Engine")
        repeat(2) { d.putLandOnBattlefield(me, "Mountain") }
        val blast = d.putCardInHand(me, "X Blast")
        val counter = d.putCardInHand(me, "Free Counter")

        d.castXSpell(me, blast, 2, listOf(d.player2)).error shouldBe null
        d.castSpellWithTargets(me, counter, listOf(ChosenTarget.Spell(blast))).error shouldBe null
        d.bothPass() // Free Counter resolves: X Blast leaves the stack under the copy trigger.

        d.state.stack.contains(blast) shouldBe false
        d.state.departedSpells shouldContainKey blast

        d.settle()

        d.getGraveyard(me) shouldContain blast
        d.getLifeTotal(d.player2) shouldBe 18
    }

    test("a spell returned to its owner's hand is still copied") {
        val d = driver()
        val me = d.player1
        d.putPermanentOnBattlefield(me, "Echo Engine")
        repeat(3) { d.putLandOnBattlefield(me, "Mountain") }
        val blast = d.putCardInHand(me, "X Blast")
        val bounce = d.putCardInHand(me, "Spell Bounce")

        d.castXSpell(me, blast, 3, listOf(d.player2)).error shouldBe null
        d.castSpellWithTargets(me, bounce, listOf(ChosenTarget.Spell(blast))).error shouldBe null
        d.settle()

        d.getHand(me) shouldContain blast
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("a spell that resolves leaves no last-known entry, and the entries end with the turn") {
        val d = driver()
        val me = d.player1
        repeat(2) { d.putLandOnBattlefield(me, "Mountain") }
        val blast = d.putCardInHand(me, "X Blast")
        val other = d.putCardInHand(me, "X Blast")
        val counter = d.putCardInHand(me, "Free Counter")

        d.castXSpell(me, blast, 1, listOf(d.player2)).error shouldBe null
        d.settle()
        d.state.departedSpells.shouldBeEmpty()

        d.castXSpell(me, other, 1, listOf(d.player2)).error shouldBe null
        d.castSpellWithTargets(me, counter, listOf(ChosenTarget.Spell(other))).error shouldBe null
        d.settle()
        d.state.departedSpells shouldContainKey other

        d.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        d.state.departedSpells.shouldBeEmpty()
    }

    test("a card cast again is a new spell and drops its old last-known entry") {
        val d = driver()
        val me = d.player1
        repeat(2) { d.putLandOnBattlefield(me, "Mountain") }
        val blast = d.putCardInHand(me, "X Blast")
        val bounce = d.putCardInHand(me, "Spell Bounce")

        d.castXSpell(me, blast, 1, listOf(d.player2)).error shouldBe null
        d.castSpellWithTargets(me, bounce, listOf(ChosenTarget.Spell(blast))).error shouldBe null
        d.settle()
        d.state.departedSpells shouldContainKey blast

        d.castXSpell(me, blast, 1, listOf(d.player2)).error shouldBe null
        d.state.departedSpells.shouldBeEmpty()
    }
})
