package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Slobad, Iron Goblin (ONE #149):
 * "{T}, Sacrifice an artifact: Add an amount of {R} equal to the sacrificed artifact's mana value.
 *  Spend this mana only to cast artifact spells or activate abilities of artifacts."
 */
class SlobadIronGoblinScenarioTest : ScenarioTestBase() {

    init {
        fun poolAfterSacrificing(artifactName: String): ManaPoolComponent {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Slobad, Iron Goblin", summoningSickness = false)
                .withCardOnBattlefield(1, artifactName)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val slobad = game.findPermanent("Slobad, Iron Goblin")!!
            val artifact = game.findPermanent(artifactName)!!
            val ability = cardRegistry.getCard("Slobad, Iron Goblin")!!.script.activatedAbilities[0]

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = slobad,
                    abilityId = ability.id,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(artifact))
                )
            )
            withClue("Activating Slobad should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()
            withClue("the sacrificed artifact is gone") { game.isOnBattlefield(artifactName) shouldBe false }
            return game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        }

        val artifactOnly = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
            cardType = CardType.ARTIFACT,
            allowSpells = true,
            allowAbilities = true,
        )

        context("Slobad, Iron Goblin") {
            test("sacrificing a mana-value-2 artifact adds two artifact-restricted {R}") {
                val pool = poolAfterSacrificing("Millstone")
                pool.red shouldBe 0
                pool.restrictedMana.size shouldBe 2
                pool.restrictedMana.all { it.color == Color.RED && it.restriction == artifactOnly } shouldBe true
            }

            test("sacrificing a mana-value-0 artifact adds no mana") {
                val pool = poolAfterSacrificing("Ornithopter")
                pool.red shouldBe 0
                pool.restrictedMana.size shouldBe 0
            }
        }
    }
}
