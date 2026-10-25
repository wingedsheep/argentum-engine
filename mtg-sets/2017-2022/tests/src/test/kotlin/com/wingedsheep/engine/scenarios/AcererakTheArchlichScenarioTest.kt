package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.state.components.player.DungeonProgressComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Acererak the Archlich (AFR #87).
 *
 * The enters trigger is gated on *Tomb of Annihilation specifically* — completing another dungeon
 * doesn't stop the bounce-and-venture. The attack trigger asks each opponent to sacrifice a creature
 * or hand Acererak's controller a 2/2 Zombie.
 */
class AcererakTheArchlichScenarioTest : ScenarioTestBase() {

    init {
        fun builder(completed: List<String>) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Acererak the Archlich")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
            .also { game ->
                if (completed.isNotEmpty()) {
                    game.state = game.state.updateEntity(game.player1Id) {
                        it.with(DungeonProgressComponent(completed = completed))
                    }
                }
            }

        test("entering without Tomb of Annihilation completed returns Acererak and ventures") {
            val game = builder(completed = listOf("Lost Mine of Phandelver"))

            game.castSpell(1, "Acererak the Archlich").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Tomb of Annihilation"))).error shouldBe null
            game.resolveStack()

            withClue("a different completed dungeon doesn't satisfy the condition: Acererak bounces") {
                game.isOnBattlefield("Acererak the Archlich") shouldBe false
                game.isInHand(1, "Acererak the Archlich") shouldBe true
            }
            DungeonService.marker(game.state, game.player1Id)!!.dungeon shouldBe "Tomb of Annihilation"
        }

        test("with Tomb of Annihilation completed the enters ability doesn't trigger") {
            val game = builder(completed = listOf("Tomb of Annihilation"))

            game.castSpell(1, "Acererak the Archlich").error shouldBe null
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.isOnBattlefield("Acererak the Archlich") shouldBe true
            DungeonService.marker(game.state, game.player1Id) shouldBe null
        }

        test("attacking: a sacrifice spares the token, declining gives you a Zombie") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Acererak the Archlich")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .apply { repeat(4) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Swamp") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Acererak the Archlich" to 2)).error shouldBe null
            game.resolveStack()

            withClue("the opponent, not you, chooses whether and what to sacrifice") {
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.playerId shouldBe game.player2Id
                game.selectCards(game.findPermanents("Grizzly Bears"))
            }
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.findAllPermanents("Zombie Token") shouldHaveSize 0
        }

        test("attacking: declining creates a Zombie under your control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Acererak the Archlich")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .apply { repeat(4) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Swamp") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Acererak the Archlich" to 2)).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.playerId shouldBe game.player2Id
            game.skipSelection()
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            val zombies = game.findAllPermanents("Zombie Token")
            zombies shouldHaveSize 1
            withClue("the token is yours, not the opponent's") {
                game.state.projectedState.getController(zombies.single()) shouldBe game.player1Id
                game.state.projectedState.getPower(zombies.single()) shouldBe 2
            }
        }
    }
}
