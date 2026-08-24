package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Dance of Many (DRK #22).
 *
 * {U}{U} Enchantment
 * "When this enchantment enters, create a token that's a copy of target nontoken creature.
 *  When this enchantment leaves the battlefield, exile the token.
 *  When the token leaves the battlefield, sacrifice this enchantment.
 *  At the beginning of your upkeep, sacrifice this enchantment unless you pay {U}{U}."
 *
 * Three of the four clauses only mean anything if they find *the* token this enchantment made, so
 * the tests exercise the linkage in both directions: enchantment goes → token goes, token goes →
 * enchantment goes.
 */
class DanceOfManyScenarioTest : ScenarioTestBase() {

    init {
        context("Dance of Many") {

            test("entering copies the targeted creature") {
                val game = scenario()
                    .withPlayers("Dancer", "Opponent")
                    .withCardInHand(1, "Dance of Many")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dance of Many").error shouldBe null
                game.resolveStack()
                // The ETB trigger targets; pick the opponent's Angel.
                game.selectTargets(listOf(game.findPermanent("Serra Angel")!!))
                game.resolveStack()

                withClue("the ETB trigger minted a copy of the only legal target") {
                    game.findPermanents("Serra Angel").size shouldBe 2
                }
            }

            test("when the enchantment leaves, its token is exiled") {
                val game = scenario()
                    .withPlayers("Dancer", "Opponent")
                    .withCardInHand(1, "Dance of Many")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dance of Many").error shouldBe null
                game.resolveStack()
                // The ETB trigger targets; pick the opponent's Angel.
                game.selectTargets(listOf(game.findPermanent("Serra Angel")!!))
                game.resolveStack()
                game.findPermanents("Serra Angel").size shouldBe 2

                // Blow up the enchantment; the token should follow it off the battlefield.
                val dance = game.findPermanent("Dance of Many")!!
                game.castSpell(1, "Disenchant", targetId = dance).error shouldBe null
                game.resolveStack()

                withClue("the enchantment is gone") {
                    game.findPermanent("Dance of Many").shouldBeNull()
                }
                withClue("and so is its token — only the original Angel is left") {
                    game.findPermanents("Serra Angel").size shouldBe 1
                }
            }

            test("killing the token sacrifices the enchantment") {
                // The other direction of the linkage, and the clause with the least obvious
                // implementation: that trigger has to be ANY-bound with a provenance filter,
                // because the permanent leaving is the token, not the enchantment.
                val game = scenario()
                    .withPlayers("Dancer", "Opponent")
                    .withCardInHand(1, "Dance of Many")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val original = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Dance of Many").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(original))
                game.resolveStack()

                val bears = game.findPermanents("Grizzly Bears")
                bears.size shouldBe 2
                // Bolt the copy, not the original — the token is the id that wasn't there before.
                val token = bears.single { it != original }
                game.castSpell(1, "Lightning Bolt", targetId = token).error shouldBe null
                game.resolveStack()

                withClue("the token died") {
                    game.findPermanents("Grizzly Bears").size shouldBe 1
                }
                withClue("and took the enchantment with it") {
                    game.findPermanent("Dance of Many").shouldBeNull()
                }
            }

            test("a permanent that isn't the token leaves and the enchantment stays put") {
                // The provenance gate, from the other side: "when *the token* leaves the
                // battlefield" must not fire for any other permanent going to the graveyard —
                // not the creature the token copied, and not a land.
                val game = scenario()
                    .withPlayers("Dancer", "Opponent")
                    .withCardInHand(1, "Dance of Many")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Stone Rain")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val original = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Dance of Many").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(original))
                game.resolveStack()
                game.findPermanents("Grizzly Bears").size shouldBe 2

                // Kill the creature the token was copied from — a nontoken permanent leaving.
                game.castSpell(1, "Lightning Bolt", targetId = original).error shouldBe null
                game.resolveStack()

                withClue("the original died, the token did not") {
                    game.findPermanents("Grizzly Bears").size shouldBe 1
                }
                withClue("and the enchantment was not sacrificed — that wasn't its token") {
                    game.findPermanent("Dance of Many").shouldNotBeNull()
                }

                // And a land leaving is no different (the playtest report that surfaced this).
                game.castSpell(1, "Stone Rain", targetId = game.findPermanent("Plains")!!).error shouldBe null
                game.resolveStack()

                withClue("a land going to the graveyard is still not the token") {
                    game.findPermanent("Dance of Many").shouldNotBeNull()
                    game.findPermanents("Grizzly Bears").size shouldBe 1
                }
            }

            test("declining the upkeep {U}{U} sacrifices the enchantment and takes the token with it") {
                val game = scenario()
                    .withPlayers("Dancer", "Opponent")
                    .withCardInHand(1, "Dance of Many")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dance of Many").error shouldBe null
                game.resolveStack()
                // The ETB trigger targets; pick the opponent's Angel.
                game.selectTargets(listOf(game.findPermanent("Serra Angel")!!))
                game.resolveStack()
                game.findPermanents("Serra Angel").size shouldBe 2

                // Round the table to the Dancer's next upkeep.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()

                game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("refusing the ransom sacrifices the enchantment") {
                    game.findPermanent("Dance of Many").shouldBeNull()
                }
                withClue("which exiles the token it made") {
                    game.findPermanents("Serra Angel").size shouldBe 1
                }
            }
        }
    }
}
