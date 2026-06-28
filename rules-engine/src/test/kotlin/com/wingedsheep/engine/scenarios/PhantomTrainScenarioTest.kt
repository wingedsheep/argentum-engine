package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fin.cards.PhantomTrain
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Phantom Train {3}{B} Artifact — Vehicle 4/4 (FIN #110).
 *
 * Trample
 * Sacrifice another artifact or creature: Put a +1/+1 counter on this Vehicle. It becomes a Spirit
 * artifact creature in addition to its other types until end of turn.
 *
 * Proves the self-animating activated ability: paying the "sacrifice another artifact or creature"
 * cost removes the chosen permanent, puts a +1/+1 counter on the Train, and makes it a creature
 * (a 5/5 with the counter, keeping its Vehicle subtype) for the rest of the turn — then it reverts
 * to a noncreature Vehicle, while the +1/+1 counter persists.
 */
class PhantomTrainScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(PhantomTrain))

        context("Phantom Train") {

            test("sacrificing a creature animates it into a 5/5 Spirit artifact creature until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Phantom Train")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val train = game.findPermanent("Phantom Train")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("starts as a noncreature artifact Vehicle") {
                    val before = game.state.projectedState
                    before.isCreature(train) shouldBe false
                    before.hasType(train, "ARTIFACT") shouldBe true
                    before.hasSubtype(train, "Vehicle") shouldBe true
                }
                withClue("starts with no +1/+1 counters") {
                    val counters = game.state.getEntity(train)?.get<CountersComponent>()?.counters ?: emptyMap()
                    counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe null
                }

                val abilityId = cardRegistry.getCard("Phantom Train")!!
                    .script.activatedAbilities[0].id

                val activate = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = train,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                    )
                )
                withClue("activating the sacrifice ability should succeed: ${activate.error}") {
                    activate.error shouldBe null
                }
                game.resolveStack()

                withClue("the sacrificed creature is gone") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                withClue("Phantom Train gains a +1/+1 counter") {
                    val counters = game.state.getEntity(train)?.get<CountersComponent>()?.counters ?: emptyMap()
                    counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 1
                }

                val animated = game.state.projectedState
                withClue("becomes a creature") { animated.isCreature(train) shouldBe true }
                withClue("is a Spirit") { animated.hasSubtype(train, "Spirit") shouldBe true }
                withClue("still an artifact (in addition to its other types)") {
                    animated.hasType(train, "ARTIFACT") shouldBe true
                }
                withClue("still a Vehicle") { animated.hasSubtype(train, "Vehicle") shouldBe true }
                withClue("base 4/4 plus the +1/+1 counter = 5/5 power") { animated.getPower(train) shouldBe 5 }
                withClue("base 4/4 plus the +1/+1 counter = 5/5 toughness") { animated.getToughness(train) shouldBe 5 }
            }

            test("reverts to a noncreature Vehicle at end of turn but keeps the +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Phantom Train")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val train = game.findPermanent("Phantom Train")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.getCard("Phantom Train")!!
                    .script.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = train,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                    )
                )
                game.resolveStack()
                game.state.projectedState.isCreature(train) shouldBe true

                // Advance into the opponent's turn; the until-end-of-turn animation is cleaned up.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                val reverted = game.state.projectedState
                withClue("no longer a creature once the turn ends") { reverted.isCreature(train) shouldBe false }
                withClue("still an artifact Vehicle") {
                    reverted.hasType(train, "ARTIFACT") shouldBe true
                    reverted.hasSubtype(train, "Vehicle") shouldBe true
                }
                withClue("the +1/+1 counter persists past end of turn") {
                    val counters = game.state.getEntity(train)?.get<CountersComponent>()?.counters ?: emptyMap()
                    counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 1
                }
            }
        }
    }
}
