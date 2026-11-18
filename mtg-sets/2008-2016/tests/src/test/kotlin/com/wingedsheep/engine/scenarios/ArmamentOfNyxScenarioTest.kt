package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Armament of Nyx (JOU #4): "Enchanted creature has double strike as long as it's an enchantment.
 * Otherwise, prevent all damage that would be dealt by enchanted creature."
 *
 * Both halves read the host's projected types: an enchantment creature gets double strike and
 * deals its damage; a non-enchantment creature gets no double strike and every point of damage it
 * would deal — combat or not — is prevented.
 */
class ArmamentOfNyxScenarioTest : ScenarioTestBase() {

    init {
        test("an enchantment creature gains double strike and deals damage in both combat damage steps") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Eidolon of Rhetoric")
                .withCardAttachedTo(1, "Armament of Nyx", "Eidolon of Rhetoric")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val eidolon = game.findPermanent("Eidolon of Rhetoric")!!
            game.state.projectedState.hasKeyword(eidolon, Keyword.DOUBLE_STRIKE) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Eidolon of Rhetoric" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the 1/4 Eidolon hits twice for 1 each") {
                game.getLifeTotal(2) shouldBe 18
            }
        }

        test("a non-enchantment creature gets no double strike and its combat damage is prevented") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Armament of Nyx", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe false

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("all combat damage from the enchanted Bears is prevented") {
                game.getLifeTotal(2) shouldBe 20
            }
        }

        test("noncombat damage dealt by an enchanted non-enchantment creature is prevented too") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
                .withCardAttachedTo(1, "Armament of Nyx", "Prodigal Sorcerer")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = sorcerer,
                    abilityId = cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities[0].id,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the Sorcerer's ping is prevented") {
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
