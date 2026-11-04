package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.MournersShield
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mourner's Shield (MRD #209) — "Imprint — When this artifact enters, you may exile target card
 * from a graveyard. {2}, {T}: Prevent all damage that would be dealt this turn by a source of your
 * choice that shares a color with the exiled card."
 *
 * Two things are load-bearing and neither was expressible before:
 *  - the colour clause is an *eligibility filter on the choice*, read relative to the Shield, so
 *    only sources sharing a colour with the imprinted card are offered at all;
 *  - the prevention has **no recipient clause**. Every other "source of your choice" card in the
 *    corpus protects *you* (Samite Ministration, the Circles of Protection); this one stops the
 *    chosen source's damage to anything. The tests therefore aim the chosen source at the
 *    *opponent* — the case a recipient-scoped shield would get wrong.
 *
 * The imprint is always Counterspell ({U}{U}, blue) and the chosen source is a blue Prodigal
 * Sorcerer, with a red Goblin Guide on the same battlefield as the ineligible control.
 */
class MournersShieldScenarioTest : FunSpec({

    val shieldAbility = MournersShield.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + MournersShield)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Pass priority only while the stack is busy, answering the imprint's decisions on the way. */
    fun GameTestDriver.settle(accept: Boolean, graveyardCard: EntityId?) {
        repeat(20) {
            when (val decision = state.pendingDecision) {
                null -> if (state.stack.isNotEmpty()) bothPass() else return
                is ChooseTargetsDecision ->
                    submitTargetSelection(player1, listOfNotNull(graveyardCard)).error shouldBe null
                is YesNoDecision -> submitYesNo(player1, accept).error shouldBe null
                else -> error("unexpected decision $decision")
            }
        }
    }

    /** Board: a blue pinger and a red creature, both player 1's, plus a Shield that imprinted or not. */
    fun GameTestDriver.setUpBoard(accept: Boolean): Triple<EntityId, EntityId, EntityId> {
        val counterspell = putCardInGraveyard(player1, "Counterspell")
        val sorcerer = putCreatureOnBattlefield(player1, "Prodigal Sorcerer")
        removeSummoningSickness(sorcerer)
        val goblin = putCreatureOnBattlefield(player1, "Goblin Guide")
        removeSummoningSickness(goblin)

        val inHand = putCardInHand(player1, "Mourner's Shield")
        giveColorlessMana(player1, 4)
        castSpell(player1, inHand).error shouldBe null
        settle(accept = accept, graveyardCard = counterspell)

        return Triple(findPermanent(player1, "Mourner's Shield")!!, sorcerer, goblin)
    }

    /** The Sorcerer pings player 2 for 1. */
    fun GameTestDriver.ping(sorcerer: EntityId) {
        val pingAbility = cardRegistry.requireCard("Prodigal Sorcerer").activatedAbilities.single()
        submit(
            ActivateAbility(
                playerId = player1,
                sourceId = sorcerer,
                abilityId = pingAbility.id,
                targets = listOf(ChosenTarget.Player(player2))
            )
        ).error shouldBe null
        settle(accept = true, graveyardCard = null)
    }

    test("only sources sharing a color with the exiled card are offered") {
        val d = driver()
        val (shield, sorcerer, goblin) = d.setUpBoard(accept = true)
        d.getExileCardNames(d.player1) shouldBe listOf("Counterspell")

        d.giveColorlessMana(d.player1, 2)
        d.submit(ActivateAbility(d.player1, shield, shieldAbility)).error shouldBe null
        d.bothPass()

        val decision = d.state.pendingDecision
        withClue("the ability pauses to choose a source: $decision") {
            (decision is SelectCardsDecision) shouldBe true
        }
        val options = (decision as SelectCardsDecision).options
        withClue("the blue Sorcerer shares a colour with the blue Counterspell") {
            (sorcerer in options) shouldBe true
        }
        withClue("the red Goblin Guide does not, so it is never offered") {
            (goblin in options) shouldBe false
        }
        withClue("nor is the colourless Shield itself") {
            (shield in options) shouldBe false
        }
    }

    test("the chosen source's damage to the opponent is prevented, not just damage to you") {
        val d = driver()
        val (shield, sorcerer, _) = d.setUpBoard(accept = true)

        d.giveColorlessMana(d.player1, 2)
        d.submit(ActivateAbility(d.player1, shield, shieldAbility)).error shouldBe null
        d.bothPass()
        d.submitCardSelection(d.player1, listOf(sorcerer)).error shouldBe null

        d.ping(sorcerer)

        withClue("\"prevent all damage that would be dealt by\" has no recipient clause") {
            d.getLifeTotal(d.player2) shouldBe 20
        }
    }

    test("without the Shield's ability the same ping lands") {
        val d = driver()
        val (_, sorcerer, _) = d.setUpBoard(accept = true)

        d.ping(sorcerer)

        withClue("the control case — nothing is preventing anything yet") {
            d.getLifeTotal(d.player2) shouldBe 19
        }
    }

    test("declining the imprint leaves no eligible source, and nothing is prevented") {
        val d = driver()
        val (shield, sorcerer, _) = d.setUpBoard(accept = false)
        d.getExileCardNames(d.player1) shouldBe emptyList()

        d.giveColorlessMana(d.player1, 2)
        d.submit(ActivateAbility(d.player1, shield, shieldAbility)).error shouldBe null
        d.bothPass()

        withClue("no exiled card means no colour to share, so no source qualifies and no choice is offered") {
            d.state.pendingDecision shouldBe null
        }

        d.ping(sorcerer)
        d.getLifeTotal(d.player2) shouldBe 19
    }
})
