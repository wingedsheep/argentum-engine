package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.events.DamageType
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine-level coverage for `StatePredicate.WasDealtDamageThisTurn(damageType)` — the passive
 * damage-history predicate — and the `WasDealtDamageThisTurnComponent` marker behind it, which
 * records *which kinds* of damage (combat, noncombat) the object was dealt this turn.
 *
 *  1. **Kind matching** — each `DamageType` against each marker shape.
 *  2. **Recording paths**, in real games: combat damage to a creature marks it combat only; a
 *     spell's damage marks it noncombat only; both in one turn mark both; damage to a player is
 *     classified the same way; and the end-of-turn cleanup clears the marker.
 */
class WasDealtDamageThisTurnPredicateTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.marker(id: EntityId): WasDealtDamageThisTurnComponent? =
        state.getEntity(id)?.get<WasDealtDamageThisTurnComponent>()

    init {
        context("kind matching") {
            test("a combat-only marker matches Any and Combat, not NonCombat") {
                val m = WasDealtDamageThisTurnComponent(combat = true)
                m.matches(DamageType.Any) shouldBe true
                m.matches(DamageType.Combat) shouldBe true
                m.matches(DamageType.NonCombat) shouldBe false
            }

            test("a noncombat-only marker matches Any and NonCombat, not Combat") {
                val m = WasDealtDamageThisTurnComponent(noncombat = true)
                m.matches(DamageType.Any) shouldBe true
                m.matches(DamageType.Combat) shouldBe false
                m.matches(DamageType.NonCombat) shouldBe true
            }

            test("markedOn accumulates kinds rather than overwriting") {
                val once = WasDealtDamageThisTurnComponent.markedOn(ComponentContainer(), isCombatDamage = true)
                val twice = WasDealtDamageThisTurnComponent.markedOn(once, isCombatDamage = false)
                twice.get<WasDealtDamageThisTurnComponent>() shouldBe
                    WasDealtDamageThisTurnComponent(combat = true, noncombat = true)
            }
        }

        context("recording paths") {
            test("combat damage to a blocker marks it combat only") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.marker(giant) shouldBe WasDealtDamageThisTurnComponent(combat = true)
            }

            test("combat damage to a player marks the player combat only") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.marker(game.player2Id) shouldBe WasDealtDamageThisTurnComponent(combat = true)
            }

            test("a spell's damage marks creature and player noncombat only") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Shock")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Shock", giant).error shouldBe null
                game.resolveStack()
                game.marker(giant) shouldBe WasDealtDamageThisTurnComponent(noncombat = true)

                val shock = game.findCardsInHand(1, "Shock").single()
                game.execute(CastSpell(game.player1Id, shock, listOf(ChosenTarget.Player(game.player2Id)))).error shouldBe null
                game.resolveStack()
                game.marker(game.player2Id) shouldBe WasDealtDamageThisTurnComponent(noncombat = true)
            }

            test("noncombat then combat damage in one turn marks both kinds") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shock")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Enormous Baloth", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val baloth = game.findPermanent("Enormous Baloth")!!
                game.castSpell(1, "Shock", baloth).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Enormous Baloth" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Combat damage after Shock adds the combat kind instead of replacing the noncombat one") {
                    game.marker(baloth) shouldBe WasDealtDamageThisTurnComponent(combat = true, noncombat = true)
                }
            }

            test("the marker is cleared at end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shock")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Shock", giant).error shouldBe null
                game.resolveStack()
                game.marker(giant) shouldBe WasDealtDamageThisTurnComponent(noncombat = true)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("New turn: the per-turn marker is gone") {
                    game.marker(giant) shouldBe null
                }
            }
        }
    }
}
