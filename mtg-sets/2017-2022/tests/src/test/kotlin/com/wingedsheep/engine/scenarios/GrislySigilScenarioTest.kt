package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Grisly Sigil (SNC #82) — {B} Sorcery.
 *
 *   Casualty 1. Choose target creature or planeswalker. If it was dealt noncombat damage this
 *   turn, Grisly Sigil deals 3 damage to it and you gain 3 life. Otherwise, Grisly Sigil deals 1
 *   damage to it and you gain 1 life.
 *
 * Pins each branch of the noncombat-damage check: an untouched target, one hurt by a spell, one
 * hurt only in combat (still the small branch), and the casualty copy priming the original.
 */
class GrislySigilScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.damageOn(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    init {
        test("an undamaged target takes 1 damage and you gain 1 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Grisly Sigil")
                .withCardOnBattlefield(2, "Enormous Baloth")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val baloth = game.findPermanent("Enormous Baloth")!!
            game.castSpell(1, "Grisly Sigil", baloth).error shouldBe null
            game.resolveStack()

            game.damageOn(baloth) shouldBe 1
            game.getLifeTotal(1) shouldBe 21
        }

        test("a target dealt noncombat damage this turn takes 3 damage and you gain 3 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Grisly Sigil")
                .withCardInHand(1, "Shock")
                .withCardOnBattlefield(2, "Enormous Baloth")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val baloth = game.findPermanent("Enormous Baloth")!!
            game.castSpell(1, "Shock", baloth).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Grisly Sigil", baloth).error shouldBe null
            game.resolveStack()

            game.damageOn(baloth) shouldBe 2 + 3
            game.getLifeTotal(1) shouldBe 23
        }

        test("combat damage alone doesn't count — the target takes 1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Grisly Sigil")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Enormous Baloth", summoningSickness = false)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val baloth = game.findPermanent("Enormous Baloth")!!
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Enormous Baloth" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("The Bears dealt 2 combat damage to the Baloth") { game.damageOn(baloth) shouldBe 2 }

            game.castSpell(1, "Grisly Sigil", baloth).error shouldBe null
            game.resolveStack()

            game.damageOn(baloth) shouldBe 2 + 1
            game.getLifeTotal(1) shouldBe 21
        }

        test("with casualty, the copy's 1 damage primes the original for 3") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Grisly Sigil")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(2, "Enormous Baloth")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val baloth = game.findPermanent("Enormous Baloth")!!
            val lions = game.findPermanent("Savannah Lions")!!
            val sigil = game.findCardsInHand(1, "Grisly Sigil").single()
            game.execute(
                CastSpell(game.player1Id, sigil, listOf(ChosenTarget.Permanent(baloth)), casualtyCreature = lions)
            ).error shouldBe null
            game.resolveStack()
            withClue("The casualty copy asks for its target") {
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            }
            game.selectTargets(listOf(baloth)).error shouldBe null
            game.resolveStack()

            withClue("Copy: 1 damage + 1 life; original: 3 damage + 3 life") {
                game.damageOn(baloth) shouldBe 1 + 3
                game.getLifeTotal(1) shouldBe 24
            }
        }
    }
}
