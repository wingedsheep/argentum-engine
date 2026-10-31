package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Feather, the Redeemed (WAR #197).
 *
 *   Flying
 *   Whenever you cast an instant or sorcery spell that targets a creature you control, exile that
 *   card instead of putting it into your graveyard as it resolves. If you do, return it to your
 *   hand at the beginning of the next end step.
 */
class FeatherTheRedeemedScenarioTest : ScenarioTestBase() {

    init {
        test("a spell targeting your creature is exiled as it resolves and returns to hand at the end step") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Feather, the Redeemed")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInHand(1, "Giant Growth")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()

            withClue("Giant Growth resolved") { game.state.projectedState.getPower(bears) shouldBe 5 }
            withClue("exiled instead of going to the graveyard") {
                game.isInExile(1, "Giant Growth") shouldBe true
                game.isInGraveyard(1, "Giant Growth") shouldBe false
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            withClue("back in hand at the beginning of the end step") {
                game.isInHand(1, "Giant Growth") shouldBe true
                game.isInExile(1, "Giant Growth") shouldBe false
            }
        }

        test("a spell that fizzles goes to the graveyard; the spell that killed its target comes back") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Feather, the Redeemed")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Giant Growth")
                .withCardInHand(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            // In response, Shock our own Bears: it resolves first and Giant Growth loses its target.
            game.castSpell(1, "Shock", bears).error shouldBe null
            game.resolveStack()

            withClue("the Bears died to Shock") { game.findPermanent("Grizzly Bears") shouldBe null }
            withClue("Giant Growth fizzled into the graveyard — not exiled") {
                game.isInGraveyard(1, "Giant Growth") shouldBe true
                game.isInExile(1, "Giant Growth") shouldBe false
            }
            withClue("Shock resolved, so it was exiled") { game.isInExile(1, "Shock") shouldBe true }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.isInHand(1, "Shock") shouldBe true
            game.isInHand(1, "Giant Growth") shouldBe false
        }

        test("a spell that targets only an opponent's creature is not exiled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Feather, the Redeemed")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Shock", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Shock") shouldBe true
            game.isInExile(1, "Shock") shouldBe false
        }
    }
}
