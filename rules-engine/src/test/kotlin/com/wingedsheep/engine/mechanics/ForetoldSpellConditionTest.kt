package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.identity.ForetoldComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `Conditions.WasForetold` (CR 702.143c): "a spell that was foretold" is one that was a foretold
 * card in exile before it was cast. The engine stamps `ChoiceSlot.FORETOLD` as the spell is cast,
 * read by the resolving spell and by the permanent a permanent spell becomes. Also pins an {X} in a
 * foretell cost (Starnheim Unleashed's {X}{X}{W}): it is announced and paid like a printed {X}, and
 * the resolving spell reads that X.
 */
class ForetoldSpellConditionTest : FunSpec({

    val foretoldGain = card("Test Foretold Gain") {
        manaCost = "{2}{W}"
        typeLine = "Instant"
        oracleText = "You gain 1 life. If this spell was foretold, you gain 5 life.\nForetell {W}"
        spell {
            effect = Effects.GainLife(1) then Effects.If(Conditions.WasForetold, Effects.GainLife(5))
        }
        keywordAbility(KeywordAbility.foretell("{W}"))
    }

    val foretoldX = card("Test Foretold X") {
        manaCost = "{3}{W}"
        typeLine = "Instant"
        oracleText = "You gain 1 life. If this spell was foretold, you gain X life instead.\nForetell {X}{X}{W}"
        spell {
            effect = Effects.If(
                Conditions.WasForetold,
                then = Effects.GainLife(DynamicAmounts.xValue()),
                otherwise = Effects.GainLife(1),
            )
        }
        keywordAbility(KeywordAbility.foretell("{X}{X}{W}"))
    }

    val foretoldCreature = card("Test Foretold Creature") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Angel"
        power = 2
        toughness = 2
        oracleText = "When this creature enters, if it was foretold, you gain 3 life.\nForetell {W}"
        triggeredAbility {
            trigger = Triggers.self.enters()
            interveningIf = Conditions.WasForetold
            effect = Effects.GainLife(3)
        }
        keywordAbility(KeywordAbility.foretell("{W}"))
    }

    val recall = card("Test Recall From Exile") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Return target card you own in exile to your hand."
        spell {
            val c = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.EXILE))
            effect = Effects.ReturnToHand(c)
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(foretoldGain, foretoldX, foretoldCreature, recall))
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Foretells [name] from [player]'s hand, then advances to the opponent's main phase. */
    fun GameTestDriver.foretellAndPassTurn(player: EntityId, name: String): EntityId {
        val card = putCardInHand(player, name)
        giveMana(player, Color.WHITE, 2)
        submitSuccess(ForetellCard(player, card))
        getExile(player).contains(card) shouldBe true
        passPriorityUntil(Step.PRECOMBAT_MAIN, getOpponent(player))
        return card
    }

    fun GameTestDriver.resolve() {
        bothPass()
    }

    test("a spell cast from hand was not foretold") {
        val d = driver()
        val p = d.activePlayer!!
        val card = d.putCardInHand(p, "Test Foretold Gain")
        d.giveMana(p, Color.WHITE, 3)
        d.castSpell(p, card).error shouldBe null
        d.resolve()
        d.getLifeTotal(p) shouldBe 21
    }

    test("a foretold card cast from exile for its foretell cost was foretold") {
        val d = driver()
        val p = d.activePlayer!!
        val card = d.foretellAndPassTurn(p, "Test Foretold Gain")
        d.passPriority(d.getOpponent(p)) // an instant, cast on the opponent's turn
        d.giveMana(p, Color.WHITE, 1)
        d.castSpell(p, card).error shouldBe null
        d.resolve()
        d.getLifeTotal(p) shouldBe 26
    }

    test("an {X} in the foretell cost is announced, paid and read by the resolving spell") {
        val d = driver()
        val p = d.activePlayer!!
        val card = d.foretellAndPassTurn(p, "Test Foretold X")
        d.passPriority(d.getOpponent(p))
        // {X}{X}{W} with X = 3 costs seven mana; give exactly that.
        d.giveMana(p, Color.WHITE, 7)
        d.castXSpell(p, card, xValue = 3).error shouldBe null
        d.resolve()
        d.getLifeTotal(p) shouldBe 23
    }

    test("an {X} foretell cast with too little mana for the announced X is rejected") {
        val d = driver()
        val p = d.activePlayer!!
        val card = d.foretellAndPassTurn(p, "Test Foretold X")
        d.passPriority(d.getOpponent(p))
        d.giveMana(p, Color.WHITE, 6)
        (d.castXSpell(p, card, xValue = 3).error != null) shouldBe true
    }

    test("the permanent a foretold spell becomes remembers it was foretold") {
        val d = driver()
        val p = d.activePlayer!!
        val card = d.foretellAndPassTurn(p, "Test Foretold Creature")
        // A creature is cast at sorcery speed: wait for the foreteller's own next main phase.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, p)
        d.giveMana(p, Color.WHITE, 1)
        d.castSpell(p, card).error shouldBe null
        d.resolve() // creature spell resolves; the enters trigger goes on the stack
        d.resolve()
        d.getLifeTotal(p) shouldBe 23
    }

    test("the same creature cast from hand is not foretold") {
        val d = driver()
        val p = d.activePlayer!!
        val card = d.putCardInHand(p, "Test Foretold Creature")
        d.giveMana(p, Color.WHITE, 3)
        d.castSpell(p, card).error shouldBe null
        d.resolve()
        d.resolve()
        d.getLifeTotal(p) shouldBe 20
    }

    // CR 400.7 / 702.143c: a foretold card that leaves exile is a new object, no longer foretold —
    // if it is exiled again by some other route, casting it must not read "was foretold".
    test("a foretold card that leaves exile stops being foretold") {
        val d = driver()
        val p = d.activePlayer!!
        val card = d.putCardInHand(p, "Test Foretold Gain")
        d.giveMana(p, Color.WHITE, 2)
        d.submitSuccess(ForetellCard(p, card))
        val recall = d.putCardInHand(p, "Test Recall From Exile")
        d.giveMana(p, Color.BLUE, 1)
        d.castSpellWithTargets(p, recall, listOf(ChosenTarget.Card(card, p, Zone.EXILE))).error shouldBe null
        d.resolve()
        d.state.getHand(p).contains(card) shouldBe true
        d.state.getEntity(card)!!.has<ForetoldComponent>() shouldBe false
        d.state.getEntity(card)!!.has<PlayWithFixedAlternativeManaCostComponent>() shouldBe false
    }

    test("a foretold spell does not carry the foretold marker past its cast") {
        val d = driver()
        val p = d.activePlayer!!
        val card = d.foretellAndPassTurn(p, "Test Foretold Gain")
        d.passPriority(d.getOpponent(p))
        d.giveMana(p, Color.WHITE, 1)
        d.castSpell(p, card).error shouldBe null
        d.resolve()
        d.state.getGraveyard(p).contains(card) shouldBe true
        d.state.getEntity(card)!!.has<ForetoldComponent>() shouldBe false
    }
})
