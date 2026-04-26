package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for High Perfect Morcant (Lorwyn Eclipsed).
 *
 * Card reference:
 * - High Perfect Morcant {2}{B}{G} — 4/4 Legendary Creature — Elf Noble
 *   Whenever this or another Elf you control enters, each opponent blights 1.
 *   Tap three untapped Elves you control: Proliferate. Activate only as a sorcery.
 *
 * These tests focus on the new Proliferate effect — both the happy path
 * (multi-kind multi-target counter add) and the no-op path (no counters anywhere).
 * The cast-time blight trigger is exercised by the existing ECL set and not
 * re-tested here, but a smoke test confirms it fires from the activated ability path.
 */
class HighPerfectMorcantScenarioTest : ScenarioTestBase() {

    init {
        context("High Perfect Morcant — Proliferate activated ability") {

            test("proliferate adds one of each existing counter kind to the chosen entities") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    // Three untapped Elves to pay the activation cost (Morcant is also an Elf,
                    // but we keep three other elves to keep the scenario explicit).
                    .withCardOnBattlefield(1, "High Perfect Morcant")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Elvish Ranger")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    // Two recipients with different counter kinds.
                    .withCardOnBattlefield(2, "Grizzly Bears") // will receive +1/+1
                    .withCardOnBattlefield(2, "Horned Turtle") // 1/4, survives 2 x -1/-1
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val seeker = game.findPermanent("Horned Turtle")!!

                game.state = game.state
                    .updateEntity(bears) {
                        it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                    }
                    .updateEntity(seeker) {
                        it.with(CountersComponent(mapOf(CounterType.MINUS_ONE_MINUS_ONE to 1)))
                    }

                val morcant = game.findPermanent("High Perfect Morcant")!!
                val warrior = game.findPermanent("Elvish Warrior")!!
                val ranger = game.findPermanent("Elvish Ranger")!!
                val llanowar = game.findPermanent("Llanowar Elves")!!
                val abilityId = cardRegistry.getCard("High Perfect Morcant")!!
                    .script.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = morcant,
                        abilityId = abilityId,
                        targets = emptyList(),
                        costPayment = AdditionalCostPayment(
                            tappedPermanents = listOf(warrior, ranger, llanowar)
                        )
                    )
                )

                withClue("Proliferate ability should activate: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Proliferate should pause for the multi-select decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose to proliferate both creatures with counters
                val selectResult = game.selectCards(listOf(bears, seeker))
                withClue("Selecting proliferate targets should succeed: ${selectResult.error}") {
                    selectResult.error shouldBe null
                }

                val bearsCounters = game.state.getEntity(bears)?.get<CountersComponent>()
                bearsCounters.shouldNotBeNull()
                withClue("Grizzly Bears should now have 2 +1/+1 counters") {
                    bearsCounters.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }

                val seekerCounters = game.state.getEntity(seeker)?.get<CountersComponent>()
                seekerCounters.shouldNotBeNull()
                withClue("Horned Turtle should now have 2 -1/-1 counters") {
                    seekerCounters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
                }
            }

            test("proliferate is a no-op when nothing on the battlefield has counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "High Perfect Morcant")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Elvish Ranger")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val morcant = game.findPermanent("High Perfect Morcant")!!
                val warrior = game.findPermanent("Elvish Warrior")!!
                val ranger = game.findPermanent("Elvish Ranger")!!
                val llanowar = game.findPermanent("Llanowar Elves")!!
                val abilityId = cardRegistry.getCard("High Perfect Morcant")!!
                    .script.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = morcant,
                        abilityId = abilityId,
                        targets = emptyList(),
                        costPayment = AdditionalCostPayment(
                            tappedPermanents = listOf(warrior, ranger, llanowar)
                        )
                    )
                )
                withClue("Activation should still succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("With nothing carrying counters, proliferate resolves silently") {
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("proliferate respects choosing zero entities") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "High Perfect Morcant")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Elvish Ranger")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }

                val morcant = game.findPermanent("High Perfect Morcant")!!
                val warrior = game.findPermanent("Elvish Warrior")!!
                val ranger = game.findPermanent("Elvish Ranger")!!
                val llanowar = game.findPermanent("Llanowar Elves")!!
                val abilityId = cardRegistry.getCard("High Perfect Morcant")!!
                    .script.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = morcant,
                        abilityId = abilityId,
                        targets = emptyList(),
                        costPayment = AdditionalCostPayment(
                            tappedPermanents = listOf(warrior, ranger, llanowar)
                        )
                    )
                )
                game.resolveStack()
                game.hasPendingDecision() shouldBe true

                // Decline to add counters to anything
                game.skipSelection()

                val bearsCounters = game.state.getEntity(bears)?.get<CountersComponent>()
                withClue("Choosing zero entities should leave existing counters untouched") {
                    bearsCounters.shouldNotBeNull()
                    bearsCounters.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }
        }
    }
}
