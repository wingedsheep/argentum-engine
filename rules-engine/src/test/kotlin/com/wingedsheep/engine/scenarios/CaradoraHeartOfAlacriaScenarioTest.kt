package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Caradora, Heart of Alacria (DFT #195).
 *
 * Caradora, Heart of Alacria {2}{G}{W} — Legendary Creature — Human Knight 4/2
 * When Caradora enters, you may search your library for a Mount or Vehicle card, reveal it, put it
 * into your hand, then shuffle.
 * If one or more +1/+1 counters would be put on a creature or Vehicle you control, that many plus
 * one +1/+1 counters are put on it instead.
 *
 * The load-bearing claim is the widened recipient on the counter-placement replacement. Hardened
 * Scales' default covers "a creature you control"; Caradora also covers a Vehicle, which is not a
 * creature until something animates it. So the interesting case is an **uncrewed** Vehicle getting
 * the bonus counter, and an opponent's permanent not getting it.
 */
class CaradoraHeartOfAlacriaScenarioTest : ScenarioTestBase() {

    private val mechanicAbilityId
        get() = cardRegistry.getCard("Daring Mechanic")!!.script.activatedAbilities[0].id

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Caradora, Heart of Alacria") {

            test("an uncrewed Vehicle you control gets the extra counter even though it isn't a creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Caradora, Heart of Alacria")
                    .withCardOnBattlefield(1, "Daring Mechanic")
                    .withCardOnBattlefield(1, "Air Response Unit") // Artifact — Vehicle, uncrewed
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val unit = game.findPermanent("Air Response Unit")!!
                withClue("the Vehicle is not a creature — Hardened Scales' default wouldn't fire") {
                    game.state.projectedState.isCreature(unit) shouldBe false
                }

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Daring Mechanic")!!,
                        abilityId = mechanicAbilityId,
                        targets = listOf(ChosenTarget.Permanent(unit))
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("one counter plus Caradora's one") {
                    plusOneCounters(game, unit) shouldBe 2
                }
            }

            test("a creature you control gets the extra counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Caradora, Heart of Alacria")
                    .withCardOnBattlefield(1, "Daring Mechanic")
                    .withCardOnBattlefield(1, "Brightfield Glider") // Creature — Possum Mount
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glider = game.findPermanent("Brightfield Glider")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Daring Mechanic")!!,
                        abilityId = mechanicAbilityId,
                        targets = listOf(ChosenTarget.Permanent(glider))
                    )
                ).error shouldBe null
                game.resolveStack()

                plusOneCounters(game, glider) shouldBe 2
            }

            test("an opponent's permanent gets no bonus — the replacement is scoped to you") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Caradora, Heart of Alacria")
                    .withCardOnBattlefield(1, "Daring Mechanic")
                    .withCardOnBattlefield(2, "Air Response Unit")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val theirUnit = game.findPermanent("Air Response Unit")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Daring Mechanic")!!,
                        abilityId = mechanicAbilityId,
                        targets = listOf(ChosenTarget.Permanent(theirUnit))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Daring Mechanic's counter lands, but Caradora doesn't add to it") {
                    plusOneCounters(game, theirUnit) shouldBe 1
                }
            }

            test("the enters tutor finds a Mount or Vehicle card and nothing else") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Caradora, Heart of Alacria")
                    .withCardInLibrary(1, "Air Response Unit") // Vehicle — a legal find
                    .withCardInLibrary(1, "Brightfield Glider") // Mount — a legal find
                    .withCardInLibrary(1, "Centaur Courser") // neither — must not be offered
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Caradora, Heart of Alacria").error shouldBe null
                game.resolveStack()

                withClue("the search pauses for a selection") {
                    game.hasPendingDecision() shouldBe true
                }
                val vehicle = game.findCardsInLibrary(1, "Air Response Unit").single()
                game.selectCards(listOf(vehicle)).error shouldBe null
                game.resolveStack()

                withClue("the chosen Vehicle card is in hand") {
                    game.isInHand(1, "Air Response Unit") shouldBe true
                }
                withClue("the ineligible creature card stays in the library") {
                    game.isInHand(1, "Centaur Courser") shouldBe false
                }
            }
        }
    }
}
