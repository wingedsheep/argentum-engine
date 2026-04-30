package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class SleepCursedFaerieScenarioTest : ScenarioTestBase() {

    init {
        context("Sleep-Cursed Faerie") {
            test("enters tapped with three stun counters") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Sleep-Cursed Faerie")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Sleep-Cursed Faerie")
                withClue("Sleep-Cursed Faerie should cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                val faerie = game.findPermanent("Sleep-Cursed Faerie")!!
                game.state.getEntity(faerie)?.has<TappedComponent>() shouldBe true
                game.state.getEntity(faerie)
                    ?.get<CountersComponent>()
                    ?.getCount(CounterType.STUN) shouldBe 3
            }

            test("activated untap ability removes stun before untapping") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, "Sleep-Cursed Faerie", tapped = true)
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val faerie = game.findPermanent("Sleep-Cursed Faerie")!!
                game.state = game.state.updateEntity(faerie) { container ->
                    container.with(CountersComponent(mapOf(CounterType.STUN to 1)))
                }

                val ability = cardRegistry.getCard("Sleep-Cursed Faerie")!!.script.activatedAbilities[0]

                val firstActivation = game.execute(
                    ActivateAbility(game.player1Id, faerie, ability.id)
                )
                withClue("First untap activation should resolve through stun: ${firstActivation.error}") {
                    firstActivation.error shouldBe null
                }
                game.resolveStack()

                game.state.getEntity(faerie)?.has<TappedComponent>() shouldBe true
                game.state.getEntity(faerie)
                    ?.get<CountersComponent>()
                    ?.getCount(CounterType.STUN) shouldBe 0

                val secondActivation = game.execute(
                    ActivateAbility(game.player1Id, faerie, ability.id)
                )
                withClue("Second untap activation should untap the Faerie: ${secondActivation.error}") {
                    secondActivation.error shouldBe null
                }
                game.resolveStack()

                game.state.getEntity(faerie)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}
