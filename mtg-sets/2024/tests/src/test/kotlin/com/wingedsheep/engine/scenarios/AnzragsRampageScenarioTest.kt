package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Anzrag's Rampage (MKM) — "Destroy all artifacts you don't control, then exile the top X cards of
 * your library, where X is the number of artifacts that were put into graveyards from the
 * battlefield this turn. You may put a creature card exiled this way onto the battlefield. It gains
 * haste. Return it to your hand at the beginning of the next end step."
 *
 * The thing that can silently break here is the **word "then"**: the artifacts this spell destroys
 * count toward its own X. `TurnTracker.ARTIFACTS_DIED` is incremented inline by
 * `ZoneTransitionService`, and the pipeline's gather step evaluates X when that step runs, so the
 * wrath's own casualties are already tallied. A version that read X up front would exile 0.
 *
 * The tracker is per-player, credited to each artifact's last-known controller, and read here with
 * `Player.Each` — so it is the game-wide count the card asks for, including artifacts that died
 * earlier in the turn under *your* control.
 */
class AnzragsRampageScenarioTest : ScenarioTestBase() {

    /** The exiled card with [name] in player 1's exile zone, if any. */
    private fun exiled(game: TestGame, name: String) =
        game.state.getExile(game.player1Id).firstOrNull { entityId ->
            game.state.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.name == name
        }

    init {
        test("X counts the artifacts the spell itself just destroyed") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Anzrag's Rampage")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Bottle Gnomes")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Anzrag's Rampage").error shouldBe null
            game.resolveStack()

            withClue("both opposing artifacts are gone") {
                game.isInGraveyard(2, "Ornithopter") shouldBe true
                game.isInGraveyard(2, "Bottle Gnomes") shouldBe true
            }

            // Two artifacts died, so X = 2 and the top two cards are exiled. The "you may put a
            // creature card onto the battlefield" prompt follows; decline it here.
            if (game.state.pendingDecision != null) {
                game.selectCards(emptyList())
                game.resolveStack()
            }

            withClue("X = 2 — the two artifacts this very spell destroyed") {
                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInExile(1, "Lightning Bolt") shouldBe true
            }
        }

        test("you may put a creature card exiled this way onto the battlefield, and it has haste") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Anzrag's Rampage")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Bottle Gnomes")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Anzrag's Rampage").error shouldBe null
            game.resolveStack()

            val exiledBears = exiled(game, "Grizzly Bears")
            withClue("the creature is among the exiled cards, so it can be offered") {
                (exiledBears != null) shouldBe true
            }
            game.selectCards(listOfNotNull(exiledBears))
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")
            withClue("it entered the battlefield") {
                (bears != null) shouldBe true
            }
            withClue("and it gains haste, so it can attack the turn it arrives") {
                game.state.projectedState.hasKeyword(bears!!, Keyword.HASTE) shouldBe true
            }
        }

        test("with no artifacts dying this turn X is zero and nothing is exiled") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Anzrag's Rampage")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Anzrag's Rampage").error shouldBe null
            game.resolveStack()

            withClue("no artifacts on the battlefield to destroy, so X = 0") {
                game.isInExile(1, "Grizzly Bears") shouldBe false
            }
        }
    }
}
