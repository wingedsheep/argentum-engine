package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.ApproachOfTheSecondSun
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Approach of the Second Sun (AKH #4) — {6}{W} Sorcery.
 *
 *   "If this spell was cast from your hand and you've cast another spell named Approach of the
 *    Second Sun this game, you win the game. Otherwise, put Approach of the Second Sun into its
 *    owner's library seventh from the top and you gain 7 life."
 *
 * Pins the rulings: the first Approach only counts as cast (a countered one still counts), other
 * spells don't count, the same physical card cast again is "another spell", and a short library
 * takes it on the bottom.
 */
class ApproachOfTheSecondSunScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + ApproachOfTheSecondSun)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0, startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castApproach(me: EntityId, card: EntityId = putCardInHand(me, "Approach of the Second Sun")): EntityId {
        giveMana(me, Color.WHITE, 7)
        castSpell(me, card).error shouldBe null
        return card
    }

    fun GameTestDriver.trimLibrary(playerId: EntityId, size: Int) {
        val keep = state.getLibrary(playerId).take(size)
        replaceState(state.copy(zones = state.zones + (ZoneKey(playerId, Zone.LIBRARY) to keep)))
    }

    test("the first Approach goes into the library seventh from the top and gains 7 life") {
        val d = driver()
        val me = d.activePlayer!!

        // Other spells cast this game don't count toward "another spell named Approach".
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        d.castSpellWithTargets(me, bolt, listOf(ChosenTarget.Player(d.getOpponent(me)))).error shouldBe null
        d.bothPass()

        val approach = d.castApproach(me)
        d.bothPass()

        d.state.gameOver shouldBe false
        d.getLifeTotal(me) shouldBe 27
        d.state.getLibrary(me).indexOf(approach) shouldBe 6
        d.state.getGraveyard(me) shouldNotContain approach
    }

    test("the second Approach cast from hand wins the game") {
        val d = driver()
        val me = d.activePlayer!!

        d.castApproach(me)
        d.bothPass()
        d.state.gameOver shouldBe false

        d.castApproach(me)
        d.bothPass()

        d.assertGameOver(expectedWinner = me)
    }

    test("the same card cast again is another spell named Approach") {
        val d = driver()
        val me = d.activePlayer!!

        d.trimLibrary(me, 0)
        val approach = d.castApproach(me)
        d.bothPass()
        // An empty library takes it on the bottom — it is the whole library now.
        d.state.getLibrary(me) shouldBe listOf(approach)

        // Draw it back and cast the same physical card again.
        d.replaceState(
            d.state.copy(
                zones = d.state.zones +
                    (ZoneKey(me, Zone.LIBRARY) to emptyList()) +
                    (ZoneKey(me, Zone.HAND) to d.state.getHand(me) + approach)
            )
        )
        d.castApproach(me, approach)
        d.bothPass()

        d.assertGameOver(expectedWinner = me)
    }

    test("a library of fewer than six cards takes it on the bottom") {
        val d = driver()
        val me = d.activePlayer!!

        d.trimLibrary(me, 3)
        val approach = d.castApproach(me)
        d.bothPass()

        d.state.getLibrary(me).size shouldBe 4
        d.state.getLibrary(me).last() shouldBe approach
    }

    test("a countered first Approach still counts as cast") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)

        val first = d.castApproach(me)
        d.passPriority(me)
        val counterspell = d.putCardInHand(opp, "Counterspell")
        d.giveMana(opp, Color.BLUE, 2)
        d.castSpellWithTargets(opp, counterspell, listOf(ChosenTarget.Spell(first))).error shouldBe null
        d.bothPass()
        d.state.getGraveyard(me).contains(first) shouldBe true
        d.getLifeTotal(me) shouldBe 20

        d.castApproach(me)
        d.bothPass()

        d.assertGameOver(expectedWinner = me)
    }
})
