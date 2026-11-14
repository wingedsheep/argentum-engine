package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.RenownedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Enshrouding Mist (ORI #13) — {W} Instant.
 *
 *   Target creature gets +1/+1 until end of turn. Prevent all damage that would be dealt to it
 *   this turn. If it's renowned, untap it.
 *
 * The first card to test a spell *target's* renowned designation at resolution
 * (`Conditions.TargetMatchesFilter` over `StatePredicate.IsRenowned`).
 */
class EnshroudingMistScenarioTest : ScenarioTestBase() {

    private fun castOnTappedBears(renowned: Boolean): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Enshrouding Mist")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val bears = game.findPermanent("Grizzly Bears")!!
        if (renowned) game.state = game.state.updateEntity(bears) { it.with(RenownedComponent) }

        game.castSpell(1, "Enshrouding Mist", bears).error shouldBe null
        game.resolveStack()
        return game to bears
    }

    init {
        test("renowned target: pumped and untapped") {
            val (game, bears) = castOnTappedBears(renowned = true)

            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
            withClue("a renowned target is untapped") {
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
            }
        }

        test("non-renowned target: pumped but stays tapped") {
            val (game, bears) = castOnTappedBears(renowned = false)

            game.state.projectedState.getPower(bears) shouldBe 3
            withClue("the untap clause only applies to a renowned creature") {
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            }
        }
    }
}
