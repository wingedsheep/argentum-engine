package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Xenagos, the Reveler — the +1 resolves off the stack, counts creatures then, and lets the
 * controller split the mana between {R} and {G}; the −6 exiles the top seven and puts only the
 * chosen creature and/or land cards onto the battlefield, leaving the rest in exile.
 */
class XenagosTheRevelerScenarioTest : ScenarioTestBase() {

    private val xenagos get() = cardRegistry.getCard("Xenagos, the Reveler")!!

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { container ->
            container
                .with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
                .without<AbilityActivatedThisTurnComponent>()
        }
    }

    private fun cardNamed(game: TestGame, zone: Zone, name: String): EntityId =
        game.state.getZone(game.player1Id, zone)
            .first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("+1 adds one mana per creature you control, split between red and green") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Xenagos, the Reveler")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val xenagosId = game.findPermanent("Xenagos, the Reveler")!!
            seedLoyalty(game, xenagosId, 3)

            game.execute(ActivateAbility(game.player1Id, xenagosId, xenagos.script.activatedAbilities[0].id))
                .error shouldBe null
            withClue("a loyalty ability uses the stack — no mana before it resolves") {
                game.state.stack.size shouldBe 1
            }
            game.resolveStack()

            val split = game.getPendingDecision()
            withClue("the controller chooses the {R}/{G} split") { (split is ChooseNumberDecision) shouldBe true }
            game.chooseNumber(1).error shouldBe null

            val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
            withClue("X counts only your two creatures, not the opponent's") {
                pool.red shouldBe 1
                pool.green shouldBe 1
            }
        }

        test("-6 puts only the chosen creature and land cards onto the battlefield; the rest stay exiled") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Xenagos, the Reveler")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val xenagosId = game.findPermanent("Xenagos, the Reveler")!!
            seedLoyalty(game, xenagosId, 6)
            val bears = cardNamed(game, Zone.LIBRARY, "Grizzly Bears")
            val forest = cardNamed(game, Zone.LIBRARY, "Forest")
            val bolt = cardNamed(game, Zone.LIBRARY, "Lightning Bolt")
            val giant = cardNamed(game, Zone.LIBRARY, "Hill Giant")

            game.execute(ActivateAbility(game.player1Id, xenagosId, xenagos.script.activatedAbilities[2].id))
                .error shouldBe null
            game.resolveStack()

            val choice = game.getPendingDecision() as SelectCardsDecision
            withClue("only creature and land cards are eligible") {
                choice.options shouldContainExactlyInAnyOrder listOf(bears, forest, giant)
            }
            game.selectCards(listOf(bears, forest)).error shouldBe null
            game.resolveStack()

            val battlefield = game.state.getBattlefield()
            withClue("the chosen creature and land enter the battlefield") {
                (bears in battlefield) shouldBe true
                (forest in battlefield) shouldBe true
            }
            withClue("the unchosen creature and the instant stay in exile") {
                val exile = game.state.getZone(game.player1Id, Zone.EXILE)
                (giant in exile) shouldBe true
                (bolt in exile) shouldBe true
            }
        }
    }
}
