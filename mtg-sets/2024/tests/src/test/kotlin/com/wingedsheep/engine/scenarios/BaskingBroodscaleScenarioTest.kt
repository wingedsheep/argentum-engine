package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.BaskingBroodscale
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Basking Broodscale (MH3) — "{1}{G}: Adapt 1." and "Whenever one or more +1/+1 counters are put
 * on this creature, you may create a 0/1 colorless Eldrazi Spawn creature token with
 * 'Sacrifice this token: Add {C}.'"
 */
class BaskingBroodscaleScenarioTest : ScenarioTestBase() {

    init {
        context("Basking Broodscale") {
            val adapt = BaskingBroodscale.activatedAbilities[0].id

            fun TestGame.plusOnes(entity: com.wingedsheep.sdk.model.EntityId) =
                state.getEntity(entity)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            test("adapt adds a counter, triggering an optional Spawn; a second adapt does nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Basking Broodscale")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val broodscale = game.findPermanent("Basking Broodscale")!!

                game.execute(ActivateAbility(game.player1Id, broodscale, adapt)).error shouldBe null
                game.resolveStack()
                game.plusOnes(broodscale) shouldBe 1

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()
                withClue("One Eldrazi Spawn token") {
                    game.findPermanents("Eldrazi Spawn") shouldHaveSize 1
                }

                game.execute(ActivateAbility(game.player1Id, broodscale, adapt)).error shouldBe null
                game.resolveStack()
                game.plusOnes(broodscale) shouldBe 1
                game.state.stack.isEmpty() shouldBe true
                game.getPendingDecision() shouldBe null
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 1
            }

            test("declining the trigger creates no token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Basking Broodscale")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val broodscale = game.findPermanent("Basking Broodscale")!!

                game.execute(ActivateAbility(game.player1Id, broodscale, adapt)).error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 0
            }
        }
    }
}
