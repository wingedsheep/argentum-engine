package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Bloom Tender.
 *
 * Card reference:
 * - Bloom Tender ({1}{G}): Creature — Elf Druid 1/1
 *   "Vivid — {T}: For each color among permanents you control, add one mana of that color."
 */
class BloomTenderScenarioTest : ScenarioTestBase() {

    init {
        context("Bloom Tender mana ability") {
            test("produces one green mana when only Bloom Tender is on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bloom Tender")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bloomTender = game.findPermanent("Bloom Tender")!!
                val cardDef = cardRegistry.getCard("Bloom Tender")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bloomTender,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully") {
                    result.isSuccess shouldBe true
                }

                val manaPool = result.newState.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Should have exactly 1 green mana (Bloom Tender is green)") {
                    manaPool shouldNotBe null
                    manaPool!!.green shouldBe 1
                    manaPool.white shouldBe 0
                    manaPool.blue shouldBe 0
                    manaPool.black shouldBe 0
                    manaPool.red shouldBe 0
                }
            }

            test("produces mana for every distinct color across permanents") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bloom Tender")           // Green
                    .withCardOnBattlefield(1, "Serra Angel")            // White
                    .withCardOnBattlefield(1, "Adeliz, the Cinder Wind") // Blue + Red
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bloomTender = game.findPermanent("Bloom Tender")!!
                val cardDef = cardRegistry.getCard("Bloom Tender")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bloomTender,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully") {
                    result.isSuccess shouldBe true
                }

                val manaPool = result.newState.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Should produce one mana of each color present: W, U, R, G (4 colors)") {
                    manaPool shouldNotBe null
                    manaPool!!.white shouldBe 1
                    manaPool.blue shouldBe 1
                    manaPool.red shouldBe 1
                    manaPool.green shouldBe 1
                    manaPool.black shouldBe 0
                }
            }

            test("does not count opponent's permanents") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bloom Tender")          // Green (player 1)
                    .withCardOnBattlefield(2, "Serra Angel")           // White (player 2 - should NOT count)
                    .withCardOnBattlefield(2, "Adeliz, the Cinder Wind") // U/R (player 2 - should NOT count)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bloomTender = game.findPermanent("Bloom Tender")!!
                val cardDef = cardRegistry.getCard("Bloom Tender")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bloomTender,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully") {
                    result.isSuccess shouldBe true
                }

                val manaPool = result.newState.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Should only produce green — opponent's permanents don't contribute") {
                    manaPool shouldNotBe null
                    manaPool!!.green shouldBe 1
                    manaPool.white shouldBe 0
                    manaPool.blue shouldBe 0
                    manaPool.red shouldBe 0
                    manaPool.black shouldBe 0
                }
            }

            test("colorless permanents do not contribute any mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bloom Tender")    // Green
                    .withCardOnBattlefield(1, "Mox Amber")       // Colorless artifact
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bloomTender = game.findPermanent("Bloom Tender")!!
                val cardDef = cardRegistry.getCard("Bloom Tender")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bloomTender,
                        abilityId = ability.id
                    )
                )

                withClue("Ability should activate successfully") {
                    result.isSuccess shouldBe true
                }

                val manaPool = result.newState.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Should produce only green — colorless Mox Amber contributes nothing") {
                    manaPool shouldNotBe null
                    manaPool!!.green shouldBe 1
                    manaPool.white shouldBe 0
                    manaPool.blue shouldBe 0
                    manaPool.red shouldBe 0
                    manaPool.black shouldBe 0
                }
            }
        }
    }
}
