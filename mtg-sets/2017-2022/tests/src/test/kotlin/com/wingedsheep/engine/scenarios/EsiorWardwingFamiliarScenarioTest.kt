package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Esior, Wardwing Familiar (CMR #67).
 *
 *   Spells your opponents cast that target one or more commanders you control cost {3} more to cast.
 *
 * Esior sits on the non-active player's side (player 2) next to a Grizzly Bears that is tagged as
 * player 2's commander in the first test and left untagged in the second, so the active player's
 * Lightning Bolt shows the tax lands on commanders alone: one land short of {3}{R} is refused,
 * {3}{R} is accepted, and the plain Bears costs only {R}.
 */
class EsiorWardwingFamiliarScenarioTest : ScenarioTestBase() {

    private fun board(mountains: Int): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(2, "Esior, Wardwing Familiar")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", mountains)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        return game
    }

    private fun tagCommander(game: TestGame) {
        val bears = game.findPermanent("Grizzly Bears")!!
        game.state = game.state.updateEntity(bears) { it.with(CommanderComponent(ownerId = game.player2Id)) }
    }

    init {
        test("an opponent's spell targeting a commander you control costs {3} more") {
            val short = board(mountains = 3)
            tagCommander(short)
            withClue("Lightning Bolt at Esior's controller's commander can't be cast for {2}{R}") {
                short.castSpell(1, "Lightning Bolt", short.findPermanent("Grizzly Bears")!!).error shouldNotBe null
            }

            val paid = board(mountains = 4)
            tagCommander(paid)
            withClue("{3}{R} pays for it") {
                paid.castSpell(1, "Lightning Bolt", paid.findPermanent("Grizzly Bears")!!).error shouldBe null
            }
            paid.resolveStack()
            paid.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("a creature you control that isn't a commander is not protected") {
            val game = board(mountains = 1)
            withClue("an untagged Grizzly Bears costs only {R} to Bolt") {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            }
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
    }
}
