package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.PlayerHexproofFromComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m19.cards.DetectionTower
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ProtectionScope
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Detection Tower (M19 #249).
 *
 *   {T}: Add {C}.
 *   {1}, {T}: Until end of turn, your opponents and creatures your opponents control with hexproof
 *   can be the targets of spells and abilities you control as though they didn't have hexproof.
 *
 * The second ability grants `SuppressHexproofForGroup` to the activating player for the turn, so
 * these cover both halves (players via Leyline of Sanctity, creatures via Geist of Saint Traft),
 * "hexproof from" (ruling), that the effect outlives the land (CR 611.2a), and
 * that it ends with the turn.
 */
class DetectionTowerScenarioTest : ScenarioTestBase() {

    private val suppressAbility = DetectionTower.activatedAbilities[1].id

    private fun shockTargets(game: TestGame): List<EntityId> =
        game.getLegalActions(1)
            .filter { it.description.contains("Shock") }
            .flatMap { it.validTargets.orEmpty() }

    private fun activateTower(game: TestGame) {
        val tower = game.findPermanent("Detection Tower")!!
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = tower, abilityId = suppressAbility)
        ).error shouldBe null
    }

    private fun towerGame(vararg opponentCards: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Detection Tower")
        .apply { opponentCards.forEach { withCardOnBattlefield(2, it) } }
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInHand(1, "Shock")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("before activation the hexproof opponent and creature can't be targeted") {
            val game = towerGame("Leyline of Sanctity", "Geist of Saint Traft")
            val geist = game.findPermanent("Geist of Saint Traft")!!

            val targets = shockTargets(game)
            withClue("opponent not offered") { targets.contains(game.player2Id) shouldBe false }
            withClue("Geist not offered") { targets.contains(geist) shouldBe false }
        }

        test("after activation you can target the hexproof opponent") {
            val game = towerGame("Leyline of Sanctity")
            activateTower(game)
            game.resolveStack()

            withClue("Shock offers the hexproof opponent") { shockTargets(game).contains(game.player2Id) shouldBe true }
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
        }

        test("after activation you can target a creature with hexproof an opponent controls") {
            val game = towerGame("Geist of Saint Traft")
            val geist = game.findPermanent("Geist of Saint Traft")!!
            activateTower(game)
            game.resolveStack()

            withClue("Shock offers Geist") { shockTargets(game).contains(geist) shouldBe true }
            game.castSpell(1, "Shock", geist).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Geist of Saint Traft") shouldBe true
        }

        test("it reaches hexproof from a quality too") {
            val game = towerGame()
            game.state = game.state.updateEntity(game.player2Id) { c ->
                c.with(PlayerHexproofFromComponent(listOf(PlayerHexproofFromComponent.Grant(ProtectionScope.Color(Color.RED)))))
            }
            withClue("hexproof from red stops Shock") { shockTargets(game).contains(game.player2Id) shouldBe false }
            activateTower(game)
            game.resolveStack()
            shockTargets(game).contains(game.player2Id) shouldBe true
        }

        test("the effect lasts the turn even if Detection Tower is destroyed in response") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Detection Tower")
                .withCardOnBattlefield(2, "Leyline of Sanctity")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(2, "Forest", 3)
                .withCardInHand(1, "Shock")
                .withCardInHand(2, "Beast Within")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tower = game.findPermanent("Detection Tower")!!

            activateTower(game)
            game.passPriority().error shouldBe null
            game.castSpell(2, "Beast Within", tower).error shouldBe null
            game.resolveStack()

            withClue("Beast Within destroyed the Tower") { game.isInGraveyard(1, "Detection Tower") shouldBe true }
            withClue("the opponent is still targetable") { shockTargets(game).contains(game.player2Id) shouldBe true }
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
        }

        test("the effect ends at end of turn") {
            val game = towerGame("Leyline of Sanctity")
            activateTower(game)
            game.resolveStack()
            shockTargets(game).contains(game.player2Id) shouldBe true

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("now the opponent's upkeep") { game.state.activePlayerId shouldBe game.player2Id }
            game.passPriority().error shouldBe null
            withClue("player 1 has priority in the opponent's upkeep") { game.state.priorityPlayerId shouldBe game.player1Id }
            withClue("hexproof applies again") { shockTargets(game).contains(game.player2Id) shouldBe false }
        }

        test("an opponent's activation doesn't let you target them") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Detection Tower")
                .withCardOnBattlefield(2, "Leyline of Sanctity")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Shock")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tower = game.findPermanent("Detection Tower")!!
            game.execute(
                ActivateAbility(playerId = game.player2Id, sourceId = tower, abilityId = suppressAbility)
            ).error shouldBe null
            game.resolveStack()
            game.passPriority().error shouldBe null

            withClue("player 1 has priority") { game.state.priorityPlayerId shouldBe game.player1Id }
            withClue("the opponent's grant frees only their own spells") {
                shockTargets(game).contains(game.player2Id) shouldBe false
            }
        }
    }
}
