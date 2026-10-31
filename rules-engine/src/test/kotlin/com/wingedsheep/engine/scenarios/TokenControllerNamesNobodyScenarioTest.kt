package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A token effect whose `controller` is a player reference that resolves to nobody creates no
 * token — it never falls back to the source's controller. The case that matters is
 * `EffectTarget.TargetController` after an "up to one target" chose none: Cavalier of Dawn's
 * "destroy up to one target nonland permanent. Its controller creates a 3/3 Golem" makes no
 * Golem when no target was chosen (2025-02-07 ruling). With a target, "its controller" still
 * gets the token. Covers the three token paths: `CreateToken`, predefined tokens, `Investigate`.
 */
class TokenControllerNamesNobodyScenarioTest : ScenarioTestBase() {

    private fun upToOne(name: String, rider: com.wingedsheep.sdk.scripting.effects.Effect) = card(name) {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy up to one target creature. Its controller creates a token."
        spell {
            val t = target(TargetFilter.Creature, optional = true)
            effect = Effects.Destroy(t) then rider
        }
    }

    private val golemRider = upToOne(
        "Test Golem Gift",
        Effects.CreateToken(
            power = 3, toughness = 3, creatureTypes = setOf("Golem"), artifactToken = true,
            controller = EffectTarget.TargetController
        )
    )
    private val mapRider = upToOne("Test Map Gift", CreatePredefinedTokenEffect("Map", 1, EffectTarget.TargetController))
    private val clueRider = upToOne("Test Clue Gift", Effects.Investigate(controller = EffectTarget.TargetController))

    private fun tokensOf(game: TestGame, playerNumber: Int, name: String): Int {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getBattlefield(playerId).count { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    private fun board(spell: String) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, spell)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        listOf(golemRider, mapRider, clueRider).forEach { cardRegistry.register(it) }

        for ((spell, token) in listOf(
            "Test Golem Gift" to "Golem Token",
            "Test Map Gift" to "Map",
            "Test Clue Gift" to "Clue",
        )) {
            context(spell) {
                test("with no target chosen, nobody creates the token") {
                    val game = board(spell)
                    game.castSpell(1, spell).error shouldBe null
                    game.resolveStack()

                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    withClue("the caster gets no fallback token") { tokensOf(game, 1, token) shouldBe 0 }
                    tokensOf(game, 2, token) shouldBe 0
                }

                test("with a target, its controller creates the token") {
                    val game = board(spell)
                    val bears = game.findPermanent("Grizzly Bears")!!
                    game.castSpell(1, spell, targetId = bears).error shouldBe null
                    game.resolveStack()

                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    tokensOf(game, 2, token) shouldBe 1
                    tokensOf(game, 1, token) shouldBe 0
                }
            }
        }
    }
}
