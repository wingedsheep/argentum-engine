package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.spm.cards.StrengthOfWill
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Strength of Will (SPM #118).
 *
 * {1}{G} Instant
 * "Until end of turn, target creature you control gains indestructible and
 *  \"Whenever this creature is dealt damage, put that many +1/+1 counters on it.\""
 */
class StrengthOfWillScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    // A vanilla 2/2 under my control (locally defined so its P/T is trustworthy for the
    // "survives lethal damage" math — see repo note about stub catalog P/T).
    private val willboundOx = card("Willbound Ox") {
        manaCost = "{2}"
        typeLine = "Creature — Ox"
        power = 2
        toughness = 2
    }

    // A {0} sorcery that deals 3 damage to a target creature — the damage source.
    private val threeBolt = card("Three Bolt") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Deal 3 damage to target creature."
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.DealDamage(3, t)
        }
    }

    init {
        cardRegistry.register(willboundOx)
        cardRegistry.register(threeBolt)
        cardRegistry.register(StrengthOfWill)

        context("Strength of Will") {

            test("grants indestructible + dealt-damage counters; survives lethal damage this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Willbound Ox", summoningSickness = false)
                    .withCardInHand(1, "Strength of Will")
                    .withCardInHand(1, "Three Bolt")
                    .withLandsOnBattlefield(1, "Forest", 2) // pays {1}{G}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oxId = game.findPermanent("Willbound Ox")!!

                // Cast Strength of Will on the Ox.
                game.castSpell(1, "Strength of Will", oxId).error shouldBe null
                game.resolveStack()

                withClue("Ox gains indestructible from Strength of Will") {
                    projector.hasProjectedKeyword(game.state, oxId, Keyword.INDESTRUCTIBLE) shouldBe true
                }

                // Deal 3 damage to the 2/2 Ox. Without indestructible, marked damage (3) >= toughness (2)
                // would destroy it before the trigger resolves.
                game.castSpell(1, "Three Bolt", oxId).error shouldBe null
                game.resolveStack()

                withClue("Ox survives lethal damage thanks to indestructible") {
                    game.isOnBattlefield("Willbound Ox") shouldBe true
                }
                withClue("Granted trigger put 3 +1/+1 counters (one per point of damage dealt)") {
                    val counters = game.state.getEntity(oxId)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    counters shouldBe 3
                }
                withClue("Ox is now a 5/5 (2/2 base + three +1/+1 counters)") {
                    projector.getProjectedPower(game.state, oxId) shouldBe 5
                    projector.getProjectedToughness(game.state, oxId) shouldBe 5
                }
            }

            test("both grants expire at end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Willbound Ox", summoningSickness = false)
                    .withCardInHand(1, "Strength of Will")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oxId = game.findPermanent("Willbound Ox")!!

                game.castSpell(1, "Strength of Will", oxId).error shouldBe null
                game.resolveStack()
                projector.hasProjectedKeyword(game.state, oxId, Keyword.INDESTRUCTIBLE) shouldBe true

                // Advance to end of turn, then on into the opponent's turn — the EndOfTurn
                // grants are cleaned up during the intervening cleanup step.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue("Indestructible grant expired at end of turn") {
                    projector.hasProjectedKeyword(game.state, oxId, Keyword.INDESTRUCTIBLE) shouldBe false
                }
            }
        }
    }
}
