package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kiora, the Crashing Wave (BNG #149, {2}{G}{U}, Loyalty 2).
 *
 *   +1: Until your next turn, prevent all damage that would be dealt to and dealt by target
 *       permanent an opponent controls.
 *   −1: Draw a card. You may play an additional land this turn.
 *   −5: You get an emblem with "At the beginning of your end step, create a 9/9 blue Kraken
 *       creature token."
 *
 * Pins the +1's two shields (noncombat damage *to* the permanent on your turn, combat damage *by*
 * it on the opponent's turn — the "until your next turn" span), the −1's extra land drop on top of
 * one already used, and the −5 emblem outliving Kiora.
 */
class KioraTheCrashingWaveScenarioTest : ScenarioTestBase() {

    init {
        test("+1 prevents damage dealt to the target, and damage it deals during the opponent's turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kiora, the Crashing Wave")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kiora = game.findPermanent("Kiora, the Crashing Wave")!!
            setLoyalty(game, kiora, 2)
            val bears = game.findPermanent("Grizzly Bears")!!

            activate(game, kiora, index = 0, targets = listOf(ChosenTarget.Permanent(bears)))
            game.resolveStack()
            loyalty(game, kiora) shouldBe 3

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()
            withClue("Lightning Bolt's 3 damage to the shielded Grizzly Bears is prevented") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                (game.state.getEntity(bears)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            }

            // Into the opponent's turn: the shield lasts until Kiora's controller's next turn.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the unblocked Grizzly Bears deal no combat damage") {
                game.getLifeTotal(1) shouldBe 20
            }
        }

        test("−1 draws a card and grants a land drop beyond the one already used") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kiora, the Crashing Wave")
                .withCardsInHand(1, "Forest", 2)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kiora = game.findPermanent("Kiora, the Crashing Wave")!!
            setLoyalty(game, kiora, 2)

            playLand(game, game.findCardsInHand(1, "Forest").first()).error shouldBe null

            activate(game, kiora, index = 1, targets = emptyList())
            game.resolveStack()

            loyalty(game, kiora) shouldBe 1
            withClue("the Island was drawn") { game.isInHand(1, "Island") shouldBe true }
            withClue("the second land drop is legal") {
                playLand(game, game.findCardsInHand(1, "Forest").single()).error shouldBe null
            }
            withClue("a third land drop is not") {
                playLand(game, game.findCardsInHand(1, "Island").single()).error shouldNotBe null
            }
        }

        test("−5 emblem creates a 9/9 Kraken at your end step even after Kiora is gone") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Kiora, the Crashing Wave")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kiora = game.findPermanent("Kiora, the Crashing Wave")!!
            setLoyalty(game, kiora, 5)

            activate(game, kiora, index = 2, targets = emptyList())
            game.resolveStack()

            withClue("Kiora died at 0 loyalty; the emblem is global") {
                game.findPermanent("Kiora, the Crashing Wave") shouldBe null
                game.state.globalGrantedTriggeredAbilities.size shouldBe 1
            }
            game.findPermanents("Kraken Token").size shouldBe 0

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            val krakens = game.findPermanents("Kraken Token")
            krakens.size shouldBe 1
            val projected = game.state.projectedState
            projected.getPower(krakens.single()) shouldBe 9
            projected.getToughness(krakens.single()) shouldBe 9
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget>) {
        val ability = cardRegistry.getCard("Kiora, the Crashing Wave")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id, targets = targets)
        ).error shouldBe null
    }

    private fun playLand(game: TestGame, card: EntityId) = game.execute(PlayLand(game.player1Id, card))

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
