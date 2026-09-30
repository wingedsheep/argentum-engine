package com.wingedsheep.engine.handlers.effects.control

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ControlChangeDirection
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `GainControlEffect` aimed at a spell on the stack ("gain control of target spell", Invert
 * Polarity). The spell's controller is the stack object's `casterId`, so taking it must make the
 * thief the spell's "you" at resolution, put a permanent spell onto the battlefield under the
 * thief's control while its owner is unchanged, and send a resolved instant to its *owner's*
 * graveyard. A spell is not a permanent, so "an opponent gains control of a permanent from you"
 * (Zidane) must not trigger.
 */
class GainControlOfSpellTest : FunSpec({

    val StealSpell = card("Steal Spell") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Gain control of target spell."
        spell {
            val spell = target(TargetFilter.SpellOnStack)
            effect = Effects.GainControl(spell)
        }
    }

    val DrawTwo = card("Draw Two") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Draw two cards."
        spell { effect = Effects.DrawCards(2) }
    }

    val ControlWatcher = card("Control Watcher") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "Whenever an opponent gains control of a permanent from you, you gain 5 life."
        triggeredAbility {
            trigger = Triggers.a().controlChanges(ControlChangeDirection.LOST, toOpponent = true)
            effect = Effects.GainLife(5)
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(StealSpell, DrawTwo, ControlWatcher))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /**
     * The active player (the "caster", so a creature spell is castable) casts [cardName] and passes;
     * the other player (the "thief") answers with Steal Spell on it. Returns (spell, caster, thief).
     */
    fun GameTestDriver.stealActivePlayersSpell(cardName: String, color: Color, mana: Int): Triple<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val caster = state.activePlayerId!!
        val thief = getOpponent(caster)
        val theirs = putCardInHand(caster, cardName)
        val steal = putCardInHand(thief, "Steal Spell")
        giveMana(caster, color, mana)
        giveMana(thief, Color.BLUE, 1)

        submit(CastSpell(caster, theirs, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        val spellOnStack = getTopOfStack()!!
        passPriority(caster)
        submit(
            CastSpell(thief, steal, targets = listOf(ChosenTarget.Spell(spellOnStack)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        return Triple(spellOnStack, caster, thief)
    }

    test("the thief becomes the spell's controller and a permanent spell enters under their control") {
        val d = driver()
        val (courser, p2, p1) = d.stealActivePlayersSpell("Centaur Courser", Color.GREEN, 3)

        val result = d.bothPass() // Steal Spell resolves
        withClue("a control change is announced for the spell") {
            result.events.filterIsInstance<ControlChangedEvent>().single().let {
                it.permanentId shouldBe courser
                it.oldControllerId shouldBe p2
                it.newControllerId shouldBe p1
            }
        }
        d.state.getEntity(courser)!!.get<SpellOnStackComponent>()!!.casterId shouldBe p1

        d.bothPass() // Centaur Courser resolves
        withClue("the creature enters under the thief's control") {
            d.state.projectedState.getController(courser) shouldBe p1
            (courser in d.state.getBattlefield()) shouldBe true
        }
        d.state.getEntity(courser)!!.get<CardComponent>()!!.ownerId shouldBe p2
    }

    test("\"you\" in a stolen spell is the thief, and the card still goes to its owner's graveyard") {
        val d = driver()
        val (drawTwo, p2, p1) = d.stealActivePlayersSpell("Draw Two", Color.BLUE, 1)
        val p1Hand = d.state.getHand(p1).size
        val p2Hand = d.state.getHand(p2).size

        d.bothPass() // Steal Spell
        d.bothPass() // Draw Two

        d.state.getHand(p1).size shouldBe p1Hand + 2
        d.state.getHand(p2).size shouldBe p2Hand
        (drawTwo in d.state.getGraveyard(p2)) shouldBe true
    }

    test("stealing a spell is not an opponent gaining control of a permanent") {
        val d = driver()
        val p2 = d.state.activePlayerId!!
        d.putPermanentOnBattlefield(p2, "Control Watcher")
        d.stealActivePlayersSpell("Draw Two", Color.BLUE, 1)

        d.bothPass() // Steal Spell
        withClue("no control trigger went on the stack; only Draw Two remains") {
            d.stackSize shouldBe 1
        }
        d.getLifeTotal(p2) shouldBe 20
    }
})
