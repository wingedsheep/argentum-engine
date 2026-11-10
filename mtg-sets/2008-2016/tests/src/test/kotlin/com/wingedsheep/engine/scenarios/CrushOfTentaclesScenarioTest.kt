package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Crush of Tentacles (OGW #53, {4}{U}{U}, Sorcery).
 *
 *   Surge {3}{U}{U}
 *   Return all nonland permanents to their owners' hands. If this spell's surge cost was paid,
 *   create an 8/8 blue Octopus creature token.
 */
class CrushOfTentaclesScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Crush of Tentacles")
        .withCardInHand(1, "Lightning Bolt")
        .withCardOnBattlefield(1, "Glory Seeker")
        .withCardOnBattlefield(2, "Glory Seeker")
        .withLandsOnBattlefield(1, "Island", 6)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("surged: every nonland permanent bounces, then an 8/8 Octopus is created and stays") {
            val game = board()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.execute(
                CastSpell(
                    game.player1Id, game.findCardsInHand(1, "Crush of Tentacles").first(),
                    useAlternativeCost = true, alternativeCostType = AlternativeCostType.SURGE,
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("both Glory Seekers returned to their owners' hands") {
                game.findPermanents("Glory Seeker").size shouldBe 0
                game.isInHand(1, "Glory Seeker") shouldBe true
                game.isInHand(2, "Glory Seeker") shouldBe true
            }
            withClue("lands stay") { game.findPermanents("Island").size shouldBe 6 }
            val octopus = game.findPermanent("Octopus Token")
            octopus shouldNotBe null
            game.state.projectedState.getPower(octopus!!) shouldBe 8
            game.state.projectedState.getToughness(octopus) shouldBe 8
        }

        test("cast for its mana cost: the bounce happens but no Octopus") {
            val game = board()
            game.castSpell(1, "Crush of Tentacles").error shouldBe null
            game.resolveStack()

            game.findPermanents("Glory Seeker").size shouldBe 0
            game.findPermanent("Octopus Token") shouldBe null
        }
    }
}
