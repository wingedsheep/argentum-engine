package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for The Raven's Warning (KHM #227).
 *
 *   I — Create a 1/1 blue Bird creature token with flying. You gain 2 life.
 *   II — Whenever one or more creatures you control with flying deal combat damage to a player
 *        this turn, look at that player's hand and draw a card.
 *   III — You may put a card you own from outside the game on top of your library.
 *
 * Chapter II is the interesting one: a floating "this turn" ability on the batched combat-damage
 * trigger. Two fliers connecting draw exactly one card (one batch), a non-flier connecting draws
 * nothing, and the damaged player's hand is looked at.
 */
class TheRavensWarningScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.setLore(sagaName: String, lore: Int) {
            val saga = findPermanent(sagaName)!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, lore))
                    .with(SagaComponent(triggeredChapters = (1..lore).toSet()))
            }
        }

        fun chapterTwoBoard() = scenario()
            .withPlayers("Raven", "Opponent")
            .withCardOnBattlefield(1, "The Raven's Warning")
            .withCardOnBattlefield(1, "Augury Raven")
            .withCardOnBattlefield(1, "Wind Drake")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(2, "Shock")
            .withCardInHand(2, "Lightning Bolt")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Mountain") } }
            .build()
            .apply {
                setLore("The Raven's Warning", 1)
                passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                resolveStack() // chapter II creates the floating trigger
            }

        test("chapter I creates a 1/1 blue flying Bird and gains 2 life") {
            val game = scenario()
                .withPlayers("Raven", "Opponent")
                .withCardInHand(1, "The Raven's Warning")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Mountain") } }
                .build()

            game.castSpell(1, "The Raven's Warning").error shouldBe null
            game.resolveStack()

            val bird = game.findPermanent("Bird Token").shouldNotBeNull()
            val projected = game.state.projectedState
            projected.getPower(bird) shouldBe 1
            projected.getToughness(bird) shouldBe 1
            projected.hasKeyword(bird, Keyword.FLYING) shouldBe true
            game.getLifeTotal(1) shouldBe 22
        }

        test("chapter II: two fliers connecting look at the hand and draw exactly one card") {
            val game = chapterTwoBoard()
            val handBefore = game.handSize(1)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Augury Raven" to 2, "Wind Drake" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            withClue("all three connected") { game.getLifeTotal(2) shouldBe 20 - 3 - 2 - 2 }
            withClue("one batch of fliers hitting one player is one trigger: one card drawn") {
                game.handSize(1) shouldBe handBefore + 1
            }
            withClue("the damaged player's hand was looked at") {
                game.findCardsInHand(2, "Shock").single().let { shock ->
                    game.state.getEntity(shock)?.get<RevealedToComponent>()?.isRevealedTo(game.player1Id) shouldBe true
                }
            }
        }

        test("chapter II: only a non-flier connecting does nothing") {
            val game = chapterTwoBoard()
            val handBefore = game.handSize(1)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 18
            withClue("Grizzly Bears has no flying, so nothing triggers") { game.handSize(1) shouldBe handBefore }
        }

        test("chapter III puts a chosen sideboard card on top of your library") {
            val game = scenario()
                .withPlayers("Raven", "Opponent")
                .withCardOnBattlefield(1, "The Raven's Warning")
                .withCardInSideboard(1, "Serra Angel")
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Mountain") } }
                .build()
            game.setLore("The Raven's Warning", 2)

            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()

            val decision = game.getPendingDecision() as? SelectCardsDecision
            decision.shouldNotBeNull()
            val angel = decision.cardInfo!!.entries.first { it.value.name == "Serra Angel" }.key
            game.selectCards(listOf(angel)).error shouldBe null

            withClue("Serra Angel is now the top card of the library") {
                game.state.getLibrary(game.player1Id).first() shouldBe angel
            }
            game.isInSideboard(1, "Serra Angel") shouldBe false
            withClue("the Saga is sacrificed after chapter III") {
                game.isInGraveyard(1, "The Raven's Warning") shouldBe true
            }
        }
    }
}
