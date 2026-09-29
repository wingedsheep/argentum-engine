package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Baral and Kari Zev (MOM #218) — whenever you cast your first instant or sorcery spell each turn,
 * you may cast a spell with lesser mana value that shares a card type with it from your hand for
 * free. If you don't, create First Mate Ragavan (legendary 2/1 red Monkey Pirate), which gains
 * haste until end of turn.
 *
 * Think Twice ({1}{U} instant, MV 2) is the trigger; Dark Ritual ({B} instant, MV 1) is the
 * eligible free cast; Divination ({2}{U} sorcery, MV 3) is never eligible off Think Twice.
 */
class BaralAndKariZevScenarioTest : ScenarioTestBase() {

    private fun board(vararg hand: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Baral and Kari Zev")
        .withCardsInHand(1, "Think Twice", 2)
        .apply { hand.forEach { withCardInHand(1, it) } }
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.ragavans() = findPermanents("First Mate Ragavan")

    private fun TestGame.castThinkTwice() {
        castSpell(1, "Think Twice").error shouldBe null
    }

    init {
        context("Baral and Kari Zev") {
            test("first instant: may cast a lesser-MV instant from hand for free, and no Ragavan") {
                val game = board("Dark Ritual")
                game.castThinkTwice()
                game.resolveStack()

                val pick = game.state.pendingDecision as? SelectCardsDecision
                    ?: error("expected the free-cast pick, got ${game.state.pendingDecision}")
                val ritual = game.findCardsInHand(1, "Dark Ritual").single()
                pick.options shouldBe listOf(ritual)
                game.selectCards(listOf(ritual)).error shouldBe null
                game.resolveStack()

                withClue("Dark Ritual was cast for free and resolved") {
                    game.isInGraveyard(1, "Dark Ritual") shouldBe true
                }
                game.isInGraveyard(1, "Think Twice") shouldBe true
                withClue("casting the spell means the 'if you don't' token is not created") {
                    game.ragavans().size shouldBe 0
                }
            }

            test("declining the free cast creates First Mate Ragavan with haste") {
                val game = board("Dark Ritual")
                game.castThinkTwice()
                game.resolveStack()

                game.state.pendingDecision as? SelectCardsDecision
                    ?: error("expected the free-cast pick, got ${game.state.pendingDecision}")
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Dark Ritual") shouldBe true
                val ragavan = game.ragavans().single()
                val card = game.state.getEntity(ragavan)?.get<CardComponent>().shouldNotBeNull()
                game.state.getEntity(ragavan)?.has<TokenComponent>() shouldBe true
                card.typeLine.isLegendary shouldBe true
                card.typeLine.hasSubtype(Subtype("Monkey")) shouldBe true
                card.typeLine.hasSubtype(Subtype("Pirate")) shouldBe true
                val projected = game.state.projectedState
                projected.getPower(ragavan) shouldBe 2
                projected.getToughness(ragavan) shouldBe 1
                projected.hasKeyword(ragavan, Keyword.HASTE) shouldBe true
            }

            test("no eligible card in hand (higher MV, other type) creates Ragavan") {
                val game = board("Divination")
                game.castThinkTwice()
                game.resolveStack()
                (game.state.pendingDecision as? SelectCardsDecision)?.let {
                    it.options shouldBe emptyList()
                    game.skipSelection().error shouldBe null
                    game.resolveStack()
                }

                game.isInHand(1, "Divination") shouldBe true
                game.ragavans().size shouldBe 1
            }

            test("the second instant in the same turn does not trigger") {
                val game = board()
                game.castThinkTwice()
                game.resolveStack()
                (game.state.pendingDecision as? SelectCardsDecision)?.let {
                    game.skipSelection().error shouldBe null
                    game.resolveStack()
                }
                game.ragavans().size shouldBe 1

                game.castThinkTwice()
                withClue("only the first instant or sorcery each turn triggers") {
                    game.state.stack.size shouldBe 1
                }
                game.resolveStack()
                game.state.pendingDecision shouldBe null
                game.ragavans().size shouldBe 1
            }
        }
    }
}
