package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.AshnodsAltar
import com.wingedsheep.mtg.sets.definitions.avr.cards.Griselbrand
import com.wingedsheep.mtg.sets.definitions.roe.cards.EvolvingWilds
import com.wingedsheep.mtg.sets.definitions.znr.cards.YasharnImplacableEarth
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Yasharn, Implacable Earth (ZNR #240) — "When Yasharn enters, search your library for a basic
 * Forest card and a basic Plains card, reveal those cards, put them into your hand, then shuffle.
 * Players can't pay life or sacrifice nonland permanents to cast spells or activate abilities."
 *
 * The enters trigger is a single search capped at one card per basic land type; the lock reuses
 * Angel of Jubilation's pair of prohibitions, widened to nonland permanents — lands stay sacrificeable.
 */
class YasharnImplacableEarthScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(YasharnImplacableEarth, Griselbrand, AshnodsAltar, EvolvingWilds))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.canUse(player: EntityId, sourceId: EntityId) =
        legalActions(player).any { (it.action as? ActivateAbility)?.sourceId == sourceId && it.affordable }

    fun GameTestDriver.castYasharn(player: EntityId) {
        val yasharn = putCardInHand(player, "Yasharn, Implacable Earth")
        giveMana(player, Color.GREEN, 1)
        giveMana(player, Color.WHITE, 1)
        giveColorlessMana(player, 2)
        castSpell(player, yasharn).error shouldBe null
        while (pendingDecision == null && stackSize > 0) bothPass()
    }

    test("enters: one search offers only basic Forests and Plains and finds one of each") {
        val d = driver()
        val me = d.player1
        val forestA = d.putCardOnTopOfLibrary(me, "Forest")
        val forestB = d.putCardOnTopOfLibrary(me, "Forest")
        val plains = d.putCardOnTopOfLibrary(me, "Plains")
        val handBefore = d.getHand(me).size

        d.castYasharn(me)

        val search = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        search.playerId shouldBe me
        withClue("only basic Forest and basic Plains cards are searchable") {
            search.options.toSet() shouldBe setOf(forestA, forestB, plains)
        }
        withClue("one card per basic land type") { search.maxSelections shouldBe 2 }

        d.submitCardSelection(me, listOf(forestA, plains)).error shouldBe null

        val hand = d.getHand(me)
        hand shouldContainAll listOf(forestA, plains)
        hand shouldNotContain forestB
        hand.size shouldBe handBefore + 2
    }

    test("enters: may find only one of the two") {
        val d = driver()
        val me = d.player1
        val forest = d.putCardOnTopOfLibrary(me, "Forest")
        val handBefore = d.getHand(me).size

        d.castYasharn(me)

        val search = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        search.minSelections shouldBe 0
        d.submitCardSelection(me, listOf(forest)).error shouldBe null
        d.getHand(me).size shouldBe handBefore + 1
        d.getHand(me) shouldContainAll listOf(forest)
    }

    test("no player can pay life or sacrifice a nonland permanent for a cost — lands are still fine") {
        val d = driver()
        val me = d.player1
        val gris = d.putCreatureOnBattlefield(me, "Griselbrand")
        val altar = d.putPermanentOnBattlefield(me, "Ashnod's Altar")
        d.putCreatureOnBattlefield(me, "Savannah Lions")
        val wilds = d.putLandOnBattlefield(me, "Evolving Wilds")
        d.canUse(me, gris) shouldBe true
        d.canUse(me, altar) shouldBe true

        val yasharn = d.putCreatureOnBattlefield(d.player2, "Yasharn, Implacable Earth")
        withClue("Griselbrand's life cost") { d.canUse(me, gris) shouldBe false }
        withClue("sacrificing a creature to a mana ability") { d.canUse(me, altar) shouldBe false }
        withClue("a land may still be sacrificed for a cost") { d.canUse(me, wilds) shouldBe true }

        d.moveToGraveyard(yasharn)
        d.canUse(me, gris) shouldBe true
        d.canUse(me, altar) shouldBe true
    }
})
