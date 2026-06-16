package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Sharkey, Tyrant of the Shire (LTR):
 *  - "Activated abilities of lands your opponents control can't be activated unless they're mana abilities."
 *  - "Sharkey has all activated abilities of lands your opponents control except mana abilities."
 *  - "Mana of any type can be spent to activate Sharkey's abilities."
 *
 * Test land: Memorial to Genius (DOM) — a land with both a mana ability ({T}: Add {U})
 * and a non-mana activated ability ({4}{U}, {T}, Sacrifice Memorial to Genius: Draw two cards).
 */
class SharkeyTyrantOfTheShireScenarioTest : ScenarioTestBase() {

    init {
        context("Piece 1: opponents' lands' non-mana abilities are locked, mana abilities are not") {
            test("the opponent's Memorial to Genius cannot activate its non-mana draw ability while Sharkey is out") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Sharkey, Tyrant of the Shire", summoningSickness = false)
                    .withCardOnBattlefield(2, "Memorial to Genius")
                    .withLandsOnBattlefield(2, "Island", 5)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val memorial = game.findPermanent("Memorial to Genius")!!
                val memorialDef = cardRegistry.getCard("Memorial to Genius")!!
                val drawAbility = memorialDef.script.activatedAbilities.first { !it.isManaAbility }

                val p2Legal = game.getLegalActions(2)
                val drawActivation = p2Legal.find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == memorial && a.abilityId == drawAbility.id
                }
                withClue("Opponent's land non-mana ability must be locked while Sharkey is in play") {
                    drawActivation shouldBe null
                }

                // The handler must also reject a direct activation attempt.
                val result = game.execute(
                    ActivateAbility(playerId = game.player2Id, sourceId = memorial, abilityId = drawAbility.id)
                )
                withClue("Engine should reject the opponent's non-mana land ability") {
                    (result.error != null) shouldBe true
                }
            }

            test("the opponent's Memorial to Genius can still activate its mana ability while Sharkey is out") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Sharkey, Tyrant of the Shire", summoningSickness = false)
                    .withCardOnBattlefield(2, "Memorial to Genius")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val memorial = game.findPermanent("Memorial to Genius")!!

                val p2Legal = game.getLegalActions(2)
                val manaActivation = p2Legal.find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == memorial
                }
                withClue("Mana abilities of opponents' lands remain usable ('unless they're mana abilities')") {
                    (manaActivation != null).shouldBeTrue()
                }
            }
        }

        context("Piece 2: Sharkey gains the opponent's land's non-mana activated abilities") {
            test("Sharkey has Memorial to Genius's draw ability available as a legal action") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Sharkey, Tyrant of the Shire", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(2, "Memorial to Genius")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sharkey = game.findPermanent("Sharkey, Tyrant of the Shire")!!
                val memorialDef = cardRegistry.getCard("Memorial to Genius")!!
                val drawAbility = memorialDef.script.activatedAbilities.first { !it.isManaAbility }

                val p1Legal = game.getLegalActions(1)
                val sharkeyDraw = p1Legal.find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == sharkey && a.abilityId == drawAbility.id
                }
                withClue("Sharkey should have gained Memorial to Genius's draw ability") {
                    (sharkeyDraw != null).shouldBeTrue()
                }
            }

            test("Sharkey does NOT gain the opponent's land's mana ability") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Sharkey, Tyrant of the Shire", summoningSickness = false)
                    .withCardOnBattlefield(2, "Memorial to Genius")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sharkey = game.findPermanent("Sharkey, Tyrant of the Shire")!!
                val memorialDef = cardRegistry.getCard("Memorial to Genius")!!
                val manaAbility = memorialDef.script.activatedAbilities.first { it.isManaAbility }

                val p1Legal = game.getLegalActions(1)
                val sharkeyMana = p1Legal.find {
                    val a = it.action
                    a is ActivateAbility && a.sourceId == sharkey && a.abilityId == manaAbility.id
                }
                withClue("'except mana abilities' — Sharkey must not gain the land's mana ability") {
                    sharkeyMana shouldBe null
                }
            }
        }

        context("Piece 3: any mana type can pay for Sharkey's (gained) abilities") {
            test("Sharkey activates the gained {4}{U} draw ability paying entirely with red mana") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Sharkey, Tyrant of the Shire", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(2, "Memorial to Genius")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sharkey = game.findPermanent("Sharkey, Tyrant of the Shire")!!
                val memorialDef = cardRegistry.getCard("Memorial to Genius")!!
                val drawAbility = memorialDef.script.activatedAbilities.first { !it.isManaAbility }

                val handBefore = game.handSize(1)

                // 5 Mountains can only produce {R}{R}{R}{R}{R}; the gained ability costs {4}{U}.
                // Without "mana of any type can be spent" the {U} pip would be unpayable.
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = sharkey, abilityId = drawAbility.id)
                )
                withClue("Sharkey's gained ability must be activatable with any mana type (5 red pays {4}{U})") {
                    (result.error == null) shouldBe true
                }
                game.resolveStack()

                withClue("The gained draw-two ability should resolve and draw two cards") {
                    game.handSize(1) shouldBe handBefore + 2
                }
                // SacrificeSelf in the copied ability sacrifices the gainer (Sharkey), CR 113.2.
                withClue("Sharkey should be sacrificed by the gained ability's SacrificeSelf cost") {
                    game.isOnBattlefield("Sharkey, Tyrant of the Shire") shouldBe false
                }
            }
        }
    }
}
