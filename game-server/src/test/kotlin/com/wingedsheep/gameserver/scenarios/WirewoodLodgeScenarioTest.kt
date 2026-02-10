package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Wirewood Lodge.
 *
 * Card reference:
 * - Wirewood Lodge: Land
 *   {T}: Add {C}.
 *   {G}, {T}: Untap target Elf.
 */
class WirewoodLodgeScenarioTest : ScenarioTestBase() {

    init {
        context("Wirewood Lodge untap ability") {
            test("untaps a tapped Elf") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Wirewood Lodge")
                    .withCardOnBattlefield(1, "Wellwisher", tapped = true)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lodge = game.findPermanent("Wirewood Lodge")!!
                val wellwisher = game.findPermanent("Wellwisher")!!

                // Verify Wellwisher is tapped
                withClue("Wellwisher should start tapped") {
                    game.state.getEntity(wellwisher)?.has<TappedComponent>() shouldBe true
                }

                // Activate {G}, {T}: Untap target Elf
                val cardDef = cardRegistry.getCard("Wirewood Lodge")!!
                val untapAbility = cardDef.script.activatedAbilities[1] // second ability

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = lodge,
                        abilityId = untapAbility.id,
                        targets = listOf(ChosenTarget.Permanent(wellwisher))
                    )
                )

                withClue("Ability should activate successfully") {
                    result.error shouldBe null
                }

                game.resolveStack()

                // Wellwisher should now be untapped
                withClue("Wellwisher should be untapped after Lodge ability resolves") {
                    game.state.getEntity(wellwisher)?.has<TappedComponent>() shouldBe false
                }
            }

            test("cannot target a non-Elf creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wirewood Lodge")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lodge = game.findPermanent("Wirewood Lodge")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val cardDef = cardRegistry.getCard("Wirewood Lodge")!!
                val untapAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = lodge,
                        abilityId = untapAbility.id,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )

                withClue("Should fail targeting a non-Elf") {
                    (result.error != null) shouldBe true
                }
            }

            test("can untap an opponent's Elf") {
                val game = scenario()
                    .withPlayers("Player", "Elf Opponent")
                    .withCardOnBattlefield(1, "Wirewood Lodge")
                    .withCardOnBattlefield(2, "Elvish Warrior", tapped = true)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lodge = game.findPermanent("Wirewood Lodge")!!
                val opponentElf = game.findPermanent("Elvish Warrior")!!

                withClue("Opponent's Elf should start tapped") {
                    game.state.getEntity(opponentElf)?.has<TappedComponent>() shouldBe true
                }

                val cardDef = cardRegistry.getCard("Wirewood Lodge")!!
                val untapAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = lodge,
                        abilityId = untapAbility.id,
                        targets = listOf(ChosenTarget.Permanent(opponentElf))
                    )
                )

                withClue("Should be able to target opponent's Elf") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Opponent's Elf should be untapped") {
                    game.state.getEntity(opponentElf)?.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
