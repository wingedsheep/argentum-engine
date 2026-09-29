package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.ElvishVatkeeper
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Elvish Vatkeeper (MOM #223) — "When this creature enters, incubate 2.
 * {5}: Transform target Incubator token you control. Double the number of +1/+1 counters on it."
 */
class ElvishVatkeeperScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Elvish Vatkeeper") {
            test("enters and incubates 2; {5} transforms the Incubator and doubles its counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Elvish Vatkeeper")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Elvish Vatkeeper").error shouldBe null
                game.resolveStack()

                val incubator = game.findPermanent("Incubator").shouldNotBeNull()
                game.plusCounters(incubator) shouldBe 2
                withClue("an untransformed Incubator is not a creature") {
                    game.state.projectedState.isCreature(incubator) shouldBe false
                }

                val vatkeeper = game.findPermanent("Elvish Vatkeeper").shouldNotBeNull()
                val abilityId = ElvishVatkeeper.activatedAbilities.single().id
                game.execute(
                    ActivateAbility(
                        game.player1Id, vatkeeper, abilityId,
                        targets = listOf(ChosenTarget.Permanent(incubator))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the token transformed into a Phyrexian artifact creature") {
                    game.state.projectedState.isCreature(incubator) shouldBe true
                }
                withClue("two +1/+1 counters doubled to four") {
                    game.plusCounters(incubator) shouldBe 4
                    game.state.projectedState.getPower(incubator) shouldBe 4
                }
            }

            test("the ability can't target a non-Incubator creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Elvish Vatkeeper")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vatkeeper = game.findPermanent("Elvish Vatkeeper")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = ElvishVatkeeper.activatedAbilities.single().id
                game.execute(
                    ActivateAbility(
                        game.player1Id, vatkeeper, abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldNotBe null
            }
        }
    }
}
