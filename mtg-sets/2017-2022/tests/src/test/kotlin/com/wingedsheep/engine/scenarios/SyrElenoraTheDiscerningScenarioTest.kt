package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Syr Elenora, the Discerning (ELD #67; {3}{U}{U}, * /4).
 *
 *   Syr Elenora's power is equal to the number of cards in your hand.
 *   When Syr Elenora enters, draw a card.
 *   Spells your opponents cast that target Syr Elenora cost {2} more to cast.
 */
class SyrElenoraTheDiscerningScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame): Int? =
        game.state.projectedState.getProjectedValues(game.findPermanent("Syr Elenora, the Discerning")!!)?.power

    private fun toughness(game: TestGame): Int? =
        game.state.projectedState.getProjectedValues(game.findPermanent("Syr Elenora, the Discerning")!!)?.toughness

    init {
        context("Syr Elenora, the Discerning") {

            test("casting her draws a card on entry, and her power tracks your hand size") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Syr Elenora, the Discerning")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Hill Giant")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Island")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Syr Elenora, the Discerning").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Syr Elenora, the Discerning") shouldBe true
                withClue("ETB drew a card: two left in hand plus the draw") {
                    game.handSize(1) shouldBe 3
                }
                power(game) shouldBe 3
                toughness(game) shouldBe 4
            }

            test("power is zero with an empty hand and updates as the hand changes") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Syr Elenora, the Discerning")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                power(game) shouldBe 1
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("hand is now empty") { power(game) shouldBe 0 }
                game.isOnBattlefield("Syr Elenora, the Discerning") shouldBe true
            }

            test("counts only her controller's hand, not the opponent's") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Syr Elenora, the Discerning")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                power(game) shouldBe 1
            }

            test("an opponent's spell targeting her costs {2} more") {
                val short = taxBoard(mountains = 2)
                val elenora = short.findPermanent("Syr Elenora, the Discerning")!!
                withClue("{R} + {2} tax can't be paid with two Mountains") {
                    short.castSpell(1, "Lightning Bolt", elenora).error shouldNotBe null
                }

                val paid = taxBoard(mountains = 3)
                val elenora2 = paid.findPermanent("Syr Elenora, the Discerning")!!
                withClue("{2}{R} pays for it") {
                    paid.castSpell(1, "Lightning Bolt", elenora2).error shouldBe null
                }
                paid.resolveStack()
                withClue("3 damage doesn't kill a 4-toughness creature") {
                    paid.isOnBattlefield("Syr Elenora, the Discerning") shouldBe true
                }
            }

            test("an opponent's spell targeting another creature you control is not taxed") {
                val game = taxBoard(mountains = 1)
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("her controller's own spells targeting her are not taxed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Syr Elenora, the Discerning")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elenora = game.findPermanent("Syr Elenora, the Discerning")!!
                game.castSpell(1, "Giant Growth", elenora).error shouldBe null
                game.resolveStack()
                withClue("hand empty after casting (0) + Giant Growth's +3") { power(game) shouldBe 3 }
            }
        }
    }

    private fun taxBoard(mountains: Int): TestGame =
        scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(2, "Syr Elenora, the Discerning")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", mountains)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
}
