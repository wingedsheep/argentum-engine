package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Uncle Istvan — "Prevent all damage that would be dealt to this creature by
 * creatures."
 *
 * Two things worth holding down. He must survive a blocker that would otherwise be lethal (the
 * prevention is not capped at his toughness), and the prevention must be scoped to *creature*
 * sources — a burn spell still kills him. A `PreventDamage` written without the source filter would
 * pass the first test and fail the second, which is the mistake this guards.
 */
class UncleIstvanScenarioTest : ScenarioTestBase() {

    init {
        context("Uncle Istvan — damage from creatures is prevented") {

            test("survives blocking a creature far larger than him, unmarked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Uncle Istvan", summoningSickness = false)
                    // A 6/4 ground creature — twice lethal to a 1/3, and blockable (a flier
                    // would fail the block-legality check before prevention ever came up).
                    .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false)
                    .withActivePlayer(2)
                    .build()

                val istvan = game.findPermanent("Uncle Istvan")!!
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Craw Wurm" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Uncle Istvan" to listOf("Craw Wurm"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                withClue("all of the creature's damage is prevented, so he is still here") {
                    game.isOnBattlefield("Uncle Istvan") shouldBe true
                }
                withClue("prevented damage is never marked at all") {
                    game.state.getEntity(istvan)?.get<DamageComponent>()?.amount ?: 0 shouldBe 0
                }
            }

            test("a noncreature source still damages him — the filter is creatures only") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Uncle Istvan", summoningSickness = false)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val istvan = game.findPermanent("Uncle Istvan")!!
                game.castSpell(2, "Lightning Bolt", istvan).error shouldBe null
                game.resolveStack()

                withClue("3 damage from an instant is not prevented, and kills a 1/3") {
                    game.isOnBattlefield("Uncle Istvan") shouldBe false
                }
            }
        }
    }
}
