package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Reviving Melody (JOU #138) — {2}{G} Sorcery.
 *
 *   Choose one or both —
 *   • Return target creature card from your graveyard to your hand.
 *   • Return target enchantment card from your graveyard to your hand.
 */
class RevivingMelodyScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Reviving Melody")
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInGraveyard(1, "Pacifism")
        .withCardInGraveyard(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.cast(modes: List<Int>, modeTargets: List<List<ChosenTarget>>) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Reviving Melody").single(),
            targets = modeTargets.flatten(),
            chosenModes = modes,
            modeTargetsOrdered = modeTargets,
        )
    )

    init {
        test("choosing both returns a creature card and an enchantment card") {
            val game = board()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val pacifism = game.findCardsInGraveyard(1, "Pacifism").single()

            game.cast(
                modes = listOf(0, 1),
                modeTargets = listOf(
                    listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)),
                    listOf(ChosenTarget.Card(pacifism, game.player1Id, Zone.GRAVEYARD)),
                ),
            ).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Pacifism") shouldBe true
        }

        test("one mode is enough") {
            val game = board()
            val pacifism = game.findCardsInGraveyard(1, "Pacifism").single()

            game.cast(
                modes = listOf(1),
                modeTargets = listOf(listOf(ChosenTarget.Card(pacifism, game.player1Id, Zone.GRAVEYARD))),
            ).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Pacifism") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }

        test("it can't reach into an opponent's graveyard") {
            val game = board()
            val giant = game.findCardsInGraveyard(2, "Hill Giant").single()

            withClue("\"your graveyard\" — the opponent's creature card is not a legal target") {
                game.cast(
                    modes = listOf(0),
                    modeTargets = listOf(listOf(ChosenTarget.Card(giant, game.player2Id, Zone.GRAVEYARD))),
                ).error shouldNotBe null
            }
        }
    }
}
