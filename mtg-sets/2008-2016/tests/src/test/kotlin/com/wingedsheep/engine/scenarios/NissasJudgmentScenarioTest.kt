package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nissa's Judgment (OGW #139, {4}{G}, Sorcery).
 *
 *   Support 2. (Put a +1/+1 counter on each of up to two target creatures.)
 *   Choose up to one target creature an opponent controls. Each creature you control with a +1/+1
 *   counter on it deals damage equal to its power to that creature.
 *
 * Support finishes before the damage group is gathered, so a creature that just got its counter deals
 * damage with its boosted power, a creature of yours without a counter deals none, and the counters
 * land only on the support targets — never on the opposing target.
 */
class NissasJudgmentScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.plusOneCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun ScenarioTestBase.TestGame.damageOn(id: EntityId) =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Nissa's Judgment")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Craw Wurm")
        .withLandsOnBattlefield(1, "Forest", 5)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("the supported creature deals its boosted power; a creature without a counter deals nothing") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val wurm = game.findPermanent("Craw Wurm")!!

            val cast = game.execute(
                CastSpell(
                    game.player1Id, game.findCardsInHand(1, "Nissa's Judgment").first(),
                    targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(wurm)),
                    targetGroupCounts = listOf(1, 1),
                )
            )
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("only the support target got a counter") {
                game.plusOneCounters(bears) shouldBe 1
                game.plusOneCounters(giant) shouldBe 0
                game.plusOneCounters(wurm) shouldBe 0
            }
            withClue("Grizzly Bears (3/3 with its counter) dealt 3; Hill Giant has no counter and dealt nothing") {
                game.findPermanent("Craw Wurm") shouldBe wurm
                game.damageOn(wurm) shouldBe 3
            }
        }

        test("two supported creatures each deal their boosted power and kill the opposing creature") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val wurm = game.findPermanent("Craw Wurm")!!

            val cast = game.execute(
                CastSpell(
                    game.player1Id, game.findCardsInHand(1, "Nissa's Judgment").first(),
                    targets = listOf(
                        ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant), ChosenTarget.Permanent(wurm),
                    ),
                    targetGroupCounts = listOf(2, 1),
                )
            )
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("3 from Grizzly Bears + 4 from Hill Giant is lethal to the 6/4 Craw Wurm") {
                game.findPermanent("Craw Wurm") shouldBe null
            }
            game.plusOneCounters(bears) shouldBe 1
            game.plusOneCounters(giant) shouldBe 1
        }

        test("with no opposing target chosen, support still resolves and nothing is dealt damage") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val wurm = game.findPermanent("Craw Wurm")!!

            val cast = game.execute(
                CastSpell(
                    game.player1Id, game.findCardsInHand(1, "Nissa's Judgment").first(),
                    targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant)),
                    targetGroupCounts = listOf(2, 0),
                )
            )
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.plusOneCounters(bears) shouldBe 1
            game.plusOneCounters(giant) shouldBe 1
            game.damageOn(wurm) shouldBe 0
            game.damageOn(bears) shouldBe 0
            game.damageOn(giant) shouldBe 0
        }
    }
}
