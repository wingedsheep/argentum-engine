package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Rakdos Joins Up (OTJ #225) — {3}{B}{R} Legendary Enchantment.
 *
 *   "When Rakdos Joins Up enters, return target creature card from your graveyard to the
 *    battlefield with two additional +1/+1 counters on it.
 *    Whenever a legendary creature you control dies, Rakdos Joins Up deals damage equal to
 *    that creature's power to target opponent."
 *
 * Verifies the ETB reanimation (with two +1/+1 counters) and the dies-trigger drain using
 * the dying creature's last-known power.
 */
class RakdosJoinsUpScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Legend",
                manaCost = ManaCost.parse("{1}"),
                subtypes = setOf(Subtype.HUMAN),
                supertypes = setOf(Supertype.LEGENDARY),
                power = 4,
                toughness = 1,
            )
        )
        cardRegistry.register(
            card("Test Slay") {
                manaCost = "{B}"
                typeLine = "Instant"
                oracleText = "Destroy target creature."
                spell {
                    val t = target("target creature", Targets.Creature)
                    effect = Effects.Destroy(t)
                }
            }
        )

        context("Rakdos Joins Up") {

            test("ETB reanimates a creature from your graveyard with two +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Rakdos Joins Up")
                    .withCardInGraveyard(1, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Rakdos Joins Up").error shouldBe null
                game.resolveStack()

                val bearsCard = game.findCardsInGraveyard(1, "Grizzly Bears").first()
                game.selectTargets(listOf(bearsCard))
                game.resolveStack()

                withClue("Grizzly Bears is reanimated onto the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Reanimated with two additional +1/+1 counters") {
                    game.state.getEntity(bears)
                        ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                        ?.getCount(com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
            }

            test("a legendary creature you control dying drains the opponent for its power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rakdos Joins Up")
                    .withCardOnBattlefield(1, "Test Legend") // 4/1 legendary
                    .withCardInHand(1, "Test Slay")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p2LifeBefore = game.getLifeTotal(2)
                val legend = game.findPermanent("Test Legend")!!

                // Destroy the legendary creature so the dies trigger fires.
                val slayId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Test Slay"
                }
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = slayId,
                        targets = listOf(ChosenTarget.Permanent(legend)),
                    )
                ).error shouldBe null
                game.resolveStack()

                // The trigger targets Player2.
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(game.player2Id))
                }
                game.resolveStack()

                withClue("Opponent loses life equal to the dead legend's power (4)") {
                    game.getLifeTotal(2) shouldBe p2LifeBefore - 4
                }
            }
        }
    }
}
