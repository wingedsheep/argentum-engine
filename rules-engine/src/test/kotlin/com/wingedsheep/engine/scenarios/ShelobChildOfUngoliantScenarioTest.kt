package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Shelob, Child of Ungoliant (LTR #230) — {4}{B}{G} Legendary Spider Demon, 8/8.
 *
 * Deathtouch, ward {2}
 * Other Spiders you control have deathtouch and ward {2}.
 * Whenever another creature dealt damage this turn by a Spider you controlled dies, create a token
 * that's a copy of that creature, except it's a Food artifact with "{2}, {T}, Sacrifice this token:
 * You gain 3 life," and it loses all other card types.
 *
 * Exercises:
 * - the intrinsic deathtouch + ward {2} keywords;
 * - the "other Spiders you control" anthem granting deathtouch + ward {2} to another Spider;
 * - the observer dealt-damage-by-source-dies trigger (filtered to "a Spider you controlled") plus
 *   the new token-copy overrides (`overrideCardTypes` = ARTIFACT, `addedSubtypes` = Food,
 *   granted Food sacrifice activated ability).
 */
class ShelobChildOfUngoliantScenarioTest : ScenarioTestBase() {

    init {
        // A vanilla Spider the anthem can buff and that deals lethal (deathtouch) damage.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Pet Spider",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype("Spider")),
                power = 2,
                toughness = 2
            )
        )
        // An opposing creature the Spider can kill in combat.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Doomed Bear",
                manaCost = ManaCost.parse("{2}{G}"),
                subtypes = setOf(Subtype("Bear")),
                power = 2,
                toughness = 3
            )
        )

        context("Shelob, Child of Ungoliant") {

            test("has deathtouch and ward {2}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shelob, Child of Ungoliant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shelob = game.findPermanent("Shelob, Child of Ungoliant")!!
                withClue("Shelob has deathtouch") {
                    game.state.projectedState.hasKeyword(shelob, Keyword.DEATHTOUCH) shouldBe true
                }
                withClue("Shelob has ward") {
                    game.state.projectedState.hasKeyword(shelob, Keyword.WARD) shouldBe true
                }
            }

            test("anthem grants deathtouch and ward {2} to another Spider you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shelob, Child of Ungoliant", summoningSickness = false)
                    .withCardOnBattlefield(1, "Pet Spider", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spider = game.findPermanent("Pet Spider")!!
                withClue("the other Spider gains deathtouch from the anthem") {
                    game.state.projectedState.hasKeyword(spider, Keyword.DEATHTOUCH) shouldBe true
                }
                withClue("the other Spider gains ward from the anthem") {
                    game.state.projectedState.hasKeyword(spider, Keyword.WARD) shouldBe true
                }
            }

            test("a creature dealt damage by a Spider you control that dies becomes a Food-artifact token copy") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shelob, Child of Ungoliant", summoningSickness = false)
                    .withCardOnBattlefield(1, "Pet Spider", summoningSickness = false)
                    .withCardOnBattlefield(2, "Doomed Bear", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // The Spider (granted deathtouch by Shelob) attacks; the Bear blocks and is dealt
                // lethal (deathtouch) damage, then dies during combat damage SBAs.
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Pet Spider" to 2))
                withClue("declaring the Spider as attacker should succeed: ${attack.error}") {
                    attack.error shouldBe null
                }
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                val block = game.declareBlockers(mapOf("Doomed Bear" to listOf("Pet Spider")))
                withClue("declaring the Bear as blocker should succeed: ${block.error}") {
                    block.error shouldBe null
                }

                // Resolve combat damage (auto-resolved while passing to the postcombat main); the
                // Bear dies (deathtouch) and Shelob's trigger fires, then resolve the trigger.
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                withClue("the Doomed Bear died and is no longer the original creature on the battlefield") {
                    game.state.getBattlefield()
                        .any { game.state.getEntity(it)
                            ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                            ?.name == "Doomed Bear" &&
                            !game.state.getEntity(it)!!.has<com.wingedsheep.engine.state.components.identity.TokenComponent>()
                        } shouldBe false
                }

                val token = game.findPermanents("Doomed Bear")
                    .singleOrNull { game.state.getEntity(it)!!.has<com.wingedsheep.engine.state.components.identity.TokenComponent>() }
                    ?: error("Expected exactly one Food token copy of Doomed Bear")

                withClue("token is controlled by Shelob's controller (Player 1)") {
                    game.state.getEntity(token)
                        ?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()
                        ?.playerId shouldBe game.player1Id
                }
                withClue("token is an artifact") {
                    game.state.projectedState.hasType(token, "ARTIFACT") shouldBe true
                }
                withClue("token has the Food subtype") {
                    game.state.projectedState.getSubtypes(token).contains("Food") shouldBe true
                }
                withClue("token lost all other card types — it is no longer a creature") {
                    game.state.projectedState.isCreature(token) shouldBe false
                }
                withClue("token has a granted activated ability (the Food sacrifice ability)") {
                    game.state.grantedActivatedAbilities.any { it.entityId == token } shouldBe true
                }
            }
        }
    }
}
