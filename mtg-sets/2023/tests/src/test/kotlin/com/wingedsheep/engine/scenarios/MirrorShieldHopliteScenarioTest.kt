package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mirror-Shield Hoplite (MOM #247) — {R}{W} 2/2 vigilance. "Whenever a creature you control
 * becomes the target of a backup ability, copy that ability. You may choose new targets for the
 * copy. This ability triggers only once each turn."
 *
 * Driven by Boon-Bringer Valkyrie's Backup 1 (+1/+1 counter; flying, first strike and lifelink for
 * another creature). The copy keeps the Valkyrie as its source, so the Hoplite — not the Valkyrie —
 * gains the abilities when the copy is pointed at it.
 */
class MirrorShieldHopliteScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Mirror-Shield Hoplite") {

            test("copies the backup ability; the copy may take a new target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Boon-Bringer Valkyrie")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(1, "Mirror-Shield Hoplite")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val hoplite = game.findPermanent("Mirror-Shield Hoplite")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Boon-Bringer Valkyrie").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(bears)).error shouldBe null

                withClue("backup and the Hoplite's trigger are both on the stack") {
                    game.state.stack.size shouldBe 2
                }
                game.resolveStack()
                withClue("the copy asks for new targets") { game.hasPendingDecision() shouldBe true }
                game.selectTargets(listOf(hoplite)).error shouldBe null
                game.resolveStack()

                withClue("the original backup resolves on the Bears") {
                    plusOnes(game, bears) shouldBe 1
                    game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
                }
                withClue("the copy resolves on the Hoplite, which is not the copy's source") {
                    plusOnes(game, hoplite) shouldBe 1
                    game.state.projectedState.hasKeyword(hoplite, Keyword.FLYING) shouldBe true
                    game.state.projectedState.hasKeyword(hoplite, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("the copy targeting the Hoplite doesn't trigger it again (once each turn)") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.size shouldBe 0
                    plusOnes(game, game.findPermanent("Boon-Bringer Valkyrie")!!) shouldBe 0
                }
            }

            test("a spell targeting a creature you control is not a backup ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Mirror-Shield Hoplite")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Giant Growth", bears).error shouldBe null
                withClue("only Giant Growth is on the stack") { game.state.stack.size shouldBe 1 }
            }

            test("an opponent's creature being targeted by backup doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Boon-Bringer Valkyrie")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(1, "Mirror-Shield Hoplite")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val theirBears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Boon-Bringer Valkyrie").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(theirBears)).error shouldBe null
                withClue("only the backup trigger is on the stack") { game.state.stack.size shouldBe 1 }
                game.resolveStack()
                plusOnes(game, theirBears) shouldBe 1
                plusOnes(game, game.findPermanent("Mirror-Shield Hoplite")!!) shouldBe 0
            }
        }
    }
}
