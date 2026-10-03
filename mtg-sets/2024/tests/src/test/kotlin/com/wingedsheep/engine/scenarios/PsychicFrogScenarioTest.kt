package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Psychic Frog (MH3 #199).
 *
 * Whenever this creature deals combat damage to a player or planeswalker, draw a card.
 * Discard a card: Put a +1/+1 counter on this creature.
 * Exile three cards from your graveyard: This creature gains flying until end of turn.
 */
class PsychicFrogScenarioTest : ScenarioTestBase() {

    private val frogDef = cardRegistry.getCard("Psychic Frog")!!
    private val discardAbilityId = frogDef.activatedAbilities[0].id
    private val exileAbilityId = frogDef.activatedAbilities[1].id

    init {
        context("Psychic Frog") {

            test("combat damage to a player draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Psychic Frog", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Psychic Frog" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.getLifeTotal(2) shouldBe 19
                withClue("Frog's combat damage trigger draws a card") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("discard a card puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Psychic Frog")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val frog = game.findPermanent("Psychic Frog")!!
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = frog,
                        abilityId = discardAbilityId,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(bears)),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.state.getEntity(frog)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.state.projectedState.getPower(frog) shouldBe 2
                game.state.projectedState.getToughness(frog) shouldBe 3
            }

            test("exiling three graveyard cards grants flying until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Psychic Frog")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val frog = game.findPermanent("Psychic Frog")!!
                game.state.projectedState.hasKeyword(frog, Keyword.FLYING) shouldBe false
                val graveyard = game.state.getGraveyard(game.player1Id)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = frog,
                        abilityId = exileAbilityId,
                        costPayment = AdditionalCostPayment(exiledCards = graveyard),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.graveyardSize(1) shouldBe 0
                game.state.projectedState.hasKeyword(frog, Keyword.FLYING) shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("flying wears off at end of turn") {
                    game.state.projectedState.hasKeyword(frog, Keyword.FLYING) shouldBe false
                }
            }

            test("exile ability is unaffordable with only two graveyard cards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Psychic Frog")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val frog = game.findPermanent("Psychic Frog")!!
                val action = game.getLegalActions(1).firstOrNull { la ->
                    val a = la.action
                    a is ActivateAbility && a.sourceId == frog && a.abilityId == exileAbilityId
                }
                (action?.isAffordable ?: false) shouldBe false
            }
        }
    }
}
