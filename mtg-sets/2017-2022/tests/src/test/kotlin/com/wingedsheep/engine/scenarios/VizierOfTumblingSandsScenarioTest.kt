package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.akh.cards.VizierOfTumblingSands
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vizier of Tumbling Sands (AKH #75) — {2}{U} Creature — Human Cleric, 1/3.
 *
 *   {T}: Untap another target permanent.
 *   Cycling {1}{U}
 *   When you cycle this card, untap target permanent.
 */
class VizierOfTumblingSandsScenarioTest : ScenarioTestBase() {

    private val abilityId = VizierOfTumblingSands.activatedAbilities.first().id

    init {
        context("Vizier of Tumbling Sands") {

            test("{T}: untaps another target permanent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vizier of Tumbling Sands")
                    .withCardOnBattlefield(1, "Hill Giant", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val vizier = game.findPermanent("Vizier of Tumbling Sands")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = vizier,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant is untapped, the Vizier is tapped") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
                    game.state.getEntity(vizier)?.has<TappedComponent>() shouldBe true
                }
            }

            test("{T} ability can't target the Vizier itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vizier of Tumbling Sands")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val vizier = game.findPermanent("Vizier of Tumbling Sands")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = vizier,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(vizier)),
                    )
                ).error shouldNotBe null
            }

            test("cycling it draws a card and untaps target permanent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Vizier of Tumbling Sands")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Hill Giant", tapped = true)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!

                game.cycleCard(1, "Vizier of Tumbling Sands").error shouldBe null
                // The cycle trigger asks for its target as it goes on the stack.
                if (!game.hasPendingDecision()) game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("cycled: card drawn, Hill Giant untapped") {
                    game.isInGraveyard(1, "Vizier of Tumbling Sands") shouldBe true
                    game.isInHand(1, "Plains") shouldBe true
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
