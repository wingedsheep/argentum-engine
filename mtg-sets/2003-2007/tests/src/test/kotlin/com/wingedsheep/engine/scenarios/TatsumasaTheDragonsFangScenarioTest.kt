package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tatsumasa, the Dragon's Fang — "Equipped creature gets +5/+5. {6}, Exile Tatsumasa: Create a 5/5
 * blue Dragon Spirit creature token with flying. Return Tatsumasa to the battlefield under its
 * owner's control when that token dies. Equip {3}"
 *
 * The return is a delayed trigger watching the one token the activation made: it fires on that
 * token's death however many turns later, and not when the token leaves some other way.
 */
class TatsumasaTheDragonsFangScenarioTest : ScenarioTestBase() {

    private val dragon = "Dragon Spirit Token"

    private val exileAbility by lazy {
        cardRegistry.requireCard("Tatsumasa, the Dragon's Fang").script.activatedAbilities
            .first { it.description.startsWith("{6}, Exile") }.id
    }

    private fun armedGame(vararg removal: String): TestGame {
        val builder = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, "Tatsumasa, the Dragon's Fang")
            .withLandsOnBattlefield(1, "Swamp", 8)
            .withLandsOnBattlefield(1, "Plains", 1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        removal.forEach { builder.withCardInHand(1, it) }
        return builder.build()
    }

    private fun TestGame.activateFang() {
        val fang = findPermanent("Tatsumasa, the Dragon's Fang")!!
        execute(ActivateAbility(player1Id, fang, exileAbility)).error shouldBe null
        resolveStack()
    }

    init {
        test("equipped creature gets +5/+5") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Tatsumasa, the Dragon's Fang", "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 7
            game.state.projectedState.getToughness(bears) shouldBe 7
        }

        test("activating exiles Tatsumasa and creates a 5/5 blue flying Dragon Spirit") {
            val game = armedGame()
            game.activateFang()

            game.isInExile(1, "Tatsumasa, the Dragon's Fang") shouldBe true
            game.isOnBattlefield("Tatsumasa, the Dragon's Fang") shouldBe false
            val tokens = game.findPermanents(dragon)
            tokens shouldHaveSize 1
            val projected = game.state.projectedState
            projected.getPower(tokens[0]) shouldBe 5
            projected.getToughness(tokens[0]) shouldBe 5
            projected.hasKeyword(tokens[0], Keyword.FLYING) shouldBe true
            projected.getColors(tokens[0]) shouldBe setOf(Color.BLUE.name)
        }

        test("when the token dies, Tatsumasa returns to the battlefield unattached") {
            val game = armedGame("Terror")
            game.activateFang()

            game.castSpell(1, "Terror", game.findPermanent(dragon)!!).error shouldBe null
            game.resolveStack()

            game.findPermanents(dragon) shouldHaveSize 0
            withClue("the delayed trigger returned the Equipment") {
                game.isOnBattlefield("Tatsumasa, the Dragon's Fang") shouldBe true
                game.isInExile(1, "Tatsumasa, the Dragon's Fang") shouldBe false
            }
            val fang = game.findPermanent("Tatsumasa, the Dragon's Fang")!!
            game.state.getEntity(fang)!!.has<AttachedToComponent>() shouldBe false
            game.state.projectedState.getController(fang) shouldBe game.player1Id
        }

        test("the watch outlives the turn — the token dying turns later still returns Tatsumasa") {
            val game = armedGame("Terror")
            game.activateFang()

            // Into the opponent's turn: an end-of-turn expiry would have swept the trigger by now.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.passPriority() // the opponent passes; Tatsumasa's controller responds in their upkeep

            game.castSpell(1, "Terror", game.findPermanent(dragon)!!).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Tatsumasa, the Dragon's Fang") shouldBe true
        }

        test("the token leaving any other way does not return Tatsumasa") {
            val game = armedGame("Swords to Plowshares")
            game.activateFang()

            game.castSpell(1, "Swords to Plowshares", game.findPermanent(dragon)!!).error shouldBe null
            game.resolveStack()

            game.findPermanents(dragon) shouldHaveSize 0
            game.isOnBattlefield("Tatsumasa, the Dragon's Fang") shouldBe false
            game.isInExile(1, "Tatsumasa, the Dragon's Fang") shouldBe true
        }

        test("a Tatsumasa that left exile and came back is a new object and stays put") {
            val game = armedGame("Terror")
            game.activateFang()

            // Exile -> graveyard -> exile makes it a new object (CR 400.7); the watch named the old one.
            val fang = game.state.getExile(game.player1Id).single()
            game.state = game.zones.moveToZone(game.state, fang, Zone.GRAVEYARD).state
            val buried = game.findCardsInGraveyard(1, "Tatsumasa, the Dragon's Fang").single()
            game.state = game.zones.moveToZone(game.state, buried, Zone.EXILE).state

            game.castSpell(1, "Terror", game.findPermanent(dragon)!!).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Tatsumasa, the Dragon's Fang") shouldBe false
            game.isInExile(1, "Tatsumasa, the Dragon's Fang") shouldBe true
        }
    }
}
