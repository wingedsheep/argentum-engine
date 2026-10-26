package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The foretold-card vocabulary (CR 702.143):
 *  - `StatePredicate.IsForetold` — a card foretold by its owner, still in exile. A foretold card
 *    that leaves exile, or one exiled by any other road, isn't foretold.
 *  - `ManaRestriction.ForetellOnly` — mana spendable on the foretell special action's {2}
 *    (CR 702.143a), from the pool and from a restricted mana source the auto-payer taps.
 *  - `ManaRestriction.SpellsWithKeywordOnly` — mana spendable on spells whose card has a keyword
 *    (foretell here), and on nothing else.
 */
class ForetoldCardVocabularyTest : FunSpec({

    val foretellInstant = card("Test Foretell Instant") {
        manaCost = "{1}{W}"
        typeLine = "Instant"
        oracleText = "You gain 1 life.\nForetell {W}"
        spell { effect = Effects.GainLife(1) }
        keywordAbility(KeywordAbility.foretell("{W}"))
    }

    val plainInstant = card("Test Plain Instant") {
        manaCost = "{1}{W}"
        typeLine = "Instant"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }

    val countForetold = card("Test Count Foretold") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "You gain 1 life for each foretold card you own in exile."
        spell {
            effect = Effects.GainLife(DynamicAmounts.count(Player.You, Zone.EXILE, GameObjectFilter.Any.foretold()))
        }
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

    val exileFromHand = card("Test Exile From Hand") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Exile target card in your hand."
        spell {
            val c = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.HAND))
            effect = Effects.Exile(c)
        }
    }

    val foretellMana = ManaRestriction.ForetellOnly
    val foretellSpellMana = ManaRestriction.SpellsWithKeywordOnly(Keyword.FORETELL)

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(foretellInstant, plainInstant, countForetold, recall, exileFromHand))
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.restrictedLeft(p: EntityId): Int =
        state.getEntity(p)!!.get<ManaPoolComponent>()!!.restrictedMana.size

    fun GameTestDriver.castCount(p: EntityId) {
        val counter = putCardInHand(p, "Test Count Foretold")
        giveMana(p, Color.WHITE, 1)
        castSpell(p, counter).error shouldBe null
        bothPass()
    }

    test("foretold cards you own in exile are counted") {
        val d = driver()
        val p = d.activePlayer!!
        repeat(2) {
            val c = d.putCardInHand(p, "Test Foretell Instant")
            d.giveMana(p, Color.WHITE, 2)
            d.submitSuccess(ForetellCard(p, c))
        }
        d.castCount(p)
        d.getLifeTotal(p) shouldBe 22
    }

    test("a card exiled face up by another effect is not foretold") {
        val d = driver()
        val p = d.activePlayer!!
        val c = d.putCardInHand(p, "Test Foretell Instant")
        val exile = d.putCardInHand(p, "Test Exile From Hand")
        d.giveMana(p, Color.BLUE, 1)
        d.castSpellWithTargets(p, exile, listOf(ChosenTarget.Card(c, p, Zone.HAND))).error shouldBe null
        d.bothPass()
        d.getExile(p).contains(c) shouldBe true
        d.castCount(p)
        d.getLifeTotal(p) shouldBe 20
    }

    test("a card exiled face down by another road is not foretold") {
        val d = driver()
        val p = d.activePlayer!!
        val c = d.putCardInExile(p, "Test Foretell Instant")
        d.replaceState(d.state.updateEntity(c) { it.with(FaceDownComponent) })
        d.castCount(p)
        d.getLifeTotal(p) shouldBe 20
    }

    test("a foretold card that leaves exile is no longer counted") {
        val d = driver()
        val p = d.activePlayer!!
        val c = d.putCardInHand(p, "Test Foretell Instant")
        d.giveMana(p, Color.WHITE, 2)
        d.submitSuccess(ForetellCard(p, c))
        val r = d.putCardInHand(p, "Test Recall From Exile")
        d.giveMana(p, Color.BLUE, 1)
        d.castSpellWithTargets(p, r, listOf(ChosenTarget.Card(c, p, Zone.EXILE))).error shouldBe null
        d.bothPass()
        d.castCount(p)
        d.getLifeTotal(p) shouldBe 20
    }

    test("foretell-only mana pays the foretell action's {2}") {
        val d = driver()
        val p = d.activePlayer!!
        val c = d.putCardInHand(p, "Test Foretell Instant")
        d.giveRestrictedMana(p, Color.BLUE, 2, foretellMana)
        d.legalActions(p).single { it.action == ForetellCard(p, c) }.affordable shouldBe true
        d.submitSuccess(ForetellCard(p, c))
        d.getExile(p).contains(c) shouldBe true
        d.restrictedLeft(p) shouldBe 0
    }

    // CR 702.143b: foretelling is a special action, not a spell — "noncreature spells only" mana
    // (a negated card-type restriction) must not read the action's missing card types as a match.
    test("noncreature-spell mana can't pay the foretell action") {
        val d = driver()
        val p = d.activePlayer!!
        val c = d.putCardInHand(p, "Test Foretell Instant")
        d.giveRestrictedMana(
            p, Color.BLUE, 2,
            ManaRestriction.CardTypeSpellsOrAbilitiesOnly(cardType = CardType.CREATURE, negated = true)
        )
        d.legalActions(p).single { it.action == ForetellCard(p, c) }.affordable shouldBe false
    }

    test("foretell-only mana can't pay for a spell, even one with foretell") {
        val d = driver()
        val p = d.activePlayer!!
        val c = d.putCardInHand(p, "Test Foretell Instant")
        d.giveRestrictedMana(p, Color.WHITE, 2, foretellMana)
        (d.castSpell(p, c).error != null) shouldBe true
    }

    test("mana for spells that have foretell pays a foretell card cast from hand") {
        val d = driver()
        val p = d.activePlayer!!
        val c = d.putCardInHand(p, "Test Foretell Instant")
        d.giveRestrictedMana(p, Color.WHITE, 2, foretellSpellMana)
        d.castSpell(p, c).error shouldBe null
        d.bothPass()
        d.getLifeTotal(p) shouldBe 21
    }

    test("mana for spells that have foretell pays a foretold card cast from exile") {
        val d = driver()
        val p = d.activePlayer!!
        val c = d.putCardInHand(p, "Test Foretell Instant")
        d.giveMana(p, Color.WHITE, 2)
        d.submitSuccess(ForetellCard(p, c))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(p))
        d.passPriority(d.getOpponent(p))
        d.giveRestrictedMana(p, Color.WHITE, 1, foretellSpellMana)
        d.castSpell(p, c).error shouldBe null
        d.bothPass()
        d.getLifeTotal(p) shouldBe 21
    }

    test("mana for spells that have foretell can't pay a spell without it, nor the foretell action") {
        val d = driver()
        val p = d.activePlayer!!
        val plain = d.putCardInHand(p, "Test Plain Instant")
        d.giveRestrictedMana(p, Color.WHITE, 2, foretellSpellMana)
        (d.castSpell(p, plain).error != null) shouldBe true
        val c = d.putCardInHand(p, "Test Foretell Instant")
        d.legalActions(p).single { it.action == ForetellCard(p, c) }.affordable shouldBe false
    }
})
