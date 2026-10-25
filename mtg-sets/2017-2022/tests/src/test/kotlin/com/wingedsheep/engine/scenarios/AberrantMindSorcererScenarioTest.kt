package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DieRolledEvent
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.AberrantMindSorcerer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Aberrant Mind Sorcerer: the ETB targets an instant or sorcery card in your graveyard, then a d20
 * picks the row. 1—9 is an optional put-on-top (a "may" inside a results-table branch); 10—20 is a
 * mandatory return to hand.
 */
class AberrantMindSorcererScenarioTest : FunSpec({

    /** First seed whose d20 comes up [natural]. */
    fun seedFor(natural: Int): Long = (0L..10_000L).first { GameRng.seeded(it).nextInt(20).first + 1 == natural }

    data class Setup(val d: GameTestDriver, val player: EntityId, val bolt: EntityId)

    /** Cast the Sorcerer, target the Bolt in the graveyard, force the d20, and resolve the trigger. */
    fun castAndRoll(natural: Int): Setup {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(AberrantMindSorcerer))
            it.initMirrorMatch(Deck.of("Island" to 40))
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = d.activePlayer!!
        val bolt = d.putCardInGraveyard(player, "Lightning Bolt")
        val sorcerer = d.putCardInHand(player, "Aberrant Mind Sorcerer")
        d.giveMana(player, Color.BLUE, 1)
        d.giveColorlessMana(player, 4)
        d.castSpell(player, sorcerer)
        d.bothPass()
        if (d.pendingDecision is ChooseTargetsDecision) d.submitTargetSelection(player, listOf(bolt))
        d.replaceState(d.state.copy(rng = GameRng.seeded(seedFor(natural))))
        d.bothPass().events.filterIsInstance<DieRolledEvent>().single().result shouldBe natural
        return Setup(d, player, bolt)
    }

    test("10-20 returns the targeted card to your hand without asking") {
        val (d, player, bolt) = castAndRoll(natural = 14)
        d.pendingDecision shouldBe null
        (bolt in d.getHand(player)) shouldBe true
        (bolt in d.getGraveyard(player)) shouldBe false
    }

    test("1-9 lets you put the targeted card on top of your library") {
        val (d, player, bolt) = castAndRoll(natural = 5)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(player, true)
        d.state.getLibrary(player).first() shouldBe bolt
        (bolt in d.getGraveyard(player)) shouldBe false
    }

    test("1-9 declined leaves the card in the graveyard") {
        val (d, player, bolt) = castAndRoll(natural = 9)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(player, false)
        (bolt in d.getGraveyard(player)) shouldBe true
        (bolt in d.getHand(player)) shouldBe false
    }
})
