package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the Secrets of Strixhaven (SOS) conditional-effect cards:
 *
 *  - Foolish Fate: destroy + intervening-if drain ("if you gained life this turn").
 *  - Burrog Barrage: intervening-if pump ("if you've cast another instant or sorcery") +
 *    deal-damage-equal-to-power to an optional second target.
 *  - Duel Tactics: deal damage + can't-block-this-turn, with flashback recast from graveyard.
 */
class SosConditionalCardsScenarioTest : ScenarioTestBase() {

    init {
        context("Foolish Fate") {

            test("with no life gained this turn: only destroys, no life loss") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Foolish Fate")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Foolish Fate", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("creature is destroyed") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("no life lost — no life was gained this turn") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("after gaining life this turn: destroys AND the controller loses 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Foolish Fate")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Record that the caster gained life this turn so the intervening-if is satisfied.
                game.state = game.state.updateEntity(game.player1Id) {
                    it.withComponent(LifeGainedAmountThisTurnComponent(3))
                }

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Foolish Fate", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("creature is destroyed") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("the destroyed creature's controller loses 3 life") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }
        }

        context("Burrog Barrage") {

            test("no other instant/sorcery cast: no +1/+0, deals power-2 damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Burrog Barrage")
                    .withCardOnBattlefield(1, "Grizzly Bears")     // 2/2 attacker
                    .withCardOnBattlefield(2, "Grizzly Bears")     // 2/2 target
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val own = game.findPermanents("Grizzly Bears").first { game.state.projectedState.getController(it) == game.player1Id }
                val foe = game.findPermanents("Grizzly Bears").first { game.state.projectedState.getController(it) == game.player2Id }

                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Burrog Barrage"
                }
                game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(ChosenTarget.Permanent(own), ChosenTarget.Permanent(foe)),
                    ),
                ).error shouldBe null
                game.resolveStack()

                withClue("no pump (power stays 2)") {
                    game.state.projectedState.getPower(own) shouldBe 2
                }
                withClue("target took 2 damage and dies (2/2)") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("after casting another instant this turn: +1/+0 makes it deal 3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Burrog Barrage")
                    .withCardInHand(1, "Shock")                    // another instant
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")        // 3/3 target
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast another instant first to satisfy "cast another instant or sorcery this turn".
                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Shock", targetId = giant).error shouldBe null
                game.resolveStack()

                val own = game.findPermanent("Grizzly Bears")!!
                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Burrog Barrage"
                }
                game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(ChosenTarget.Permanent(own), ChosenTarget.Permanent(giant)),
                    ),
                ).error shouldBe null
                game.resolveStack()

                withClue("pump applied: power is 3") {
                    game.state.projectedState.getPower(own) shouldBe 3
                }
                withClue("Hill Giant (3/3) took 2 from Shock + 3 from Burrog Barrage = dead") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }
        }

        context("Duel Tactics") {

            test("deals 1 damage to target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Duel Tactics")
                    .withCardOnBattlefield(2, "Mons's Goblin Raiders") // 1/1 dies to 1 damage
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goblin = game.findPermanent("Mons's Goblin Raiders")!!
                game.castSpell(1, "Duel Tactics", targetId = goblin).error shouldBe null
                game.resolveStack()

                withClue("1/1 took 1 damage and died") {
                    game.isInGraveyard(2, "Mons's Goblin Raiders") shouldBe true
                }
            }

            test("flashback recast from graveyard deals damage, then exiles the card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Duel Tactics")
                    .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goblin = game.findPermanent("Mons's Goblin Raiders")!!
                val cardId = game.state.getGraveyard(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Duel Tactics"
                }
                game.execute(
                    CastSpell(
                        game.player1Id,
                        cardId,
                        listOf(ChosenTarget.Permanent(goblin)),
                        useAlternativeCost = true,
                    ),
                ).error shouldBe null
                game.resolveStack()

                withClue("the 1/1 took 1 damage and died") {
                    game.isInGraveyard(2, "Mons's Goblin Raiders") shouldBe true
                }
                withClue("flashback exiles the card after it resolves (not back to graveyard)") {
                    game.isInGraveyard(1, "Duel Tactics") shouldBe false
                }
            }
        }
    }
}
