package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Flame-Blessed Bolt (VOW #158) — {R} Instant.
 *
 * "Flame-Blessed Bolt deals 2 damage to target creature or planeswalker. If that creature or
 *  planeswalker would die this turn, exile it instead."
 *
 * The spell deals 2 damage then marks the target with a death-replacement that sends it to exile
 * instead of the graveyard if it dies this turn.
 */
class FlameBlessedBoltScenarioTest : ScenarioTestBase() {

    init {
        context("Flame-Blessed Bolt") {

            test("kills a 1-toughness creature and exiles it instead of putting it in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Flame-Blessed Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Savannah Lions") // 1/1
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findPermanent("Savannah Lions")!!
                game.castSpell(1, "Flame-Blessed Bolt", lions).error shouldBe null
                game.resolveStack()

                withClue("the 1/1 should be exiled, not in the graveyard") {
                    game.state.getZone(ZoneKey(game.player2Id, Zone.EXILE)) shouldContain lions
                    game.state.getZone(ZoneKey(game.player2Id, Zone.GRAVEYARD)) shouldNotContain lions
                }
            }

            test("a creature that survives the 2 damage stays on the battlefield with damage marked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Flame-Blessed Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3, survives 2 damage
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Flame-Blessed Bolt", giant).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant survives 2 damage and stays in play") {
                    game.findPermanent("Hill Giant") shouldBe giant
                    game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
                }
            }
        }
    }
}
