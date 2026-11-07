package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.kld.cards.AnimationModule
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Animation Module (Kaladesh #194).
 *
 * The activated ability is the first card to aim `AddCountersOfChosenKind` at a *player*
 * ("target permanent or player"), so these pin the player path — the kind choice among a
 * player's counters and the placement on the player — plus the permanent path feeding the
 * Module's own "+1/+1 counters put on a permanent you control" trigger and its {1} payment.
 */
class AnimationModuleScenarioTest : ScenarioTestBase() {

    private val moduleAbilityId = AnimationModule.activatedAbilities.first().id

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun seedCounters(game: TestGame, id: EntityId, vararg seeds: Pair<CounterType, Int>) {
        game.state = game.state.updateEntity(id) { c ->
            var component = c.get<CountersComponent>() ?: CountersComponent()
            seeds.forEach { (type, amount) -> component = component.withAdded(type, amount) }
            c.with(component)
        }
    }

    private fun activate(game: TestGame, target: ChosenTarget) =
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = game.findPermanent("Animation Module")!!,
                abilityId = moduleAbilityId,
                targets = listOf(target),
            )
        )

    init {
        cardRegistry.register(AnimationModule)

        context("Animation Module") {

            test("targeting a player with two kinds of counters gives one more of the chosen kind only") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Animation Module")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                seedCounters(game, game.player2Id, CounterType.POISON to 2, CounterType.ENERGY to 3)

                activate(game, ChosenTarget.Player(game.player2Id)).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("two kinds on the player → the controller chooses which") {
                    decision.shouldBeInstanceOf<ChooseOptionDecision>()
                }
                val choice = decision as ChooseOptionDecision
                val energyIndex = choice.options.indexOf(CounterType.ENERGY.printed)
                withClue("both kinds offered: ${choice.options}") {
                    choice.options.size shouldBe 2
                    (energyIndex >= 0) shouldBe true
                }
                game.submitDecision(OptionChosenResponse(choice.id, energyIndex)).error shouldBe null

                withClue("one more energy counter on the targeted player") {
                    counters(game, game.player2Id, CounterType.ENERGY) shouldBe 4
                }
                withClue("the unchosen kind is untouched — this isn't proliferate") {
                    counters(game, game.player2Id, CounterType.POISON) shouldBe 2
                }
            }

            test("adding a +1/+1 counter to your creature triggers the Module; paying {1} makes a Servo") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Animation Module")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                seedCounters(game, bears, CounterType.PLUS_ONE_PLUS_ONE to 1)

                activate(game, ChosenTarget.Permanent(bears)).error shouldBe null
                game.resolveStack()

                withClue("a single kind is placed with no prompt") {
                    counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
                withClue("the placed +1/+1 counter triggers the may-pay") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)
                game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("one 1/1 Servo artifact creature token") {
                    game.findPermanents("Servo Token").size shouldBe 1
                }
            }

            test("declining the {1} creates no Servo") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Animation Module")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                seedCounters(game, bears, CounterType.PLUS_ONE_PLUS_ONE to 1)

                activate(game, ChosenTarget.Permanent(bears)).error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                game.findPermanents("Servo Token").size shouldBe 0
            }
        }
    }
}
