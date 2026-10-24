package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.DevastatingMastery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Devastating Mastery (STX #14): "You may pay {2}{W}{W} rather than pay this spell's mana cost.
 * If the {2}{W}{W} cost was paid, an opponent chooses up to two nonland permanents they control
 * and returns them to their owner's hand. Destroy all nonland permanents."
 */
class DevastatingMasteryScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + DevastatingMastery)
        it.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("cast for {2}{W}{W}: the opponent saves two of their own nonland permanents, the rest are destroyed") {
        val d = createDriver()
        val me = d.player1
        val opponent = d.player2
        val courser = d.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val elves = d.putCreatureOnBattlefield(opponent, "Llanowar Elves")
        val myGuide = d.putCreatureOnBattlefield(me, "Goblin Guide")
        val theirPlains = d.putLandOnBattlefield(opponent, "Plains")
        val mastery = d.putCardInHand(me, "Devastating Mastery")
        d.giveMana(me, Color.WHITE, 4)

        d.submit(CastSpell(me, mastery, useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE)).error shouldBe null
        d.bothPass()

        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe opponent
        decision.options shouldContainExactlyInAnyOrder listOf(courser, lions, elves)
        decision.minSelections shouldBe 0
        decision.maxSelections shouldBe 2
        d.submitCardSelection(opponent, listOf(courser, lions)).error shouldBe null

        d.getHand(opponent).contains(courser) shouldBe true
        d.getHand(opponent).contains(lions) shouldBe true
        d.getGraveyard(opponent).contains(elves) shouldBe true
        d.getGraveyard(me).contains(myGuide) shouldBe true
        d.findPermanent(opponent, "Plains") shouldBe theirPlains
    }

    test("cast for {2}{W}{W}{W}{W}: nobody saves anything and every nonland permanent is destroyed") {
        val d = createDriver()
        val me = d.player1
        val opponent = d.player2
        val courser = d.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val myGuide = d.putCreatureOnBattlefield(me, "Goblin Guide")
        val theirPlains = d.putLandOnBattlefield(opponent, "Plains")
        val mastery = d.putCardInHand(me, "Devastating Mastery")
        d.giveMana(me, Color.WHITE, 6)
        val opponentHand = d.getHand(opponent).size

        d.castSpell(me, mastery).error shouldBe null
        d.bothPass()

        d.pendingDecision shouldBe null
        d.getGraveyard(opponent).contains(courser) shouldBe true
        d.getGraveyard(me).contains(myGuide) shouldBe true
        d.getHand(opponent).size shouldBe opponentHand
        d.findPermanent(opponent, "Plains") shouldBe theirPlains
    }
})
