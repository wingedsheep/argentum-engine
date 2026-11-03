package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m19.cards.AmuletOfSafekeeping
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Amulet of Safekeeping — the player-only becomes-target trigger (`Triggers.you.becomesTarget`):
 * an opponent's spell targeting *you* is countered unless they pay {1}; one targeting a permanent you
 * control, or your own spell targeting you, doesn't trigger it. Creature tokens get -1/-0, whoever
 * controls them.
 */
class AmuletOfSafekeepingScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(AmuletOfSafekeeping)

        fun board(opponentMountains: Int): TestGame = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Amulet of Safekeeping")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(2, "Lightning Bolt")
            .withLandsOnBattlefield(2, "Mountain", opponentMountains)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("an opponent's spell targeting you is countered when they can't pay {1}") {
            val game = board(opponentMountains = 1)
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }

        test("the opponent may pay {1} and the spell resolves") {
            val game = board(opponentMountains = 2)
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()

            val window = game.state.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
            withClue("the targeting spell's controller is asked to pay") {
                window.playerId shouldBe game.player2Id
                window.context.sourceName shouldBe "Amulet of Safekeeping"
            }
            game.submitDecision(ManaSourcesSelectedResponse(window.id, autoPay = true)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 17
        }

        test("a permanent you control being targeted doesn't trigger it") {
            val game = board(opponentMountains = 1)
            game.castSpell(2, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.findPermanent("Grizzly Bears") shouldBe null
        }

        test("your own spell targeting you doesn't trigger it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Amulet of Safekeeping")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.getLifeTotal(1) shouldBe 17
        }

        test("creature tokens get -1/-0 on both sides; nontoken creatures don't") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Amulet of Safekeeping")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant", isToken = true)
                .withCardOnBattlefield(2, "Hill Giant", isToken = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val projected = game.state.projectedState
            val giants = game.findPermanents("Hill Giant")
            giants.size shouldBe 2
            giants.forEach { giant ->
                projected.getPower(giant) shouldBe 2
                projected.getToughness(giant) shouldBe 3
            }
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
        }
    }
}
