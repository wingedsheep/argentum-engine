package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Valkyrie Harbinger (KHM #374, reprinted in J22 #261).
 *
 * "{4}{W}{W} Creature — Angel Cleric 4/5
 *  Flying, lifelink
 *  At the beginning of each end step, if you gained 4 or more life this turn, create a 4/4 white
 *  Angel creature token with flying and vigilance."
 *
 * The token trigger is an intervening-"if": exactly 4 life gained fires it, 3 does not.
 */
class ValkyrieHarbingerScenarioTest : ScenarioTestBase() {

    private val gainFourLife = card("Test Valkyrie Gain Four") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(4) }
    }
    private val gainThreeLife = card("Test Valkyrie Gain Three") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(3) }
    }

    init {
        cardRegistry.register(gainFourLife)
        cardRegistry.register(gainThreeLife)

        context("Valkyrie Harbinger") {

            test("is a 4/5 with flying and lifelink") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Valkyrie Harbinger", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val valkyrie = game.findPermanent("Valkyrie Harbinger")!!
                game.state.projectedState.getPower(valkyrie) shouldBe 4
                game.state.projectedState.getToughness(valkyrie) shouldBe 5
                game.state.projectedState.hasKeyword(valkyrie, Keyword.FLYING) shouldBe true
                game.state.projectedState.hasKeyword(valkyrie, Keyword.LIFELINK) shouldBe true
            }

            test("gaining exactly 4 life this turn creates one 4/4 flying, vigilance Angel token at the end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Valkyrie Harbinger", summoningSickness = false)
                    .withCardInHand(1, "Test Valkyrie Gain Four")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = angelTokens(game.state, game.player1Id)

                val cast = game.castSpell(1, "Test Valkyrie Gain Four")
                withClue("Casting the life-gain spell should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val newTokens = angelTokens(game.state, game.player1Id) - before
                withClue("Gaining 4 life should create exactly one Angel token") {
                    newTokens.size shouldBe 1
                }
                val token = newTokens.first()
                val tokenCard = game.state.getEntity(token)!!.get<CardComponent>()!!
                withClue("Token is a 4/4 Angel with flying and vigilance") {
                    game.state.projectedState.getPower(token) shouldBe 4
                    game.state.projectedState.getToughness(token) shouldBe 4
                    tokenCard.typeLine.subtypes.map { it.value } shouldBe listOf("Angel")
                    game.state.projectedState.hasKeyword(token, Keyword.FLYING) shouldBe true
                    game.state.projectedState.hasKeyword(token, Keyword.VIGILANCE) shouldBe true
                }
            }

            test("gaining only 3 life this turn creates no token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Valkyrie Harbinger", summoningSickness = false)
                    .withCardInHand(1, "Test Valkyrie Gain Three")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = angelTokens(game.state, game.player1Id)

                game.castSpell(1, "Test Valkyrie Gain Three")
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Gaining only 3 life should not create a token") {
                    (angelTokens(game.state, game.player1Id) - before).size shouldBe 0
                }
            }
        }
    }
}

private fun angelTokens(state: GameState, player: EntityId): Set<EntityId> =
    state.getBattlefield().filter {
        val e = state.getEntity(it) ?: return@filter false
        e.has<TokenComponent>() &&
            e.get<ControllerComponent>()?.playerId == player &&
            e.get<CardComponent>()?.typeLine?.isCreature == true
    }.toSet()
