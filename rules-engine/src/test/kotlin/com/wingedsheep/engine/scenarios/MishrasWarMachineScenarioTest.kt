package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Mishra's War Machine (ATQ #57).
 *
 * {7} Artifact Creature — Juggernaut 5/5, Banding
 * "At the beginning of your upkeep, this creature deals 3 damage to you unless you discard a card.
 *  If it deals damage to you this way, tap it."
 *
 * Exercises the SuccessCriterion.DamageDealt gate: the tap happens only when the damage was actually
 * dealt to you (not when avoided by discarding, and not when the damage was prevented).
 */
class MishrasWarMachineScenarioTest : ScenarioTestBase() {

    init {
        fun isTapped(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Boolean =
            game.state.getEntity(id)?.has<TappedComponent>() == true

        context("Mishra's War Machine") {

            test("deals 3 to you and taps itself when you don't discard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mishra's War Machine")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // With no card in hand there is nothing to discard, so the damage is dealt directly
                // (no decision) and the gate taps the machine.
                val machine = game.findPermanent("Mishra's War Machine")!!
                withClue("Took 3 damage (no card to discard)") {
                    game.getLifeTotal(1) shouldBe 17
                }
                withClue("It dealt damage to you this way, so it is tapped") {
                    isTapped(game, machine) shouldBe true
                }
            }

            test("does not deal damage or tap when you discard a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mishra's War Machine")
                    .withCardInHand(1, "Mountain")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // Discard the card to avoid the damage.
                val handCard = game.findCardsInHand(1, "Mountain").first()
                game.selectCards(listOf(handCard)).error shouldBe null
                game.resolveStack()

                val machine = game.findPermanent("Mishra's War Machine")!!
                withClue("Discarded → no damage dealt") {
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("No damage dealt to you → it is NOT tapped") {
                    isTapped(game, machine) shouldBe false
                }
                withClue("The card was discarded from hand") {
                    game.isInHand(1, "Mountain") shouldBe false
                }
            }

            test("does not tap when the 3 damage to you is fully prevented") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mishra's War Machine")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Shield the active player against all damage this turn. There is no card to discard,
                // so the upkeep trigger reaches the "deal 3 damage to you" branch — but the shield
                // reduces it to 0, so no DamageDealtEvent is emitted. This is the corner that makes
                // SuccessCriterion.DamageDealt different from a plain "the suffer branch ran" gate:
                // damage that was prevented must NOT count as "it dealt damage to you this way".
                val player1 = game.state.activePlayerId!!
                val (shieldId, withShield) = game.state.newEntity()
                game.state = withShield.copy(
                    floatingEffects = withShield.floatingEffects + ActiveFloatingEffect(
                        id = shieldId,
                        effect = FloatingEffectData(
                            layer = Layer.ABILITY,
                            modification = SerializableModification.PreventAllDamageTo(),
                            affectedEntities = setOf(player1),
                        ),
                        duration = Duration.EndOfTurn,
                        sourceId = null,
                        controllerId = player1,
                        timestamp = game.state.timestamp,
                    )
                )

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                val machine = game.findPermanent("Mishra's War Machine")!!
                withClue("Damage prevented → life unchanged") {
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("No damage actually dealt to you (prevented) → it is NOT tapped") {
                    isTapped(game, machine) shouldBe false
                }
            }
        }
    }
}
