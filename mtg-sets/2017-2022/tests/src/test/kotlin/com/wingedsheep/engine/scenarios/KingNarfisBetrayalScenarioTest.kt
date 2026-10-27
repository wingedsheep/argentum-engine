package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for King Narfi's Betrayal (KHM #219).
 *
 *   I — Each player mills four cards. Then you may exile a creature or planeswalker card from each
 *       graveyard.
 *   II, III — Until end of turn, you may cast spells from among cards exiled with this Saga, and you
 *       may spend mana as though it were mana of any color to cast those spells.
 *
 * Pins: every library is milled; the Saga's controller makes each graveyard's (optional) pick, even
 * a card that was already there; the picks are linked to the Saga, castable only once chapter II
 * grants it, with blue/black mana paying a red cost, and the caster controls the result.
 */
class KingNarfisBetrayalScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Betrayer", "Opponent")
            .withCardInHand(1, "King Narfi's Betrayal")
            .withLandsOnBattlefield(1, "Island", 1)
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInGraveyard(2, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(10) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }

        /** Cast the Saga and answer chapter I: exile [pick] from whichever graveyard offers it, skip the rest. */
        fun TestGame.castAndExile(pick: EntityId) {
            castSpell(1, "King Narfi's Betrayal").error shouldBe null
            resolveStack()
            var prompts = 0
            while (true) {
                val decision = getPendingDecision() as? SelectCardsDecision ?: break
                prompts++
                withClue("the Saga's controller makes every graveyard's pick") {
                    decision.playerId shouldBe player1Id
                }
                withClue("only creature or planeswalker cards are offered") {
                    decision.options.none { it in findCardsInGraveyard(1, "Plains") } shouldBe true
                }
                if (pick in decision.options) selectCards(listOf(pick)) else skipSelection()
            }
            withClue("one optional pick per graveyard") { prompts shouldBe 2 }
            resolveStack()
        }

        test("chapter I mills every player and exiles the chosen pre-existing creature card") {
            val game = builder().build()
            val giant = game.findCardsInGraveyard(2, "Hill Giant").single()
            game.castAndExile(giant)

            withClue("each library lost four cards") {
                game.librarySize(1) shouldBe 6
                game.librarySize(2) shouldBe 6
            }
            game.isInExile(2, "Hill Giant") shouldBe true
            withClue("the declined pick stays in its graveyard") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
        }

        test("the exiled card can't be cast before chapter II") {
            val game = builder().build()
            val giant = game.findCardsInGraveyard(2, "Hill Giant").single()
            game.castAndExile(giant)

            game.execute(CastSpell(game.player1Id, giant, emptyList())).error shouldNotBe null
            game.isInExile(2, "Hill Giant") shouldBe true
        }

        test("chapter II lets you cast the opponent's exiled creature with mana of any color") {
            val game = builder().build()
            val giant = game.findCardsInGraveyard(2, "Hill Giant").single()
            game.castAndExile(giant)

            // Through the opponent's turn to our next precombat main, where chapter II triggers.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            withClue("{3}{R} paid with an Island and three Swamps") {
                game.execute(CastSpell(game.player1Id, giant, emptyList())).error shouldBe null
            }
            game.resolveStack()

            val onBattlefield = game.findPermanent("Hill Giant").shouldNotBeNull()
            game.state.projectedState.getController(onBattlefield) shouldBe game.player1Id
        }
    }
}
