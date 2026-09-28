package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.player.SkipCombatPhasesComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class LoyalRetainersScenarioTest : ScenarioTestBase() {
    private fun activate(game: TestGame, targetId: com.wingedsheep.sdk.model.EntityId): com.wingedsheep.engine.core.ExecutionResult {
        val retainers = game.findPermanent("Loyal Retainers")!!
        val abilityId = cardRegistry.getCard("Loyal Retainers")!!.script.activatedAbilities[0].id
        return game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = retainers,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(targetId, game.player1Id, Zone.GRAVEYARD))
            )
        )
    }

    init {
        context("Loyal Retainers") {
            test("sacrifice returns a legendary creature card from your graveyard") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardOnBattlefield(1, "Loyal Retainers")
                    .withCardInGraveyard(1, "Guan Yu, Sainted Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val target = game.findCardsInGraveyard(1, "Guan Yu, Sainted Warrior").single()
                activate(game, target).error shouldBe null
                game.resolveStack()
                game.findPermanent("Guan Yu, Sainted Warrior") shouldNotBe null
                game.findPermanent("Loyal Retainers") shouldBe null
                game.isInGraveyard(1, "Loyal Retainers") shouldBe true
            }

            test("cannot target a nonlegendary creature card") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardOnBattlefield(1, "Loyal Retainers")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val target = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                activate(game, target).error shouldNotBe null
            }

            test("cannot be activated once attackers are declared") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardOnBattlefield(1, "Loyal Retainers")
                    .withCardInGraveyard(1, "Guan Yu, Sainted Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val target = game.findCardsInGraveyard(1, "Guan Yu, Sainted Warrior").single()
                activate(game, target).error shouldNotBe null
            }

            test("cannot be activated on the opponent's turn") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardOnBattlefield(1, "Loyal Retainers")
                    .withCardInGraveyard(1, "Guan Yu, Sainted Warrior")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val target = game.findCardsInGraveyard(1, "Guan Yu, Sainted Warrior").single()
                activate(game, target).error shouldNotBe null
            }
        }
    }
}
