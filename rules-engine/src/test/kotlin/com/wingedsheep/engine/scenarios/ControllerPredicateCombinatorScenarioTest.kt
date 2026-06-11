package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario coverage for the [ControllerPredicate] combinators (sdk-analysis §1.3):
 * `And` / `Or` / `Not` must evaluate through [com.wingedsheep.engine.handlers.PredicateEvaluator]
 * against the *projected* controller, not the base [ControllerComponent].
 *
 * The setup steals a creature with Threaten (a `Duration.EndOfTurn` control-changing floating
 * effect — visible only via state projection; the base ControllerComponent still names the
 * owner). A combinator that read base state instead of projection would misclassify the stolen
 * creature in both tests.
 */
class ControllerPredicateCombinatorScenarioTest : ScenarioTestBase() {

    // "Destroy all creatures an opponent owns that you control." — And over an owner
    // leaf and a controller leaf, the heterogeneous shape that used to need anyOf.
    private val repossessionAudit = card("Repossession Audit") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Destroy all creatures an opponent owns that you control."
        spell {
            effect = Effects.DestroyAll(
                GameObjectFilter.Creature.withControllerPredicate(
                    ControllerPredicate.And(
                        listOf(
                            ControllerPredicate.OwnedByOpponent,
                            ControllerPredicate.ControlledByYou,
                        )
                    )
                )
            )
        }
    }

    // "Destroy all creatures you don't control." — Not over a controller leaf.
    private val purgeOfTheUnruled = card("Purge of the Unruled") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Destroy all creatures you don't control."
        spell {
            effect = Effects.DestroyAll(
                GameObjectFilter.Creature.withControllerPredicate(
                    ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        }
    }

    init {
        cardRegistry.register(repossessionAudit)
        cardRegistry.register(purgeOfTheUnruled)

        test("And(OwnedByOpponent, ControlledByYou) destroys only the stolen creature") {
            val game = scenario()
                .withPlayers("Thief", "Victim")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardInHand(1, "Threaten")
                .withCardInHand(1, "Repossession Audit")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bearsId = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Threaten", bearsId)
            game.resolveStack()

            // Stolen: base controller component still says player 2; projection says player 1.
            game.state.projectedState.getController(bearsId) shouldBe game.player1Id

            game.castSpell(1, "Repossession Audit")
            game.resolveStack()

            // Only the stolen bears match (owned by an opponent AND controlled by you).
            game.findPermanent("Grizzly Bears").shouldBeNull()
            game.findPermanent("Glory Seeker").shouldNotBeNull()   // owned by you
            game.findPermanent("Hill Giant").shouldNotBeNull()     // not controlled by you
            game.state.getGraveyard(game.player2Id).size shouldBe 1
        }

        test("Not(ControlledByYou) spares a creature you only control via projection") {
            val game = scenario()
                .withPlayers("Thief", "Victim")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardInHand(1, "Threaten")
                .withCardInHand(1, "Purge of the Unruled")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bearsId = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Threaten", bearsId)
            game.resolveStack()

            game.castSpell(1, "Purge of the Unruled")
            game.resolveStack()

            // The stolen bears are controlled by you (projection) — spared. A base-state
            // read would have destroyed them alongside the Hill Giant.
            game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.findPermanent("Glory Seeker").shouldNotBeNull()
            game.findPermanent("Hill Giant").shouldBeNull()
        }
    }
}
