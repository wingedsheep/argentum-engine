package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Security Rhox (SNC #220) — {2}{R}{G} Creature — Rhino Warrior 5/4.
 *
 *   You may pay {R}{G} rather than pay this spell's mana cost. Spend only mana produced by
 *   Treasures to cast it this way.
 */
class SecurityRhoxScenarioTest : ScenarioTestBase() {

    private fun altCastOf(game: TestGame, spell: com.wingedsheep.sdk.model.EntityId) =
        game.getLegalActions(1).firstOrNull {
            val a = it.action
            a is CastSpell && a.cardId == spell && a.alternativeCostType == AlternativeCostType.SELF_ALTERNATIVE
        }

    init {
        test("a Treasure tapped live plus a cracked Treasure cast it for {R}{G}") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Security Rhox")
                // Glittering Stockpile is a Treasure whose {T}: Add {R} doesn't sacrifice it, so
                // auto-pay may tap it; the plain Treasure is cracked for {G} first.
                .withCardOnBattlefield(1, "Glittering Stockpile")
                .withCardOnBattlefield(1, "Treasure")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val rhox = game.findCardsInHand(1, "Security Rhox").single()
            altCastOf(game, rhox).shouldNotBeNull()

            val treasure = game.findPermanent("Treasure").shouldNotBeNull()
            val crack = cardRegistry.requireCard("Treasure").script.activatedAbilities.single().id
            game.execute(ActivateAbility(game.player1Id, treasure, crack, manaColorChoice = Color.GREEN)).error shouldBe null

            game.execute(CastSpell(game.player1Id, rhox, useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Security Rhox") shouldBe true
            game.findPermanent("Treasure") shouldBe null
        }

        test("with only lands it costs the full {2}{R}{G}") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Security Rhox")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val rhox = game.findCardsInHand(1, "Security Rhox").single()

            altCastOf(game, rhox) shouldBe null
            game.execute(CastSpell(game.player1Id, rhox, useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE, paymentStrategy = PaymentStrategy.AutoPay))
                .error.shouldNotBeNull()

            game.castSpell(1, "Security Rhox").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Security Rhox") shouldBe true
        }
    }
}
