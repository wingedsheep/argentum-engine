package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Roaring Earth (NEO #204) — the first channel ability with an {X} in its cost. Proves the X
 * announced while activating from hand reaches the resolving effect as the counter count, and that
 * the land animation is permanent: a green 0/0 Spirit creature with haste that is still a land.
 */
class RoaringEarthScenarioTest : ScenarioTestBase() {

    private fun channelAbilityId() = cardRegistry.getCard("Roaring Earth")!!
        .activatedAbilities.first { it.activateFromZone == Zone.HAND }.id

    init {
        context("Roaring Earth") {

            test("Channel with X = 2 makes a land a permanent 2/2 green Spirit with haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Roaring Earth")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(1, "Wastes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val land = game.findPermanent("Wastes")!!
                val handCard = game.findCardsInHand(1, "Roaring Earth").first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = handCard,
                        abilityId = channelAbilityId(),
                        targets = listOf(ChosenTarget.Permanent(land)),
                        xValue = 2,
                    )
                )
                withClue("{X}{G}{G} with X = 2 is payable from four Forests: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("X = 2 costs four mana") {
                    (game.findAllPermanents("Forest") + land).count {
                        game.state.getEntity(it)?.has<TappedComponent>() == true
                    } shouldBe 4
                }
                withClue("Roaring Earth was discarded as a cost") {
                    game.isInGraveyard(1, "Roaring Earth") shouldBe true
                }
                game.resolveStack()

                val counters = game.state.getEntity(land)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                withClue("X = 2 puts two +1/+1 counters on the land") { counters shouldBe 2 }

                val projected = game.state.projectedState
                withClue("it's a creature and still a land") {
                    projected.isCreature(land) shouldBe true
                    projected.hasType(land, "LAND") shouldBe true
                }
                withClue("a 0/0 base plus two counters is a 2/2") {
                    projected.getPower(land) shouldBe 2
                    projected.getToughness(land) shouldBe 2
                }
                withClue("a green Spirit with haste") {
                    projected.hasSubtype(land, "Spirit") shouldBe true
                    projected.hasColor(land, Color.GREEN) shouldBe true
                    projected.hasKeyword(land, Keyword.HASTE) shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("no duration: still a creature after the turn ends") {
                    game.state.projectedState.isCreature(land) shouldBe true
                }
            }
        }
    }
}
