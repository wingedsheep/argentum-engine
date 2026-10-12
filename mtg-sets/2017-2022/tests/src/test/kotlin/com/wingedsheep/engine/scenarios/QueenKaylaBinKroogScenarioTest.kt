package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.QueenKaylaBinKroog
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Queen Kayla bin-Kroog (BRO #218).
 *
 * "{4}, {T}: Discard all the cards in your hand, then draw that many cards. You may choose an
 *  artifact or creature card with mana value 1 you discarded this way, then do the same for
 *  artifact or creature cards with mana values 2 and 3. Return those cards to the battlefield.
 *  Activate only as a sorcery."
 *
 * Proves the composition: the draw count is the discarded hand size, each pick is restricted to
 * its own mana value and to artifact/creature cards, a pick can be declined, and the three picks
 * are folded into one return to the battlefield.
 */
class QueenKaylaBinKroogScenarioTest : FunSpec({

    val abilityId = QueenKaylaBinKroog.activatedAbilities[0].id

    val oneDrop = card("Kayla Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }
    val twoDrop = card("Kayla Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val otherTwoDrop = card("Kayla Test Gadget") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }
    val threeDrop = card("Kayla Test Ogre") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Ogre"
        power = 3
        toughness = 3
    }
    val oneMvSorcery = card("Kayla Test Sorcery") {
        manaCost = "{R}"
        typeLine = "Sorcery"
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(QueenKaylaBinKroog)
        listOf(oneDrop, twoDrop, otherTwoDrop, threeDrop, oneMvSorcery).forEach { driver.registerCard(it) }
        driver.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("discards the hand, draws that many, and returns one chosen card per mana value 1-3") {
        val driver = newDriver()
        val me = driver.player1

        val kayla = driver.putPermanentOnBattlefield(me, "Queen Kayla bin-Kroog")
        driver.removeSummoningSickness(kayla)

        // Empty the opening hand so the discarded pool is exactly the cards below.
        driver.state.getZone(me, Zone.HAND).toList().forEach { driver.moveToGraveyard(it) }

        val trinket = driver.putCardInHand(me, "Kayla Test Trinket")
        val bear = driver.putCardInHand(me, "Kayla Test Bear")
        val gadget = driver.putCardInHand(me, "Kayla Test Gadget")
        val ogre = driver.putCardInHand(me, "Kayla Test Ogre")
        val sorcery = driver.putCardInHand(me, "Kayla Test Sorcery")
        val discardedPool = listOf(trinket, bear, gadget, ogre, sorcery)

        driver.giveColorlessMana(me, 4)
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = kayla, abilityId = abilityId))
        driver.bothPass()

        val offered = mutableListOf<List<EntityId>>()
        // Mana value 1: the trinket is offered, the 1-mana sorcery is not.
        // Mana value 2: both two-drops are offered; take the Bear.
        // Mana value 3: decline the Ogre.
        val picks = listOf(listOf(trinket), listOf(bear), emptyList())
        for (pick in picks) {
            val decision = driver.pendingDecision as SelectCardsDecision
            offered += decision.options
            driver.submitCardSelection(me, pick)
        }

        offered[0] shouldContainExactlyInAnyOrder listOf(trinket)
        offered[1] shouldContainExactlyInAnyOrder listOf(bear, gadget)
        offered[2] shouldContainExactlyInAnyOrder listOf(ogre)

        driver.state.getBattlefield() shouldContain trinket
        driver.state.getBattlefield() shouldContain bear
        driver.state.getBattlefield() shouldNotContain gadget
        driver.state.getBattlefield() shouldNotContain ogre

        val graveyard = driver.state.getZone(me, Zone.GRAVEYARD)
        graveyard shouldContain gadget
        graveyard shouldContain ogre
        graveyard shouldContain sorcery

        val hand = driver.state.getZone(me, Zone.HAND)
        hand.size shouldBe discardedPool.size
        discardedPool.forEach { hand shouldNotContain it }
    }
})
