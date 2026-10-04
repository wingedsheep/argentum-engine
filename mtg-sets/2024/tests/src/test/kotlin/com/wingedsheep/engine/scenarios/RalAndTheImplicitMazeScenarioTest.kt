package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Ral and the Implicit Maze (MH3 #132) — {3}{R}{R} Enchantment — Saga.
 *
 *  I — This Saga deals 2 damage to each creature and planeswalker your opponents control.
 *  II — You may discard a card. If you do, exile the top two cards of your library. You may play
 *       them until the end of your next turn.
 *  III — Create a Spellgorger Weird token ({2}{R} 2/2 Weird, "Whenever you cast a noncreature
 *        spell, put a +1/+1 counter on Spellgorger Weird.").
 */
class RalAndTheImplicitMazeScenarioTest : ScenarioTestBase() {

    private val saga = "Ral and the Implicit Maze"

    /** Saga with [loreCounters] lore counters at upkeep; reaching the main phase adds the next one. */
    private fun atNextChapter(loreCounters: Int, extra: ScenarioBuilder.() -> Unit): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, saga)
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .withTurnNumber(3)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply(extra)
            .build()
        val sagaId = game.findPermanent(saga)!!
        game.state = game.state.updateEntity(sagaId) { c ->
            c.with(CountersComponent().withAdded(CounterType.LORE, loreCounters))
                .with(SagaComponent(triggeredChapters = (1..loreCounters).toSet()))
        }
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }

    init {
        cardRegistry.register(
            CardDefinition.instant(name = "Test Bolt", manaCost = ManaCost.parse("{R}"), oracleText = "")
        )

        test("chapter I deals 2 damage to each creature and planeswalker opponents control, not yours") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, saga)
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, saga).error shouldBe null
            game.resolveStack()
            game.resolveStack()

            withClue("Bob's 2/2 dies, Bob's 3/3 survives with damage, Alice's 2/2 is untouched") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.findPermanent("Hill Giant") shouldNotBe null
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }
        }

        test("chapter II: discarding exiles the top two cards, playable until the end of your next turn") {
            val game = atNextChapter(1) {
                withCardInHand(1, "Shock")
                withCardInLibrary(1, "Grizzly Bears")
                withCardInLibrary(1, "Hill Giant")
            }
            game.resolveStack()

            val may = game.getPendingDecision()
            withClue("chapter II asks whether to discard, got $may") { (may is YesNoDecision) shouldBe true }
            game.answerYesNo(true).error shouldBe null
            (game.getPendingDecision() as? SelectCardsDecision)?.let { game.selectCards(listOf(it.options.first())) }
            game.resolveStack()

            game.isInGraveyard(1, "Shock") shouldBe true
            val exiled = game.state.getExile(game.player1Id)
            exiled.size shouldBe 2
            exiled.forEach { id ->
                withClue("each exiled card carries a may-play permission") {
                    game.state.mayPlayPermissions.any { id in it.cardIds } shouldBe true
                }
            }
        }

        test("chapter II: declining to discard exiles nothing") {
            val game = atNextChapter(1) {
                withCardInHand(1, "Shock")
            }
            game.resolveStack()

            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.state.getExile(game.player1Id).size shouldBe 0
            game.isInGraveyard(1, "Shock") shouldBe false
        }

        test("chapter III creates a {2}{R} Spellgorger Weird token that grows on noncreature spells") {
            val game = atNextChapter(2) {
                withCardInHand(1, "Test Bolt")
                withLandsOnBattlefield(1, "Mountain", 1)
            }
            game.resolveStack()

            val weirdId = game.findPermanent("Spellgorger Weird")!!
            val entity = game.state.getEntity(weirdId)!!
            entity.has<TokenComponent>() shouldBe true
            val card = entity.get<CardComponent>()!!
            withClue("the token is a copy of the Oracle card: mana cost {2}{R}, mana value 3, red") {
                card.manaCost.cmc shouldBe 3
                game.state.projectedState.getColors(weirdId) shouldBe setOf(Color.RED.name)
            }
            game.state.projectedState.getPower(weirdId) shouldBe 2
            game.state.projectedState.getToughness(weirdId) shouldBe 2
            withClue("MH3's own Spellgorger Weird token art") {
                card.imageUri!! shouldContain "33b63bd0"
            }
            withClue("the saga is sacrificed after its final chapter") {
                game.findPermanent(saga) shouldBe null
            }

            game.castSpell(1, "Test Bolt").error shouldBe null
            game.resolveStack()
            game.state.getEntity(weirdId)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }
    }
}
