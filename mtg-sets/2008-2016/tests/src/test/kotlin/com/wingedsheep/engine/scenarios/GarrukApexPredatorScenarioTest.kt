package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Garruk, Apex Predator (M15 #210, {5}{B}{G}, loyalty 5).
 *
 * - −3 gains life equal to the destroyed creature's toughness as it last existed on the
 *   battlefield, so a Giant Growth on the target counts.
 * - −8's emblem belongs to the targeted opponent (ruling): a creature attacking that player gets
 *   +5/+5 and trample until end of turn, but one attacking a planeswalker they control doesn't
 *   (ruling), and the emblem outlives Garruk.
 */
class GarrukApexPredatorScenarioTest : ScenarioTestBase() {

    private val name = "Garruk, Apex Predator"

    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("−3 destroys the creature and gains life equal to its pumped, last-known toughness") {
            val game = board()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .build()
            val garruk = game.findPermanent(name)!!
            val bears = game.findPermanent("Grizzly Bears")!!
            setLoyalty(game, garruk, 5)

            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getToughness(bears) shouldBe 5

            activate(game, garruk, index = 2, targets = listOf(ChosenTarget.Permanent(bears)))
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            withClue("2/2 Bears pumped to 5/5 before it died") { game.getLifeTotal(1) shouldBe 25 }
            loyalty(game, garruk) shouldBe 2
        }

        test("−8 emblem: a creature attacking its owner gets +5/+5 and trample; one attacking their planeswalker doesn't") {
            val game = board()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Ajani Goldmane")
                .build()
            val garruk = game.findPermanent(name)!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            setLoyalty(game, garruk, 8)
            setLoyalty(game, game.findPermanent("Ajani Goldmane")!!, 4)

            activate(game, garruk, index = 3, targets = listOf(ChosenTarget.Player(game.player2Id)))
            game.resolveStack()

            withClue("the targeted opponent owns the emblem") {
                game.state.globalGrantedTriggeredAbilities.single().controllerId shouldBe game.player2Id
            }
            withClue("Garruk spent all 8 loyalty; the emblem stays") { game.findPermanent(name) shouldBe null }

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                playerAttackers = mapOf("Grizzly Bears" to 2),
                permanentAttackers = mapOf("Hill Giant" to "Ajani Goldmane"),
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 7
            projected.getToughness(bears) shouldBe 7
            projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            withClue("attacking a planeswalker isn't attacking the emblem's owner") {
                projected.getPower(giant) shouldBe 3
                projected.getToughness(giant) shouldBe 3
                projected.hasKeyword(giant, Keyword.TRAMPLE) shouldBe false
            }
        }

        test("−8 emblem doesn't trigger when the emblem's owner attacks Garruk's controller") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, name)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val garruk = game.findPermanent(name)!!
            val bears = game.findPermanent("Grizzly Bears")!!
            setLoyalty(game, garruk, 8)

            activate(game, garruk, index = 3, targets = listOf(ChosenTarget.Player(game.player2Id)))
            game.resolveStack()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id, targets = targets)
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
