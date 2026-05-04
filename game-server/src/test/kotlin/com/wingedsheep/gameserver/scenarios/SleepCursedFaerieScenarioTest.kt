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

            test("natural untap step burns down stun counters one per turn") {
                val builder = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardInHand(1, "Sleep-Cursed Faerie")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // Both players need enough library to survive ~7 draw steps without decking out.
                repeat(15) { builder.withCardInLibrary(1, "Island") }
                repeat(15) { builder.withCardInLibrary(2, "Island") }
                val game = builder.build()

                game.castSpell(1, "Sleep-Cursed Faerie").error shouldBe null
                game.resolveStack()

                val faerie = game.findPermanent("Sleep-Cursed Faerie")!!
                fun stunCount() = game.state.getEntity(faerie)
                    ?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0
                fun tapped() = game.state.getEntity(faerie)?.has<TappedComponent>() == true

                stunCount() shouldBe 3
                tapped() shouldBe true

                // Walk through full turns: each pass through P1's untap step should consume
                // one stun counter and leave the Faerie tapped, until the 4th untap step
                // (with stun = 0) finally untaps it.
                fun advanceToNextOwnUntapStep() {
                    game.passUntilPhase(Phase.ENDING, Step.END)        // finish P1 turn
                    game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)  // P2 untap (no effect)
                    game.passUntilPhase(Phase.ENDING, Step.END)        // finish P2 turn
                    game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)  // P1 untap step
                }

                advanceToNextOwnUntapStep()
                withClue("after 1st natural untap step (3 → 2)") {
                    stunCount() shouldBe 2
                    tapped() shouldBe true
                }

                advanceToNextOwnUntapStep()
                withClue("after 2nd natural untap step (2 → 1)") {
                    stunCount() shouldBe 1
                    tapped() shouldBe true
                }

                advanceToNextOwnUntapStep()
                withClue("after 3rd natural untap step (1 → 0)") {
                    stunCount() shouldBe 0
                    tapped() shouldBe true
                }

                advanceToNextOwnUntapStep()
                withClue("after 4th natural untap step (stun depleted, Faerie untaps)") {
                    tapped() shouldBe false
                }
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
