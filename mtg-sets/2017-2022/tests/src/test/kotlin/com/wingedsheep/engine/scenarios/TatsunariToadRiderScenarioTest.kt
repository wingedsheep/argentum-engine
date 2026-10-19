package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tatsunari, Toad Rider (NEO #123) — {2}{B} Legendary Creature — Human Ninja 3/3
 *
 *   Whenever you cast an enchantment spell, if you don't control a creature named Keimi, create
 *   Keimi, a legendary 3/3 black and green Frog creature token with "Whenever you cast an
 *   enchantment spell, each opponent loses 1 life and you gain 1 life."
 *   {1}{G/U}: Tatsunari and target Frog you control can't be blocked this turn except by creatures
 *   with flying or reach.
 *
 * Proves the named-token intervening-if (no second Keimi), that Keimi's own granted trigger drains,
 * and that the activated ability restricts blockers on both Tatsunari and the targeted Frog to
 * creatures with flying *or* reach.
 */
class TatsunariToadRiderScenarioTest : ScenarioTestBase() {
    init {
        test("first enchantment creates Keimi; the next one drains via Keimi and makes no second Keimi") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Tatsunari, Toad Rider")
                .withCardInHand(1, "Glorious Anthem")
                .withCardInHand(1, "Glorious Anthem")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Glorious Anthem").error shouldBe null
            game.resolveStack()

            withClue("Keimi is created by the first enchantment, and it missed that cast") {
                game.findAllPermanents("Keimi").size shouldBe 1
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }

            game.castSpell(1, "Glorious Anthem").error shouldBe null
            game.resolveStack()

            withClue("Keimi already on the battlefield: no second Keimi, and Keimi's trigger drains 1") {
                game.findAllPermanents("Keimi").size shouldBe 1
                game.getLifeTotal(1) shouldBe 21
                game.getLifeTotal(2) shouldBe 19
            }
        }

        test("Tatsunari and the target Frog can be blocked only by creatures with flying or reach") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Tatsunari, Toad Rider")
                .withCardOnBattlefield(1, "Frogmite")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Giant Spider")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tatsunari = game.findPermanent("Tatsunari, Toad Rider")!!
            val frogmite = game.findPermanent("Frogmite")!!
            val abilityId = cardRegistry.getCard("Tatsunari, Toad Rider")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = tatsunari,
                    abilityId = abilityId,
                    targets = listOf(entityIdToChosenTarget(game.state, frogmite)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Tatsunari, Toad Rider" to 2, "Frogmite" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("Grizzly Bears has neither flying nor reach: it can't block the Frog") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Frogmite"))).error shouldNotBe null
            }
            withClue("Grizzly Bears can't block Tatsunari either") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Tatsunari, Toad Rider"))).error shouldNotBe null
            }
            withClue("Giant Spider has reach: it can block Tatsunari") {
                game.declareBlockers(mapOf("Giant Spider" to listOf("Tatsunari, Toad Rider"))).error shouldBe null
            }
        }
    }
}
