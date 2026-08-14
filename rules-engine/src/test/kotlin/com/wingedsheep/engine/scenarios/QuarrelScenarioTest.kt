package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Quarrel (HOB) — {1}{G} Instant.
 * "Target creature you control deals damage equal to its power to target creature an opponent
 *  controls."
 *
 * Not a fight: the damage is one-directional, so your creature must take none back. The amount is
 * read off the *first* target's power, which is checked by pumping it first.
 */
class QuarrelScenarioTest : ScenarioTestBase() {

    private fun damageOn(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
        game.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    init {
        cardRegistry.register(
            CardDefinition.creature(
                "Test Cave Troll", ManaCost.parse("{5}{G}"), emptySet(), power = 6, toughness = 7
            )
        )

        context("Quarrel") {

            test("your creature deals its power to theirs, and takes nothing back") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Quarrel")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Test Cave Troll")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mine = game.findPermanent("Centaur Courser")!!
                val theirs = game.findPermanent("Test Cave Troll")!!
                val spell = game.state.getHand(game.player1Id).single { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Quarrel"
                }

                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("3 power dealt 3 damage to the opponent's creature") {
                    damageOn(game, theirs) shouldBe 3
                }
                withClue("this is not a fight — your creature takes no damage back") {
                    damageOn(game, mine) shouldBe 0
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
                withClue("3 damage does not kill a 6/7") {
                    game.isOnBattlefield("Test Cave Troll") shouldBe true
                }
            }

            test("the damage tracks the current power of your creature, not its printed power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Quarrel")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Test Cave Troll")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mine = game.findPermanent("Centaur Courser")!!
                val theirs = game.findPermanent("Test Cave Troll")!!

                // Giant Growth makes the Courser a 6/6 first.
                game.castSpell(1, "Giant Growth", targetId = mine).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.state.projectedState.getPower(mine) shouldBe 6

                val spell = game.state.getHand(game.player1Id).single { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Quarrel"
                }
                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the pumped 6 power is what gets dealt") {
                    damageOn(game, theirs) shouldBe 6
                }
            }
        }
    }
}
