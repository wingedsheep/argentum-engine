package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gideon of the Trials (AKH #14, {1}{W}{W}, loyalty 3).
 *
 *   0: You get an emblem with "As long as you control a Gideon planeswalker, you can't lose the
 *      game and your opponents can't win the game."
 *
 * The emblem lives outside every zone, so these pin that the loss and win checks read it, and that
 * its "you control a Gideon planeswalker" gate is re-evaluated on every check: at 0 life you keep
 * playing while Gideon is around and lose the moment he's gone, and an opponent's "you win the game"
 * does nothing while the gate holds.
 */
class GideonOfTheTrialsScenarioTest : ScenarioTestBase() {

    init {
        test("at 0 life you don't lose while you control a Gideon, and lose once he leaves") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gideon of the Trials")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val gideon = game.findPermanent("Gideon of the Trials")!!
            setLoyalty(game, gideon, 3)
            getEmblem(game, gideon)

            game.state = game.state.updateEntity(game.player1Id) { it.with(LifeTotalComponent(0)) }
            // Casting a spell runs state-based actions; the emblem keeps the 0-life player in.
            game.castSpell(1, "Lightning Bolt", gideon).error shouldBe null
            withClue("still in the game at 0 life while Gideon is on the battlefield") {
                game.state.gameOver shouldBe false
            }

            // Bolt takes Gideon's last 3 loyalty: he dies, the gate closes, and the 0-life player loses.
            game.resolveStack()
            withClue("Gideon gone -> the emblem stops protecting -> the player loses") {
                game.isInGraveyard(1, "Gideon of the Trials") shouldBe true
                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe game.player2Id
            }
        }

        test("an opponent's win-the-game effect does nothing while you control a Gideon") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gideon of the Trials")
                .withCardOnBattlefield(2, "Felidar Sovereign")
                .withLifeTotal(2, 45)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val gideon = game.findPermanent("Gideon of the Trials")!!
            setLoyalty(game, gideon, 3)
            getEmblem(game, gideon)

            // The opponent's upkeep: Felidar Sovereign's "you win the game" resolves and fizzles.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
            withClue("Felidar Sovereign's trigger resolved without ending the game") {
                game.state.activePlayerId shouldBe game.player2Id
                game.state.gameOver shouldBe false
            }
        }

        test("without the emblem, the opponent's Felidar Sovereign wins") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gideon of the Trials")
                .withCardOnBattlefield(2, "Felidar Sovereign")
                .withLifeTotal(2, 45)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            game.state.gameOver shouldBe true
            game.state.winnerId shouldBe game.player2Id
        }

        test("without the emblem, 0 life loses as usual") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gideon of the Trials")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val gideon = game.findPermanent("Gideon of the Trials")!!
            setLoyalty(game, gideon, 3)
            game.state = game.state.updateEntity(game.player1Id) { it.with(LifeTotalComponent(0)) }
            // The creature 0 (not the emblem) — any action runs state-based actions.
            activate(game, gideon, index = 1)
            game.state.gameOver shouldBe true
        }
    }

    private fun getEmblem(game: TestGame, gideon: EntityId) {
        activate(game, gideon, index = 2)
        game.resolveStack()
    }

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        val ability = cardRegistry.getCard("Gideon of the Trials")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id)
        ).error shouldBe null
    }

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
