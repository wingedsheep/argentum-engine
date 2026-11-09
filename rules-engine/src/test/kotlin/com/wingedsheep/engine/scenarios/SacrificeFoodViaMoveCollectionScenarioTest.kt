package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Guards that a Food sacrificed through the MoveCollection pipeline (gather → MoveCollectionEffect
 * with MoveType.Sacrifice) lands in the controller's per-turn sacrifice record — what "if you've
 * sacrificed a Food this turn" payoffs (`Conditions.SacrificedFoodThisTurn`) read.
 * trackPermanentSacrifice appends to that record on the controller entity, not the moved card, so
 * the MoveCollection executor has to carry it onto its result, or Foods sacrificed this way go unseen
 * by Food payoffs while the cost-sacrifice path (SacrificeExecutor) registers them.
 *
 * Inline card, no set dependency.
 */
class SacrificeFoodViaMoveCollectionScenarioTest : ScenarioTestBase() {

    // "Sacrifice each Food you control." — gather the Foods, then move them to the graveyard as a
    // sacrifice (the MoveCollectionEffect path, distinct from the cost-sacrifice SacrificeExecutor).
    private val sacrificeYourFoods = card("Sacrifice Your Foods") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = CompositeEffect(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.FromZone(
                            Zone.BATTLEFIELD,
                            Player.You,
                            GameObjectFilter.Artifact.withSubtype("Food")
                        ),
                        storeAs = "foods"
                    ),
                    MoveCollectionEffect(
                        from = "foods",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD),
                        moveType = MoveType.Sacrifice
                    )
                )
            )
        }
    }

    init {
        cardRegistry.register(sacrificeYourFoods)

        context("sacrificing a Food via the MoveCollection pipeline") {

            test("records the Food-sacrifice tag on the controller, like the cost-sacrifice path does") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withCardInHand(1, "Sacrifice Your Foods")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.findPermanents("Food").size shouldBe 1

                game.castSpell(1, "Sacrifice Your Foods")
                game.resolveStack()

                withClue("the Food is sacrificed — the token leaves the battlefield") {
                    game.findPermanents("Food").size shouldBe 0
                }
                withClue("sacrificing a Food this way tags the controller for 'you sacrificed a Food this turn' payoffs") {
                    services.predicateEvaluator.conditions.evaluate(
                        game.state,
                        Conditions.SacrificedFoodThisTurn,
                        EffectContext(sourceId = null, controllerId = game.player1Id),
                    ) shouldBe true
                }
            }
        }
    }
}
