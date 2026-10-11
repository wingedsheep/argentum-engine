package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh2.cards.ChatterfangSquirrelGeneral
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Chatterfang, Squirrel General (MH2 #151) — "those tokens plus that many 1/1 green Squirrel
 * creature tokens", and "{B}, Sacrifice X Squirrels: Target creature gets +X/-X".
 */
class ChatterfangSquirrelGeneralScenarioTest : ScenarioTestBase() {

    private val twoSoldiers = card("Test Two Soldiers") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Soldier"), count = 2) }
    }

    private val oneTreasure = card("Test One Treasure") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateTreasure(1) }
    }

    init {
        cardRegistry.register(twoSoldiers)
        cardRegistry.register(oneTreasure)
        val pumpId = ChatterfangSquirrelGeneral.activatedAbilities.single().id

        fun board(spell: String, chatterfangPlayer: Int = 1) = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(chatterfangPlayer, "Chatterfang, Squirrel General")
            .withCardInHand(1, spell)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("two tokens bring two Squirrels") {
            val game = board("Test Two Soldiers")
            game.castSpell(1, "Test Two Soldiers")
            game.resolveStack()
            game.findPermanents("Soldier Token").size shouldBe 2
            game.findPermanents("Squirrel").size shouldBe 2
            val squirrel = game.findPermanents("Squirrel").first()
            game.state.projectedState.getPower(squirrel) shouldBe 1
            game.state.projectedState.getToughness(squirrel) shouldBe 1
        }

        test("any kind of token counts — a Treasure brings a Squirrel") {
            val game = board("Test One Treasure")
            game.castSpell(1, "Test One Treasure")
            game.resolveStack()
            game.findPermanents("Treasure").size shouldBe 1
            game.findPermanents("Squirrel").size shouldBe 1
        }

        test("an opponent's Chatterfang does nothing for your tokens") {
            val game = board("Test Two Soldiers", chatterfangPlayer = 2)
            game.castSpell(1, "Test Two Soldiers")
            game.resolveStack()
            game.findPermanents("Squirrel").size shouldBe 0
        }

        test("sacrificing two Squirrels gives +2/-2, killing a 2/2") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Chatterfang, Squirrel General")
                .withCardOnBattlefield(1, "Squirrel", isToken = true)
                .withCardOnBattlefield(1, "Squirrel", isToken = true)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val chatterfang = game.findPermanent("Chatterfang, Squirrel General")!!
            val result = game.execute(
                ActivateAbility(
                    game.player1Id, chatterfang, pumpId,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!)),
                    costPayment = AdditionalCostPayment(variableCostPermanents = game.findPermanents("Squirrel"))
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.findPermanents("Squirrel").size shouldBe 0
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("X can be 0 — sacrificing nothing is a legal +0/-0") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Chatterfang, Squirrel General")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val chatterfang = game.findPermanent("Chatterfang, Squirrel General")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val result = game.execute(
                ActivateAbility(
                    game.player1Id, chatterfang, pumpId,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    costPayment = AdditionalCostPayment(variableCostPermanents = emptyList())
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()
            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.getToughness(bears) shouldBe 2
        }
    }
}
