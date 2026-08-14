package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Archon of the Wild Rose — the aura-control-scoped "enchanted" predicate
 * (`StatePredicate.IsEnchantedByAura`) resolved through layer projection.
 */
class ArchonOfTheWildRoseScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? = game.state.projectedState.getToughness(id)
    private fun hasFlying(game: TestGame, id: EntityId): Boolean =
        game.state.projectedState.hasKeyword(id, Keyword.FLYING)

    init {
        context("Archon of the Wild Rose — 'enchanted by Auras you control have base P/T 4/4 and flying'") {
            test("your creature carrying your own Aura becomes a 4/4 flier") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Archon of the Wild Rose")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                withClue("base P/T is set to 4/4 in layer 7b") {
                    power(game, bears) shouldBe 4
                    toughness(game, bears) shouldBe 4
                }
                withClue("and the same printed ability grants flying") {
                    hasFlying(game, bears) shouldBe true
                }
            }

            test("an opponent's Aura on your creature does NOT switch it on") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Archon of the Wild Rose")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    // Player 2 controls the Aura; player 1 controls the creature.
                    .withCardAttachedTo(2, "Pacifism", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                withClue("this is the whole difference from A Tale for the Ages' plain 'enchanted'") {
                    power(game, bears) shouldBe 2
                    toughness(game, bears) shouldBe 2
                    hasFlying(game, bears) shouldBe false
                }
            }

            test("your Aura on an opponent's creature does not buff it either") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Archon of the Wild Rose")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                withClue("'creatures you control' scopes the creature, not the Aura") {
                    power(game, bears) shouldBe 2
                    toughness(game, bears) shouldBe 2
                    hasFlying(game, bears) shouldBe false
                }
            }

            test("Equipment is not an Aura — an equipped creature is untouched") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Archon of the Wild Rose")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Whispersilk Cloak", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                withClue("the Cloak is Equipment, so the Archon does not see the Bears") {
                    power(game, bears) shouldBe 2
                    toughness(game, bears) shouldBe 2
                }
            }

            test("'other' excludes the Archon itself even if it is enchanted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Archon of the Wild Rose")
                    .withCardAttachedTo(1, "Pacifism", "Archon of the Wild Rose")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val archon = game.findPermanent("Archon of the Wild Rose").shouldNotBeNull()

                withClue("the Archon keeps its printed 4/4 rather than re-setting its own base P/T") {
                    power(game, archon) shouldBe 4
                    toughness(game, archon) shouldBe 4
                }
            }
        }
    }
}
