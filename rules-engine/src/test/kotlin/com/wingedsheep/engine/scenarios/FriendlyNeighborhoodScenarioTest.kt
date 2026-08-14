package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Friendly Neighborhood (SPM #8) — {3}{W} Enchantment — Aura.
 *
 * "Enchant land
 *  When this Aura enters, create three 1/1 green and white Human Citizen creature tokens.
 *  Enchanted land has \"{1}, {T}: Target creature gets +1/+1 until end of turn for each
 *  creature you control. Activate only as a sorcery.\""
 *
 * Pins (1) the ETB three-token trigger and (2) the granted sorcery-speed pump on the enchanted
 * land whose +N/+N scales with the number of creatures its controller controls.
 */
class FriendlyNeighborhoodScenarioTest : ScenarioTestBase() {

    /** The pump ability the Aura's static grants to the enchanted land. */
    private val pumpAbilityId by lazy {
        cardRegistry.requireCard("Friendly Neighborhood").script.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().first().ability.id
    }

    init {
        context("Friendly Neighborhood") {

            test("its enters trigger creates three 1/1 green and white Human Citizen tokens") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Friendly Neighborhood")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!
                game.castSpell(1, "Friendly Neighborhood", forest).error shouldBe null
                game.resolveStack()

                val tokens = game.findPermanents("Human Citizen Token")
                withClue("three Human Citizen tokens are created") {
                    tokens.size shouldBe 3
                }
                tokens.forEach { token ->
                    withClue("each token is a 1/1 green and white Human Citizen") {
                        game.state.projectedState.getPower(token) shouldBe 1
                        game.state.projectedState.getToughness(token) shouldBe 1
                        game.state.projectedState.hasColor(token, Color.GREEN) shouldBe true
                        game.state.projectedState.hasColor(token, Color.WHITE) shouldBe true
                        game.state.projectedState.hasSubtype(token, "Human") shouldBe true
                        game.state.projectedState.hasSubtype(token, "Citizen") shouldBe true
                    }
                }
            }

            test("the enchanted land's granted pump gives +1/+1 for each creature you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardAttachedTo(1, "Friendly Neighborhood", "Forest")
                    // Three creatures you control: the target plus two others.
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 1) // pays the {1}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!
                val bears = game.findPermanents("Grizzly Bears")
                val target = bears.first()

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = forest,
                        abilityId = pumpAbilityId,
                        targets = listOf(ChosenTarget.Permanent(target)),
                    )
                )
                withClue("activating the granted pump should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("+1/+1 for each of the three creatures you control → 2/2 becomes 5/5") {
                    game.state.projectedState.getPower(target) shouldBe 5
                    game.state.projectedState.getToughness(target) shouldBe 5
                }
            }

            test("the granted pump can't be activated at instant speed (sorcery only)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardAttachedTo(1, "Friendly Neighborhood", "Forest")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    // Your own turn, but the combat phase — not a legal time for a sorcery-speed
                    // activation (sorcery speed requires a main phase with an empty stack).
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                    .build()

                val forest = game.findPermanent("Forest")!!
                val target = game.findPermanent("Grizzly Bears")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = forest,
                        abilityId = pumpAbilityId,
                        targets = listOf(ChosenTarget.Permanent(target)),
                    )
                )
                withClue("a sorcery-speed ability can't be activated outside a main phase") {
                    activation.error shouldNotBe null
                }
            }
        }
    }
}
