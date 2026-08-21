package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.MirrorGolem
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mirror Golem (MRD #208) — "Imprint — When this creature enters, you may exile target card from a
 * graveyard. This creature has protection from each of the exiled card's card types."
 *
 * The protection is derived from the linked-exile pile at projection time, which is what these tests
 * pin. The three interesting states are: an imprint whose card has one type, an imprint whose card
 * has *two* (an artifact creature grants protection from both), and no imprint at all — where the
 * dynamic read must grant nothing rather than fail open.
 */
class MirrorGolemScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + MirrorGolem)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /**
     * Cast Mirror Golem from hand so the imprint trigger fires, choose [graveyardCard] as its
     * target (when given), and answer the "you may" with [accept]. Returns the Golem on the
     * battlefield.
     */
    fun GameTestDriver.castGolem(graveyardCard: EntityId, accept: Boolean): EntityId {
        val inHand = putCardInHand(player1, "Mirror Golem")
        giveColorlessMana(player1, 6)
        castSpell(player1, inHand).error shouldBe null

        // Resolve the creature, then work through the imprint trigger: the target is chosen as the
        // ability goes on the stack, the "you may" is answered when it resolves.
        repeat(12) {
            when (state.pendingDecision) {
                null -> bothPass()
                is ChooseTargetsDecision ->
                    submitTargetSelection(player1, listOf(graveyardCard)).error shouldBe null
                is YesNoDecision -> submitYesNo(player1, accept).error shouldBe null
                else -> error("unexpected decision ${state.pendingDecision}")
            }
        }
        return findPermanent(player1, "Mirror Golem")!!
    }

    fun GameTestDriver.hasProtectionFrom(golem: EntityId, cardType: String): Boolean =
        state.projectedState.hasKeyword(golem, "PROTECTION_FROM_CARDTYPE_$cardType")

    test("an imprinted instant card grants protection from instants and nothing else") {
        val d = driver()
        val bolt = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val golem = d.castGolem(bolt, accept = true)

        withClue("the Bolt was exiled with the Golem") {
            d.getExileCardNames(d.player1) shouldBe listOf("Lightning Bolt")
        }
        withClue("protection from instants — the exiled card's only card type") {
            d.hasProtectionFrom(golem, "INSTANT") shouldBe true
        }
        withClue("and from nothing else: the types come from the card, not from a blanket grant") {
            d.hasProtectionFrom(golem, "CREATURE") shouldBe false
            d.hasProtectionFrom(golem, "ARTIFACT") shouldBe false
            d.hasProtectionFrom(golem, "SORCERY") shouldBe false
        }
    }

    test("an imprinted artifact creature card grants protection from both of its card types") {
        val d = driver()
        val artifactCreature = d.putCardInGraveyard(d.player1, "Artifact Creature")
        val golem = d.castGolem(artifactCreature, accept = true)

        withClue("\"each of the exiled card's card types\" is plural on purpose") {
            d.hasProtectionFrom(golem, "ARTIFACT") shouldBe true
            d.hasProtectionFrom(golem, "CREATURE") shouldBe true
        }
        withClue("still nothing it didn't print") {
            d.hasProtectionFrom(golem, "INSTANT") shouldBe false
        }
    }

    test("declining the imprint leaves the Golem with no protection at all") {
        val d = driver()
        val bolt = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val golem = d.castGolem(bolt, accept = false)

        withClue("nothing was exiled") {
            d.getExileCardNames(d.player1) shouldBe emptyList()
        }
        withClue("an empty pile grants nothing — the dynamic read fails closed") {
            listOf("INSTANT", "CREATURE", "ARTIFACT", "LAND", "SORCERY", "ENCHANTMENT")
                .filter { d.hasProtectionFrom(golem, it) } shouldBe emptyList()
        }
    }
})
