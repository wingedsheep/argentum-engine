package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rally the Ancestors (FRF #22) — {X}{W}{W} Instant. "Return each creature card with mana value X
 * or less from your graveyard to the battlefield. Exile those creatures at the beginning of your
 * next upkeep. Exile Rally the Ancestors."
 *
 * Covers what the script alone can't show: the mana-value bound reads the X paid, noncreature cards
 * stay behind, the spell exiles itself, the delayed trigger waits through the opponent's upkeep and
 * fires on the caster's own next upkeep, and cast on the opponent's turn it fires on the very next
 * (the caster's) upkeep.
 */
class RallyTheAncestorsScenarioTest : ScenarioTestBase() {

    init {
        context("Rally the Ancestors") {

            test("returns creature cards with mana value X or less, then exiles them on your next upkeep") {
                val builder = scenario()
                    .withPlayers("Rally Player", "Opponent")
                    .withCardInHand(1, "Rally the Ancestors")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) {
                    builder.withCardInLibrary(1, "Plains")
                    builder.withCardInLibrary(2, "Forest")
                }
                val game = builder.build()

                val cast = game.castXSpell(1, "Rally the Ancestors", xValue = 2)
                withClue("Rally should be castable: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("creature cards with mana value 2 or less return") {
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("a mana value 4 creature and a noncreature card stay in the graveyard") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                }
                withClue("Rally the Ancestors exiles itself") {
                    game.isInExile(1, "Rally the Ancestors") shouldBe true
                    game.isInGraveyard(1, "Rally the Ancestors") shouldBe false
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.resolveStack()
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("the opponent's upkeep is not your upkeep") {
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()
                withClue("both returned creatures are exiled at the beginning of your next upkeep") {
                    game.isInExile(1, "Savannah Lions") shouldBe true
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("the card left in the graveyard is untouched") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
            }

            test("cast on the opponent's turn, the creatures are exiled on your very next upkeep") {
                val builder = scenario()
                    .withPlayers("Rally Player", "Opponent")
                    .withCardInHand(1, "Rally the Ancestors")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .withPriorityPlayer(1)
                repeat(10) {
                    builder.withCardInLibrary(1, "Plains")
                    builder.withCardInLibrary(2, "Forest")
                }
                val game = builder.build()

                val cast = game.castXSpell(1, "Rally the Ancestors", xValue = 1)
                withClue("Rally should be castable: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("only the mana value 1 creature returns for X = 1") {
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()
                withClue("exiled at the beginning of your upkeep, the next turn") {
                    game.isInExile(1, "Savannah Lions") shouldBe true
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                }
            }
        }
    }
}
