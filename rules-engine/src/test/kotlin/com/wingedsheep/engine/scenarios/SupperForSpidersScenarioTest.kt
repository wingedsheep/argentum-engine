package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Supper for Spiders (HOB #86) — {1}{B} Instant.
 *
 * "Put onto the battlefield under your control all creature cards in your opponents' graveyards
 * that were put there from the battlefield this turn. They are Food artifacts with '{2}, {T},
 * Sacrifice this artifact: You gain 3 life.' (They lose all other types and subtypes.)"
 *
 * Three things worth proving: the "from the battlefield this turn" gate really excludes a creature
 * card that reached the graveyard another way, the returned card lands under *your* control, and
 * the transform replaces card types and subtypes (artifact Food, no longer a creature Bear) without
 * stripping the card's own abilities — the text removes types, not abilities.
 */
class SupperForSpidersScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        test("reanimates an opponent's creature that died this turn, as a Food artifact you control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Supper for Spiders")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Shock", bears).error shouldBe null
            game.resolveStack()
            game.checkStateBasedActions()

            withClue("the Bears died from the battlefield this turn") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            game.castSpell(1, "Supper for Spiders").error shouldBe null
            game.resolveStack()

            val returned = game.findPermanent("Grizzly Bears")
            withClue("the Bears came back onto the battlefield") { (returned != null) shouldBe true }

            val projected = stateProjector.project(game.state)
            withClue("under your control, not its owner's") {
                projected.getController(returned!!) shouldBe game.player1Id
            }
            withClue("it is a Food artifact and no longer a creature or a Bear") {
                projected.hasType(returned!!, "ARTIFACT") shouldBe true
                projected.isCreature(returned) shouldBe false
                projected.getSubtypes(returned) shouldBe setOf("Food")
            }
        }

        test("a creature card that reached the graveyard another way is left behind") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Supper for Spiders")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInGraveyard(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Supper for Spiders").error shouldBe null
            game.resolveStack()

            withClue("it was never on the battlefield this turn, so it stays in the graveyard") {
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe false
            }
        }

        test("your own dead creatures are not returned — only your opponents'") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Supper for Spiders")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ownBears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Shock", ownBears).error shouldBe null
            game.resolveStack()
            game.checkStateBasedActions()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true

            game.castSpell(1, "Supper for Spiders").error shouldBe null
            game.resolveStack()

            withClue("the spell only reads opponents' graveyards") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
        }
    }
}
