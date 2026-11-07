package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dark Intimations (AER #128) — {2}{U}{B}{R} Sorcery.
 *
 * "Each opponent sacrifices a creature or planeswalker of their choice, then discards a card. You
 *  return a creature or planeswalker card from your graveyard to your hand, then draw a card.
 *  When you cast a Bolas planeswalker spell, exile this card from your graveyard. That planeswalker
 *  enters with an additional loyalty counter on it."
 *
 * Pins the spell's creature-or-planeswalker edict, the untargeted resolution-time return (lands
 * aren't offered, the draw still happens with nothing to return), and the graveyard cast trigger:
 * only Bolas planeswalker spells fire it, the card is exiled, and the extra loyalty counter put on
 * the spell adds to its printed starting loyalty — once per Dark Intimations in the graveyard.
 */
class DarkIntimationsScenarioTest : ScenarioTestBase() {

    private val bolasWalker = card("Dark Intimations Test Bolas") {
        manaCost = "{B}"
        typeLine = "Legendary Planeswalker — Bolas"
        startingLoyalty = 3
        oracleText = ""
    }
    private val otherWalker = card("Dark Intimations Test Liliana") {
        manaCost = "{B}"
        typeLine = "Legendary Planeswalker — Liliana"
        startingLoyalty = 3
        oracleText = ""
    }

    init {
        cardRegistry.register(bolasWalker)
        cardRegistry.register(otherWalker)

        fun spellBuilder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Dark Intimations")
            .withLandsOnBattlefield(1, "Island", 1)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        /** Casts Dark Intimations and answers every card selection with [choose]. */
        fun TestGame.castAndResolve(
            choose: (SelectCardsDecision) -> List<EntityId> = { it.options.take(it.minSelections.coerceAtLeast(1)) },
        ) {
            castSpell(1, "Dark Intimations").error shouldBe null
            resolveStack()
            var guard = 0
            while (state.pendingDecision != null && guard++ < 10) {
                val decision = state.pendingDecision
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                selectCards(choose(decision)).error shouldBe null
                resolveStack()
            }
            state.pendingDecision shouldBe null
        }

        fun TestGame.loyalty(name: String): Int =
            state.getEntity(findPermanent(name)!!)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)

        test("opponent sacrifices a planeswalker and discards; you return the chosen card and draw") {
            val game = spellBuilder()
                .withCardOnBattlefield(2, "Dark Intimations Test Liliana")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Dark Intimations Test Bolas")
                .withCardInGraveyard(1, "Plains")
                .build()
            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
            val bolas = game.findCardsInGraveyard(1, "Dark Intimations Test Bolas").single()

            game.castAndResolve { decision ->
                if (decision.playerId == game.player1Id) {
                    withClue("only creature and planeswalker cards in your graveyard are offered") {
                        decision.options shouldContainExactlyInAnyOrder listOf(giant, bolas)
                    }
                    listOf(bolas)
                } else {
                    decision.options.take(decision.minSelections.coerceAtLeast(1))
                }
            }

            game.isInGraveyard(2, "Dark Intimations Test Liliana") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.handSize(2) shouldBe 0
            game.isInHand(1, "Dark Intimations Test Bolas") shouldBe true
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.isInGraveyard(1, "Plains") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
            game.isInGraveyard(1, "Dark Intimations") shouldBe true
        }

        test("with nothing to sacrifice or return, the opponent still discards and you still draw") {
            val game = spellBuilder()
                .withCardInHand(2, "Grizzly Bears")
                .withCardInGraveyard(1, "Plains")
                .build()

            game.castAndResolve()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Plains") shouldBe true
            game.handSize(1) shouldBe 1
            game.isInHand(1, "Forest") shouldBe true
        }

        fun walkerBuilder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInGraveyard(1, "Dark Intimations")
            .withCardInHand(1, "Dark Intimations Test Bolas")
            .withCardInHand(1, "Dark Intimations Test Liliana")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("casting a Bolas planeswalker exiles it from the graveyard and adds a loyalty counter") {
            val game = walkerBuilder().build()

            game.castSpell(1, "Dark Intimations Test Bolas").error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Dark Intimations") shouldBe true
            game.isInGraveyard(1, "Dark Intimations") shouldBe false
            withClue("printed loyalty 3 plus the additional counter") {
                game.loyalty("Dark Intimations Test Bolas") shouldBe 4
            }
        }

        test("a non-Bolas planeswalker spell does not trigger it") {
            val game = walkerBuilder().build()

            game.castSpell(1, "Dark Intimations Test Liliana").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Dark Intimations") shouldBe true
            game.loyalty("Dark Intimations Test Liliana") shouldBe 3
        }

        test("two copies in the graveyard each trigger, for two additional loyalty counters") {
            val game = walkerBuilder()
                .withCardInGraveyard(1, "Dark Intimations")
                .build()

            game.castSpell(1, "Dark Intimations Test Bolas").error shouldBe null
            game.resolveStack()

            game.graveyardSize(1) shouldBe 0
            game.loyalty("Dark Intimations Test Bolas") shouldBe 5
        }
    }
}
