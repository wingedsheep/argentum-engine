package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `RepeatWhileEffect.collectCollections` (the `repeatCollecting { }` pipeline verb): each pass of
 * the loop runs from the pristine pre-loop context, and the named body collections are unioned
 * across every pass and published to the effects after the loop once it stops.
 *
 * Covers both ways a loop can run — a body that resolves synchronously with a `WhileCondition`,
 * and a body that pauses for a card choice with a `PlayerChooses` condition — because the two
 * paths hand the aggregates on through different seams (the result's `updatedCollections` vs.
 * the resumers' `exposeCollectionsToNextFrame`).
 */
class RepeatWhileCollectCollectionsTest : FunSpec({

    // Synchronous body: mill one card per pass while your graveyard holds fewer than three cards,
    // then return every card milled this way to your hand.
    val millUntilThree = card("Mill Until Three") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Mill a card. Repeat this process until three cards are in your graveyard. " +
            "Return the cards milled this way to your hand."
        spell {
            effect = Effects.Pipeline {
                val (milled) = repeatCollecting(
                    RepeatCondition.WhileCondition(
                        Compare(DynamicAmount.Count(Player.You, Zone.GRAVEYARD), ComparisonOperator.LT, DynamicAmount.Fixed(3))
                    )
                ) {
                    listOf(mill(1))
                }
                toHand(milled)
            }
        }
    }

    // Pausing body: exile up to one card from your hand; you may repeat. Then put every card
    // exiled this way into your graveyard.
    val exileAndBury = card("Exile and Bury") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Exile up to one card from your hand. You may repeat this process any number of " +
            "times. Put the cards exiled this way into your graveyard."
        spell {
            effect = Effects.Pipeline {
                val (exiled) = repeatCollecting(
                    RepeatCondition.PlayerChooses(EffectTarget.Controller, "Exile another card?")
                ) {
                    val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                    val pick = chooseUpTo(1, from = hand, alwaysPrompt = true)
                    exile(pick)
                    listOf(pick)
                }
                toGraveyard(exiled)
            }
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(millUntilThree, exileAndBury))
        initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("a synchronous loop publishes every pass's collection, not just the last") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Mill Until Three")
        d.giveMana(me, Color.GREEN, 1)
        val handBefore = d.getHandSize(me)

        d.castSpell(me, spell)
        d.bothPass()

        withClue("three passes milled three cards, and all three came back") {
            d.getGraveyardCardNames(me) shouldBe listOf("Mill Until Three")
            // -1 for the spell cast, +3 for the milled cards returned.
            d.getHandSize(me) shouldBe handBefore - 1 + 3
        }
    }

    test("a pausing loop stopped by the player publishes every pass's picks") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Exile and Bury")
        val bears = d.putCardInHand(me, "Grizzly Bears")
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.GREEN, 1)

        d.castSpell(me, spell)
        d.bothPass()

        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(me, listOf(bears))
        d.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(me, true)
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(me, listOf(bolt))
        d.submitYesNo(me, false)

        withClue("both passes' picks moved from exile to the graveyard") {
            d.getExileCardNames(me).shouldBeEmpty()
            d.getGraveyardCardNames(me) shouldContainExactlyInAnyOrder
                listOf("Exile and Bury", "Grizzly Bears", "Lightning Bolt")
        }
    }

    test("a loop whose passes collected nothing publishes an empty aggregate") {
        val d = driver()
        val me = d.activePlayer!!
        val spell = d.putCardInHand(me, "Exile and Bury")
        d.putCardInHand(me, "Grizzly Bears")
        d.giveMana(me, Color.GREEN, 1)

        d.castSpell(me, spell)
        d.bothPass()

        d.submitCardSelection(me, emptyList())
        d.submitYesNo(me, false)

        withClue("nothing exiled, nothing buried, and the spell finished resolving") {
            d.state.pendingDecision shouldBe null
            d.getExileCardNames(me).shouldBeEmpty()
            d.getGraveyardCardNames(me) shouldBe listOf("Exile and Bury")
            d.findCardInHand(me, "Grizzly Bears") shouldNotBe null
        }
    }
})
