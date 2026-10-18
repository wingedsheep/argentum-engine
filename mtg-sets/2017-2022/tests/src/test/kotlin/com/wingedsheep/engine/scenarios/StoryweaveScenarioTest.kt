package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Storyweave (NEO #209) — {2}{G} Instant. Choose one —
 * • Put two +1/+1 counters on target creature you control.
 * • Put two lore counters on target Saga you control. The next time one or more enchantment
 *   creatures you control enter this turn, each enters with two additional +1/+1 counters on it.
 *
 * The one-shot grant's own rules live in `NextUseEntryReplacementGrantTest`; this pins both modes
 * and the card's signature play: lore counters push Kumano Faces Kakkazan to chapter III, and the
 * Saga's transformed back face enters with the two counters.
 */
class StoryweaveScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.plusOnes(id: EntityId) =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        context("Storyweave") {

            test("first mode puts two +1/+1 counters on target creature you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Storyweave")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpellWithMode(1, "Storyweave", 0, bears).error shouldBe null
                game.resolveStack()

                game.plusOnes(bears) shouldBe 2
            }

            test("second mode flips the Saga, its back face enters with two counters, and the next one gets none of them") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kumano Faces Kakkazan")
                    .withCardInHand(1, "Storyweave")
                    .withCardInHand(1, "Fang of Shigeki")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Kumano Faces Kakkazan").error shouldBe null
                game.resolveStack()

                val saga = game.findPermanent("Kumano Faces Kakkazan")!!
                game.castSpellWithMode(1, "Storyweave", 1, saga).error shouldBe null
                game.resolveStack()

                withClue("two lore counters took the Saga to chapter III, which returned it transformed") {
                    game.findPermanent("Kumano Faces Kakkazan") shouldBe null
                }
                val etching = game.findPermanent("Etching of Kumano")!!
                game.plusOnes(etching) shouldBe 2

                game.castSpell(1, "Fang of Shigeki").error shouldBe null
                game.resolveStack()
                withClue("the rider modified only the first entry; Fang keeps just chapter II's counter") {
                    game.plusOnes(game.findPermanent("Fang of Shigeki")!!) shouldBe 1
                }
            }
        }
    }
}
