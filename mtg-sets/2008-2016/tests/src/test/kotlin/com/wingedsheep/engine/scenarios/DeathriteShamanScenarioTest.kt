package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Deathrite Shaman's first ability adds mana but targets, so it is not a mana ability
 * (CR 605.1a): it must go on the stack, and a fizzled target means no mana at all. The other two
 * abilities pin the exile-then-drain / exile-then-gain halves and their graveyard type filters.
 */
class DeathriteShamanScenarioTest : ScenarioTestBase() {
    init {
        fun TestGame.pool(playerId: EntityId): ManaPoolComponent =
            state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

        val abilities = cardRegistry.getCard("Deathrite Shaman")!!.activatedAbilities
        val landAbility = abilities[0].id
        val drainAbility = abilities[1].id
        val gainAbility = abilities[2].id

        test("the land ability uses the stack, exiles the land card and adds one mana of the chosen color") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Deathrite Shaman")
                .withCardInGraveyard(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val land = game.state.getGraveyard(game.player2Id).single()

            game.execute(ActivateAbility(
                playerId = game.player1Id,
                sourceId = game.findPermanent("Deathrite Shaman")!!,
                abilityId = landAbility,
                targets = listOf(ChosenTarget.Card(land, game.player2Id, Zone.GRAVEYARD))
            )).error shouldBe null
            withClue("a targeted ability is not a mana ability, so it waits on the stack") {
                game.state.stack.isEmpty() shouldBe false
                game.pool(game.player1Id).total shouldBe 0
            }

            game.resolveStack()
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseColorDecision>()
            game.submitDecision(ColorChosenResponse(decision.id, Color.BLUE)).error shouldBe null

            game.isInExile(2, "Forest") shouldBe true
            game.pool(game.player1Id).blue shouldBe 1
            game.pool(game.player1Id).total shouldBe 1
        }

        test("the land ability adds no mana when its target leaves the graveyard in response") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Deathrite Shaman")
                .withCardOnBattlefield(1, "Tormod's Crypt")
                .withCardInGraveyard(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val land = game.state.getGraveyard(game.player2Id).single()

            game.execute(ActivateAbility(
                playerId = game.player1Id,
                sourceId = game.findPermanent("Deathrite Shaman")!!,
                abilityId = landAbility,
                targets = listOf(ChosenTarget.Card(land, game.player2Id, Zone.GRAVEYARD))
            )).error shouldBe null
            game.execute(ActivateAbility(
                playerId = game.player1Id,
                sourceId = game.findPermanent("Tormod's Crypt")!!,
                abilityId = cardRegistry.getCard("Tormod's Crypt")!!.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Player(game.player2Id))
            )).error shouldBe null
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.pool(game.player1Id).total shouldBe 0
        }

        test("{B}, {T} exiles an instant card and each opponent loses 2 life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Deathrite Shaman")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInGraveyard(2, "Giant Growth")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val instant = game.state.getGraveyard(game.player2Id).single()

            game.execute(ActivateAbility(
                playerId = game.player1Id,
                sourceId = game.findPermanent("Deathrite Shaman")!!,
                abilityId = drainAbility,
                targets = listOf(ChosenTarget.Card(instant, game.player2Id, Zone.GRAVEYARD))
            )).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Giant Growth") shouldBe true
            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 20
        }

        test("{G}, {T} exiles a creature card and you gain 2 life, but can't target a land card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Deathrite Shaman")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val shaman = game.findPermanent("Deathrite Shaman")!!
            val bears = game.state.getGraveyard(game.player1Id).single()
            val land = game.state.getGraveyard(game.player2Id).single()

            game.execute(ActivateAbility(
                playerId = game.player1Id,
                sourceId = shaman,
                abilityId = gainAbility,
                targets = listOf(ChosenTarget.Card(land, game.player2Id, Zone.GRAVEYARD))
            )).error shouldNotBe null

            game.execute(ActivateAbility(
                playerId = game.player1Id,
                sourceId = shaman,
                abilityId = gainAbility,
                targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD))
            )).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.getLifeTotal(1) shouldBe 22
        }
    }
}
