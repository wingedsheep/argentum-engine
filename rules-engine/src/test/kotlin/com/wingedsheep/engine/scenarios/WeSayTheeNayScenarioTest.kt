package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * We Say Thee Nay! (MSH #82) — {1}{U} Instant — Arcane.
 *
 *   Teamwork 2
 *   Counter target spell unless its controller pays {2}. Counter that spell unless its controller
 *   pays {4} instead if this spell was cast using teamwork.
 *
 * The board is the same shape throughout — Player 2 casts Grizzly Bears, which eats two of their
 * Forests — with only the Forest count varying, so the tax is what the tests are reading:
 *
 *  - four Forests (two left untapped): the plain cast offers a {2} the controller can pay, and the
 *    teamwork cast is out of reach, so no offer is made at all and the spell is countered;
 *  - six Forests (four left untapped): the teamwork cast offers a {4} — named in the prompt, so the
 *    number itself is pinned, not just "more than 2" — and paying it saves Grizzly Bears.
 */
class WeSayTheeNayScenarioTest : ScenarioTestBase() {

    init {
        context("We Say Thee Nay!") {

            fun board(forests: Int = 4) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "We Say Thee Nay!")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", forests)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("cast without teamwork taxes the spell's controller {2}") {
                val game = board()
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(1, "We Say Thee Nay!", "Grizzly Bears")
                    .error shouldBe null

                game.resolveStack()

                val decision = game.getPendingDecision()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<YesNoDecision>()
                decision.playerId shouldBe game.player2Id
                withClue("no teamwork was declared, so the tax is the printed {2}") {
                    decision.prompt shouldContain "{2}"
                }
                withClue("nothing was tapped to pay teamwork") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
                }

                // Saying yes opens the mana-source picker; two of the four Forests are still
                // untapped, which is exactly {2}.
                game.answerYesNo(true).error shouldBe null
                game.submitManaSourcesAutoPay().error shouldBe null
                game.resolveStack()

                withClue("Player 2 paid the {2}, so Grizzly Bears is not countered") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("cast using teamwork raises the tax to {4}, which the controller cannot pay") {
                val game = board()
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                val bearsOnStack = game.state.stack.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                val cardId = game.findCardsInHand(1, "We Say Thee Nay!").first()

                // Teamwork 2 — the 3/3 Hill Giant clears the threshold on its own.
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Spell(bearsOnStack)),
                        declaredCostSlot = ChoiceSlot.TEAMWORK,
                        additionalCostPayment = AdditionalCostPayment(
                            variableCostPermanents = listOf(giant),
                        ),
                    ),
                ).error shouldBe null
                game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true

                game.resolveStack()

                withClue("two untapped Forests cannot pay {4}, so no offer is even made") {
                    game.getPendingDecision().shouldBeNull()
                }
                withClue("the spell is countered and goes to its owner's graveyard") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            // The test above only proves the teamwork tax is *more than* the two mana on the
            // table. This one names the number: with four Forests left untapped the offer is
            // actually made, and the prompt has to say {4} — not {3}, not {5}.
            test("the teamwork tax is exactly {4}, and paying it saves the spell") {
                val game = board(forests = 6)
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                val bearsOnStack = game.state.stack.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                val cardId = game.findCardsInHand(1, "We Say Thee Nay!").first()

                // Teamwork 2 — the 3/3 Hill Giant clears the threshold on its own.
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Spell(bearsOnStack)),
                        declaredCostSlot = ChoiceSlot.TEAMWORK,
                        additionalCostPayment = AdditionalCostPayment(
                            variableCostPermanents = listOf(giant),
                        ),
                    ),
                ).error shouldBe null

                game.resolveStack()

                val decision = game.getPendingDecision()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<YesNoDecision>()
                decision.playerId shouldBe game.player2Id
                withClue("teamwork was declared, so the tax is {4} rather than the printed {2}") {
                    decision.prompt shouldContain "{4}"
                    decision.prompt shouldNotContain "{2}"
                }

                // Four of the six Forests are still untapped, which is exactly {4}.
                game.answerYesNo(true).error shouldBe null
                game.submitManaSourcesAutoPay().error shouldBe null
                game.resolveStack()

                withClue("Player 2 paid the {4}, so Grizzly Bears is not countered") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
