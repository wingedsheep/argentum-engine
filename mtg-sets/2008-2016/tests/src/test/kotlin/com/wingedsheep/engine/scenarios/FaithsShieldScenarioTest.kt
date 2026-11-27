package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.mechanics.targeting.PlayerProtectionRules
import com.wingedsheep.engine.state.components.player.PlayerProtectionComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dka.cards.FaithsShield
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Faith's Shield (DKA #7) — {W} Instant.
 *
 * "Target permanent you control gains protection from the color of your choice until end of turn.
 * Fateful hour — If you have 5 or less life, instead you and each permanent you control gain
 * protection from the color of your choice until end of turn."
 *
 * Above 5 life only the target is protected; at 5 or less the controller and every permanent they
 * control are, from the single colour chosen — and the player's protection stops a red Lightning
 * Bolt (even their own) from targeting them, until end of turn.
 */
class FaithsShieldScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(FaithsShield))

        fun board(life: Int) = scenario()
            .withPlayers("Player", "Opponent")
            .withLifeTotal(1, life)
            .withCardInHand(1, "Faith's Shield")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Savannah Lions")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.castOn(name: String, color: Color) {
            castSpell(1, "Faith's Shield", findPermanent(name)!!).error shouldBe null
            if (hasPendingDecision() && getPendingDecision() !is ChooseColorDecision) submitManaSourcesAutoPay()
            resolveStack()
            val decision = getPendingDecision()
            decision.shouldBeInstanceOf<ChooseColorDecision>()
            submitDecision(ColorChosenResponse(decision.id, color))
        }

        fun TestGame.playerProtectedFrom(name: String): Boolean =
            PlayerProtectionRules.isProtectedFromSource(
                state, player1Id, findPermanent(name), player2Id, predicateEvaluator = services.predicateEvaluator
            )

        test("above 5 life only the target permanent gains protection") {
            val game = board(life = 20)
            game.castOn("Grizzly Bears", Color.RED)
            val projected = game.state.projectedState
            withClue("the target is protected from red") {
                projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, "PROTECTION_FROM_RED") shouldBe true
            }
            withClue("another permanent you control isn't") {
                projected.hasKeyword(game.findPermanent("Savannah Lions")!!, "PROTECTION_FROM_RED") shouldBe false
            }
            withClue("and you aren't") {
                game.state.getEntity(game.player1Id)?.get<PlayerProtectionComponent>() shouldBe null
            }
        }

        test("fateful hour — you and each permanent you control gain protection from the one chosen colour") {
            val game = board(life = 5)
            game.castOn("Grizzly Bears", Color.RED)
            val projected = game.state.projectedState
            withClue("every permanent you control, lands included, is protected from red") {
                projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, "PROTECTION_FROM_RED") shouldBe true
                projected.hasKeyword(game.findPermanent("Savannah Lions")!!, "PROTECTION_FROM_RED") shouldBe true
                projected.hasKeyword(game.findPermanent("Plains")!!, "PROTECTION_FROM_RED") shouldBe true
            }
            withClue("an opponent's permanent isn't") {
                projected.hasKeyword(game.findPermanent("Hill Giant")!!, "PROTECTION_FROM_RED") shouldBe false
            }
            withClue("you're protected from a red source, from that colour only") {
                game.playerProtectedFrom("Hill Giant") shouldBe true
                game.playerProtectedFrom("Plains") shouldBe false
            }
            withClue("a red Lightning Bolt can't target you, but can still target the opponent") {
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldNotBe null
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            }
        }

        test("the player's protection ends at end of turn") {
            val game = board(life = 3)
            game.castOn("Grizzly Bears", Color.RED)
            game.playerProtectedFrom("Hill Giant") shouldBe true
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.playerProtectedFrom("Hill Giant") shouldBe false
        }
    }
}
