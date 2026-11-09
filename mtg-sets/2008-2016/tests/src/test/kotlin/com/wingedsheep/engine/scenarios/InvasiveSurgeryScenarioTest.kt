package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.InvasiveSurgery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Invasive Surgery — "Counter target sorcery spell. Delirium — If there are four or more card
 * types among cards in your graveyard, search the graveyard, hand, and library of that spell's
 * controller for any number of cards with the same name as that spell, exile those cards, then
 * that player shuffles."
 *
 * Pins what the script can't show on its own: the search runs over the countered spell's
 * controller's zones while Invasive Surgery's caster makes the pick, the countered card itself
 * (now in the graveyard) is findable, delirium is checked after the counter so your own countered
 * sorcery counts toward it (2016-04-08 ruling), and without delirium it is a plain counter.
 */
class InvasiveSurgeryScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + InvasiveSurgery)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("with delirium the caster exiles the countered sorcery and its namesakes from that player's zones") {
        val d = driver()
        val caster = d.player1
        val surgeon = d.player2
        // Creature, land, instant, artifact: four card types in the surgeon's graveyard.
        listOf("Grizzly Bears", "Island", "Shock", "Ornithopter").forEach { d.putCardInGraveyard(surgeon, it) }

        val inHand = d.putCardInHand(caster, "Divination")
        val inLibrary = d.putCardOnTopOfLibrary(caster, "Divination")
        val otherInHand = d.putCardInHand(caster, "Opt")
        // A namesake in the surgeon's own hand: only the spell's controller's zones are searched.
        val surgeonsOwn = d.putCardInHand(surgeon, "Divination")

        val divination = d.putCardInHand(caster, "Divination")
        d.giveMana(caster, Color.BLUE, 3)
        d.castSpell(caster, divination, emptyList()).error.shouldBeNull()
        val spellOnStack = d.getTopOfStack()!!
        d.passPriority(caster)

        val surgery = d.putCardInHand(surgeon, "Invasive Surgery")
        d.giveMana(surgeon, Color.BLUE, 1)
        d.castSpellWithTargets(surgeon, surgery, listOf(ChosenTarget.Spell(spellOnStack))).error.shouldBeNull()
        d.bothPass()

        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("Invasive Surgery's caster searches and picks, and the countered card is findable") {
            pick.playerId shouldBe surgeon
            pick.options shouldContainExactlyInAnyOrder listOf(divination, inHand, inLibrary)
            pick.minSelections shouldBe 0
        }
        d.submitCardSelection(surgeon, listOf(divination, inHand, inLibrary))

        val exile = d.getExile(caster)
        listOf(divination, inHand, inLibrary).forEach { exile shouldContain it }
        withClue("only cards with that name are touched") { d.getHand(caster) shouldContain otherInHand }
        d.state.getZone(ZoneKey(caster, Zone.LIBRARY)) shouldNotContain inLibrary
        withClue("nothing of the surgeon's is searched") { d.getExile(surgeon).size shouldBe 0 }
        d.getHand(surgeon) shouldContain surgeonsOwn
        d.getGraveyardCardNames(surgeon) shouldContain "Invasive Surgery"
    }

    test("delirium is checked after the counter: your own countered sorcery is the fourth type") {
        val d = driver()
        val me = d.player1
        // Creature, land, instant — three types; the countered Divination makes four.
        listOf("Grizzly Bears", "Island", "Shock").forEach { d.putCardInGraveyard(me, it) }
        val inHand = d.putCardInHand(me, "Divination")

        val divination = d.putCardInHand(me, "Divination")
        val surgery = d.putCardInHand(me, "Invasive Surgery")
        d.giveMana(me, Color.BLUE, 4)
        d.castSpell(me, divination, emptyList()).error.shouldBeNull()
        val spellOnStack = d.getTopOfStack()!!
        // The caster keeps priority and counters their own sorcery.
        d.castSpellWithTargets(me, surgery, listOf(ChosenTarget.Spell(spellOnStack))).error.shouldBeNull()
        d.bothPass()

        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.playerId shouldBe me
        pick.options shouldContainExactlyInAnyOrder listOf(divination, inHand)
        // "Any number": take only the copy in hand.
        d.submitCardSelection(me, listOf(inHand))

        d.getExile(me) shouldContain inHand
        d.getExile(me) shouldNotContain divination
        d.getGraveyard(me) shouldContain divination
    }

    test("without delirium it only counters the spell") {
        val d = driver()
        val caster = d.player1
        val surgeon = d.player2
        listOf("Grizzly Bears", "Island", "Shock").forEach { d.putCardInGraveyard(surgeon, it) }
        val inHand = d.putCardInHand(caster, "Divination")

        val divination = d.putCardInHand(caster, "Divination")
        d.giveMana(caster, Color.BLUE, 3)
        d.castSpell(caster, divination, emptyList()).error.shouldBeNull()
        val spellOnStack = d.getTopOfStack()!!
        d.passPriority(caster)

        val surgery = d.putCardInHand(surgeon, "Invasive Surgery")
        d.giveMana(surgeon, Color.BLUE, 1)
        d.castSpellWithTargets(surgeon, surgery, listOf(ChosenTarget.Spell(spellOnStack))).error.shouldBeNull()
        d.bothPass()

        d.isPaused shouldBe false
        d.getGraveyard(caster) shouldContain divination
        d.getHand(caster) shouldContain inHand
        d.getExile(caster).size shouldBe 0
    }
})
