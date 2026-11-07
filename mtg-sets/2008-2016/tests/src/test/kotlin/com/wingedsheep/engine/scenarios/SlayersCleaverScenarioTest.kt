package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Slayer's Cleaver (EMN #198) — {3} Artifact — Equipment.
 *
 *   Equipped creature gets +3/+1 and must be blocked by an Eldrazi if able.
 *   Equip {4}
 */
class SlayersCleaverScenarioTest : ScenarioTestBase() {

    private val equipAbilityId =
        cardRegistry.getCard("Slayer's Cleaver")!!.activatedAbilities.first { it.isEquipAbility }.id

    private fun equippedAttack(eldraziTapped: Boolean = false): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Slayer's Cleaver")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withCardOnBattlefield(2, "Lashweed Lurker", tapped = eldraziTapped)
            .withCardOnBattlefield(2, "Glory Seeker")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val bears = game.findPermanent("Grizzly Bears")!!
        val equip = game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = game.findPermanent("Slayer's Cleaver")!!,
                abilityId = equipAbilityId,
                targets = listOf(ChosenTarget.Permanent(bears)),
            )
        )
        withClue("Equip should succeed: ${equip.error}") { equip.error shouldBe null }
        if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
        game.resolveStack()

        withClue("equipped creature gets +3/+1") {
            game.state.projectedState.getPower(bears) shouldBe 5
            game.state.projectedState.getToughness(bears) shouldBe 3
        }
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game
    }

    init {
        test("an untapped Eldrazi must block the equipped creature; a non-Eldrazi block doesn't count") {
            val game = equippedAttack()
            withClue("no block") { game.declareNoBlockers().error shouldNotBe null }
            withClue("only a non-Eldrazi blocks") {
                game.declareBlockers(mapOf("Glory Seeker" to listOf("Grizzly Bears"))).error shouldNotBe null
            }
            withClue("the Eldrazi blocks") {
                game.declareBlockers(mapOf("Lashweed Lurker" to listOf("Grizzly Bears"))).error shouldBe null
            }
        }

        test("a non-Eldrazi may block alongside the Eldrazi") {
            val game = equippedAttack()
            game.declareBlockers(
                mapOf("Lashweed Lurker" to listOf("Grizzly Bears"), "Glory Seeker" to listOf("Grizzly Bears"))
            ).error shouldBe null
        }

        test("with no Eldrazi able to block, nothing is forced") {
            val game = equippedAttack(eldraziTapped = true)
            game.declareNoBlockers().error shouldBe null
        }
    }
}
