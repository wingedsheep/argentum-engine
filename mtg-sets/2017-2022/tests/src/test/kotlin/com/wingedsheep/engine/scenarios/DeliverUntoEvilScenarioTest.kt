package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.NicolBolasGodPharaoh
import com.wingedsheep.mtg.sets.definitions.war.cards.DeliverUntoEvil
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Deliver Unto Evil — {2}{B} Sorcery (War of the Spark #85)
 *
 * "Choose up to four target cards in your graveyard. If you control a Bolas planeswalker, return
 *  those cards to your hand. Otherwise, an opponent chooses two of them. Leave the chosen cards in
 *  your graveyard and put the rest into your hand. Exile Deliver Unto Evil."
 *
 * Covers both branches of the resolution-time Bolas check, the opponent making the split, the
 * "two or fewer targets all stay" edge (2019-05-03 ruling), and the self-exile.
 */
class DeliverUntoEvilScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + DeliverUntoEvil + NicolBolasGodPharaoh)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castDeliver(targets: List<EntityId>): EntityId {
        val spell = putCardInHand(player1, "Deliver Unto Evil")
        giveMana(player1, Color.BLACK, 1)
        giveColorlessMana(player1, 2)
        castSpellWithTargets(
            player1,
            spell,
            targets.map { ChosenTarget.Card(it, player1, Zone.GRAVEYARD) }
        ).error shouldBe null
        bothPass()
        return spell
    }

    fun GameTestDriver.fourInGraveyard(): List<EntityId> = listOf(
        putCardInGraveyard(player1, "Grizzly Bears"),
        putCardInGraveyard(player1, "Centaur Courser"),
        putCardInGraveyard(player1, "Savannah Lions"),
        putCardInGraveyard(player1, "Goblin Guide")
    )

    test("with a Bolas planeswalker, all four targets return to hand and the spell is exiled") {
        val d = driver()
        val bolas = d.putPermanentOnBattlefield(d.player1, "Nicol Bolas, God-Pharaoh")
        d.addComponent(bolas, CountersComponent(mapOf(CounterType.LOYALTY to 7)))
        val cards = d.fourInGraveyard()

        val spell = d.castDeliver(cards)

        withClue("no opponent choice") { (d.pendingDecision?.playerId == d.player2) shouldBe false }
        d.getHand(d.player1) shouldContainAll cards
        withClue("Deliver Unto Evil is exiled") { d.getExile(d.player1) shouldContain spell }
    }

    test("without a Bolas planeswalker, an opponent picks two to stay and the rest go to hand") {
        val d = driver()
        val (bears, courser, lions, guide) = d.fourInGraveyard()

        val spell = d.castDeliver(listOf(bears, courser, lions, guide))

        withClue("the opponent makes the choice") { d.pendingDecision?.playerId shouldBe d.player2 }
        d.submitCardSelection(d.player2, listOf(courser, guide)).error shouldBe null

        withClue("the chosen cards stay in the graveyard") {
            d.getGraveyard(d.player1) shouldContainAll listOf(courser, guide)
        }
        withClue("the rest go to hand") { d.getHand(d.player1) shouldContainAll listOf(bears, lions) }
        withClue("Deliver Unto Evil is exiled") { d.getExile(d.player1) shouldContain spell }
    }

    test("without a Bolas planeswalker and only two targets, both stay in the graveyard") {
        val d = driver()
        val bears = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val lions = d.putCardInGraveyard(d.player1, "Savannah Lions")

        d.castDeliver(listOf(bears, lions))

        d.getGraveyard(d.player1) shouldContainAll listOf(bears, lions)
        withClue("nothing returns to hand") {
            (bears in d.getHand(d.player1) || lions in d.getHand(d.player1)) shouldBe false
        }
    }
})
