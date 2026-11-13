package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Serpentine Spike (BFZ #133) — {5}{R}{R} Sorcery, devoid.
 *
 *   Serpentine Spike deals 2 damage to target creature, 3 damage to another target creature, and 4
 *   damage to a third target creature. If a creature dealt damage this way would die this turn,
 *   exile it instead.
 *
 * Each slot gets its own amount, and each creature dealt damage carries its own exile-instead mark:
 * the ones killed by the Spike are exiled, and the survivor is exiled when it dies later that turn.
 */
class SerpentineSpikeScenarioTest : ScenarioTestBase() {

    init {
        context("Serpentine Spike") {

            test("deals 2, 3 and 4 to three creatures; each one dealt damage is exiled when it dies this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Serpentine Spike")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Siege Mastodon")
                    .withLandsOnBattlefield(1, "Mountain", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val mastodon = game.findPermanent("Siege Mastodon")!!
                val spike = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Serpentine Spike"
                }

                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        spike,
                        listOf(
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Permanent(giant),
                            ChosenTarget.Permanent(mastodon),
                        )
                    )
                )
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("2 damage kills the 2/2 Bears and 3 kills the 3/3 Giant; both are exiled, not put in the graveyard") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.isInExile(2, "Hill Giant") shouldBe true
                    game.isInGraveyard(2, "Hill Giant") shouldBe false
                }
                withClue("the 3/5 Mastodon took exactly 4 damage from the third slot and survives") {
                    game.isOnBattlefield("Siege Mastodon") shouldBe true
                    game.state.getEntity(mastodon)?.get<DamageComponent>()?.amount shouldBe 4
                }

                game.castSpell(1, "Lightning Bolt", mastodon).error shouldBe null
                game.resolveStack()

                withClue("the Mastodon was dealt damage by the Spike, so dying later this turn exiles it") {
                    game.isOnBattlefield("Siege Mastodon") shouldBe false
                    game.isInGraveyard(2, "Siege Mastodon") shouldBe false
                    game.isInExile(2, "Siege Mastodon") shouldBe true
                }
            }
        }
    }
}
