package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mkm.cards.LoxodonEavesdropper
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Loxodon Eavesdropper (MKM) — {3}{G} 3/3 Creature — Elephant Detective.
 *
 * "When this creature enters, investigate.
 *  Whenever you draw your second card each turn, this creature gets +1/+1 and gains vigilance
 *  until end of turn."
 *
 * The enters half is a plain `Effects.Investigate()`; the interesting half is the
 * [com.wingedsheep.sdk.dsl.Triggers.NthCardDrawn]`(2)` payoff landing on the source itself, so
 * these cover: the Clue arriving on entry, the second draw (and only the second) pumping it, and
 * the bonus being end-of-turn rather than permanent.
 */
class LoxodonEavesdropperScenarioTest : ScenarioTestBase() {

    // Free instants so a cast costs no mana and the only thing under test is the draw count.
    private val drawOne = card("Draw One Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }

    private val drawThree = card("Draw Three Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw three cards."
        spell { effect = Effects.DrawCards(3) }
    }

    init {
        cardRegistry.register(LoxodonEavesdropper)
        cardRegistry.register(drawOne)
        cardRegistry.register(drawThree)

        context("Loxodon Eavesdropper") {

            test("entering the battlefield investigates — a Clue token arrives") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Loxodon Eavesdropper")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.isOnBattlefield("Clue") shouldBe false

                game.castSpell(1, "Loxodon Eavesdropper").error shouldBe null
                game.resolveStack()

                withClue("the enters trigger must create a Clue token") {
                    game.findAllPermanents("Clue").size shouldBe 1
                }
            }

            test("the second draw of the turn pumps it and grants vigilance; the first does not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Loxodon Eavesdropper", summoningSickness = false)
                    .withCardsInHand(1, "Draw One Test", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val loxodon = game.findPermanent("Loxodon Eavesdropper")!!

                game.castSpell(1, "Draw One Test").error shouldBe null
                game.resolveStack()

                withClue("the first draw of the turn advances no NthCardDrawn(2)") {
                    val projected = StateProjector().project(game.state)
                    projected.getPower(loxodon) shouldBe 3
                    projected.getToughness(loxodon) shouldBe 3
                    projected.hasKeyword(loxodon, Keyword.VIGILANCE) shouldBe false
                }

                game.castSpell(1, "Draw One Test").error shouldBe null
                game.resolveStack()

                withClue("the second draw pumps it and grants vigilance") {
                    val projected = StateProjector().project(game.state)
                    projected.getPower(loxodon) shouldBe 4
                    projected.getToughness(loxodon) shouldBe 4
                    projected.hasKeyword(loxodon, Keyword.VIGILANCE) shouldBe true
                }
            }

            test("a single multi-card draw crossing the second card fires the trigger exactly once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Loxodon Eavesdropper", summoningSickness = false)
                    .withCardInHand(1, "Draw Three Test")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val loxodon = game.findPermanent("Loxodon Eavesdropper")!!

                game.castSpell(1, "Draw Three Test").error shouldBe null
                game.resolveStack()

                withClue("draws #1 and #3 fire nothing; only #2 does, for a single +1/+1") {
                    val projected = StateProjector().project(game.state)
                    projected.getPower(loxodon) shouldBe 4
                    projected.getToughness(loxodon) shouldBe 4
                    projected.hasKeyword(loxodon, Keyword.VIGILANCE) shouldBe true
                }
            }

            test("the bonus is until end of turn, not permanent") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Loxodon Eavesdropper", summoningSickness = false)
                    .withCardInHand(1, "Draw Three Test")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val loxodon = game.findPermanent("Loxodon Eavesdropper")!!

                game.castSpell(1, "Draw Three Test").error shouldBe null
                game.resolveStack()
                StateProjector().project(game.state).getPower(loxodon) shouldBe 4

                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)

                withClue("cleanup wipes the end-of-turn buff and the granted vigilance") {
                    val projected = StateProjector().project(game.state)
                    projected.getPower(loxodon) shouldBe 3
                    projected.getToughness(loxodon) shouldBe 3
                    projected.hasKeyword(loxodon, Keyword.VIGILANCE) shouldBe false
                }
            }
        }
    }
}
