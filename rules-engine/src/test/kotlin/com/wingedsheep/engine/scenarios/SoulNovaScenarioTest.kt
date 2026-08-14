package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Soul Nova (MRD #25).
 *
 * {3}{W}{W} Instant
 * "Exile target attacking creature and all Equipment attached to it."
 *
 * The load-bearing detail is ordering: the attached Equipment is gathered into a pipeline slot
 * *before* the creature is exiled, because a permanent's attachment list is cleared the moment it
 * changes zones. Exile first and the Equipment sweep silently finds nothing — which is exactly the
 * bug these tests pin.
 */
class SoulNovaScenarioTest : ScenarioTestBase() {

    init {
        context("Soul Nova") {

            test("exiles the attacking creature and the Equipment attached to it") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInHand(1, "Soul Nova")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(2, "Bonesplitter", "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passPriority() // attacker passes; the defender gets priority

                val bears = game.findPermanent("Grizzly Bears")
                bears.shouldNotBeNull()
                game.castSpell(1, "Soul Nova", bears).error shouldBe null
                game.resolveStack()

                withClue("The attacking creature is exiled") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
                withClue("The Equipment attached to it rides along into exile") {
                    game.findPermanent("Bonesplitter") shouldBe null
                    game.isInExile(2, "Bonesplitter") shouldBe true
                }
            }

            test("exiles a bare attacking creature with no Equipment to sweep") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInHand(1, "Soul Nova")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passPriority()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Soul Nova", bears).error shouldBe null
                game.resolveStack()

                withClue("An empty attachment gather is a no-op, not an error") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
            }

            test("leaves Equipment attached to a different creature alone") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInHand(1, "Soul Nova")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardAttachedTo(2, "Bonesplitter", "Centaur Courser")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Only the Bears attack, so only the Bears are a legal target.
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passPriority()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Soul Nova", bears).error shouldBe null
                game.resolveStack()

                withClue("The targeted attacker is exiled") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
                withClue("Equipment on another creature is untouched — only 'attached to it' counts") {
                    game.findPermanent("Bonesplitter").shouldNotBeNull()
                    game.isInExile(2, "Bonesplitter") shouldBe false
                }
            }
        }
    }
}
