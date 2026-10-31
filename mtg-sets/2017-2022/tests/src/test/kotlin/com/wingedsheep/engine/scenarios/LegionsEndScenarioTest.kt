package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m20.cards.LegionsEnd
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Legion's End (M20 #106) — "Exile target creature an opponent controls with mana value 2 or less
 * and all other creatures that player controls with the same name as that creature. Then that
 * player reveals their hand and exiles all cards with that name from their hand and graveyard."
 */
class LegionsEndScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + LegionsEnd)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castAt(target: EntityId): Outcome {
        giveMana(player1, Color.BLACK, 2)
        val spell = putCardInHand(player1, "Legion's End")
        return castSpellWithTargets(player1, spell, listOf(ChosenTarget.Permanent(target))).outcome
    }

    test("exiles the target, its same-named creatures, and same-named cards in that player's hand and graveyard") {
        val d = driver()
        val target = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        val otherLions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        val otherName = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val myLions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val handLions = d.putCardInHand(d.player2, "Savannah Lions")
        val handOther = d.putCardInHand(d.player2, "Centaur Courser")
        val graveLions = d.putCardInGraveyard(d.player2, "Savannah Lions")
        val myGraveLions = d.putCardInGraveyard(d.player1, "Savannah Lions")

        withClue("casting Legion's End") { d.castAt(target) shouldBe Outcome.Done }
        d.bothPass()

        d.getExile(d.player2) shouldContainAll listOf(target, otherLions, handLions, graveLions)
        withClue("other names and the caster's own Lions are untouched") {
            d.findPermanent(d.player2, "Centaur Courser") shouldBe otherName
            d.findPermanent(d.player1, "Savannah Lions") shouldBe myLions
            d.getHand(d.player2) shouldContain handOther
            d.getGraveyard(d.player1) shouldContain myGraveLions
            d.getExile(d.player1) shouldNotContain myGraveLions
        }
        d.getGraveyardCardNames(d.player1) shouldContain "Legion's End"
    }

    test("can't target a creature with mana value 3 or more, or one you control") {
        val d = driver()
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val mine = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        d.castAt(courser) shouldNotBe Outcome.Done
        d.castAt(mine) shouldNotBe Outcome.Done
    }
})
