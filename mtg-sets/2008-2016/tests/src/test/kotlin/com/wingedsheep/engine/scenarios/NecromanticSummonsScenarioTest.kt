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
 * Necromantic Summons (ORI #110) — "Put target creature card from a graveyard onto the battlefield
 * under your control. Spell mastery — If there are two or more instant and/or sorcery cards in your
 * graveyard, that creature enters with two additional +1/+1 counters on it."
 *
 * Worth proving: the reanimation reaches into an opponent's graveyard and steals the creature, and
 * the spell-mastery count is an entry counter whose amount is read from the *caster's* graveyard at
 * resolution — instants in the opponent's graveyard don't turn it on.
 */
class NecromanticSummonsScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Necromantic Summons") {

            test("with spell mastery, an opponent's creature enters under your control with two +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Necromantic Summons")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Doom Blade")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellTargetingGraveyardCard(1, "Necromantic Summons", 2, "Grizzly Bears")
                withClue("casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("the Bears left Bob's graveyard") { game.isInGraveyard(2, "Grizzly Bears") shouldBe false }
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Alice controls the reanimated creature") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }
                withClue("spell mastery: it entered with two +1/+1 counters") {
                    game.plusOneCounters(bears) shouldBe 2
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
            }

            test("without spell mastery in your own graveyard, it enters with no counters") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Necromantic Summons")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInGraveyard(1, "Doom Blade")
                    .withCardInGraveyard(2, "Lightning Bolt")
                    .withCardInGraveyard(2, "Lightning Bolt")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellTargetingGraveyardCard(1, "Necromantic Summons", 2, "Grizzly Bears")
                withClue("casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Alice controls the reanimated creature") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }
                withClue("one instant in Alice's graveyard — Bob's don't count — so no counters") {
                    game.plusOneCounters(bears) shouldBe 0
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
            }
        }
    }
}
