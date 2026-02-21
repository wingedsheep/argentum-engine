package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Kamahl, Fist of Krosa.
 *
 * Kamahl, Fist of Krosa: {4}{G}{G} 4/3 Legendary Creature — Human Druid
 * {G}: Target land becomes a 1/1 creature until end of turn. It's still a land.
 * {2}{G}{G}{G}: Creatures you control get +3/+3 and gain trample until end of turn.
 */
class KamahlFistOfKrosaScenarioTest : ScenarioTestBase() {

    init {
        context("Kamahl animate land ability") {

            test("animated land becomes a 1/1 creature and can attack") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Kamahl, Fist of Krosa")
                    .withCardOnBattlefield(1, "Plains") // land to animate (not used for mana)
                    .withLandsOnBattlefield(1, "Forest", 1) // 1 to pay {G} cost
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kamahlId = game.findPermanent("Kamahl, Fist of Krosa")!!
                val forestToAnimate = game.findPermanent("Plains")!!

                // Get the first activated ability ({G}: Target land becomes a 1/1 creature)
                val cardDef = cardRegistry.getCard("Kamahl, Fist of Krosa")!!
                val animateAbility = cardDef.script.activatedAbilities[0]

                // Activate the ability targeting a Forest
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kamahlId,
                        abilityId = animateAbility.id,
                        targets = listOf(ChosenTarget.Permanent(forestToAnimate))
                    )
                )
                withClue("Animate land activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Verify the animated land shows as a creature in client state
                val clientState = game.getClientState(1)
                val animatedForest = clientState.cards[forestToAnimate]
                withClue("Animated forest should exist in client state") {
                    animatedForest shouldNotBe null
                }
                withClue("Animated forest should have CREATURE type") {
                    animatedForest!!.cardTypes.contains("CREATURE") shouldBe true
                }
                withClue("Animated forest should still have LAND type") {
                    animatedForest!!.cardTypes.contains("LAND") shouldBe true
                }
                withClue("Animated forest should be 1/1") {
                    animatedForest!!.power shouldBe 1
                    animatedForest!!.toughness shouldBe 1
                }

                // Advance to combat - the animated land should be a valid attacker
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Declare the animated land as an attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(forestToAnimate to game.player2Id))
                )
                withClue("Animated land should be able to attack: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Let combat resolve - no blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Opponent should have taken 1 damage from the animated land
                game.getLifeTotal(2) shouldBe 19
            }

            test("animated land can block") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(2, "Kamahl, Fist of Krosa")
                    .withCardOnBattlefield(2, "Plains") // land to animate
                    .withLandsOnBattlefield(2, "Forest", 1) // 1 to pay {G} cost
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 attacker
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kamahlId = game.findPermanent("Kamahl, Fist of Krosa")!!
                val forestToAnimate = game.findPermanent("Plains")!!

                val cardDef = cardRegistry.getCard("Kamahl, Fist of Krosa")!!
                val animateAbility = cardDef.script.activatedAbilities[0]

                // P1 passes priority to P2
                game.execute(PassPriority(game.player1Id))

                // P2 activates animate ability on the Forest
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = kamahlId,
                        abilityId = animateAbility.id,
                        targets = listOf(ChosenTarget.Permanent(forestToAnimate))
                    )
                )
                withClue("Animate land activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // P1 declares Grizzly Bears as attacker
                val bearsId = game.findPermanent("Grizzly Bears")!!
                game.execute(DeclareAttackers(game.player1Id, mapOf(bearsId to game.player2Id)))

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // P2 blocks with the animated Forest
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(forestToAnimate to listOf(bearsId)))
                )
                withClue("Animated land should be able to block: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("overrun ability gives +3/+3 and trample to all creatures") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Kamahl, Fist of Krosa")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Forest", 5) // {2}{G}{G}{G}
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kamahlId = game.findPermanent("Kamahl, Fist of Krosa")!!

                val cardDef = cardRegistry.getCard("Kamahl, Fist of Krosa")!!
                val overrunAbility = cardDef.script.activatedAbilities[1]

                // Activate the overrun ability
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kamahlId,
                        abilityId = overrunAbility.id,
                        targets = emptyList()
                    )
                )
                withClue("Overrun activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Verify stats: Kamahl should be 7/6 (4+3/3+3), Bears should be 5/5 (2+3/2+3)
                val clientState = game.getClientState(1)
                val kamahl = clientState.cards[kamahlId]
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val bears = clientState.cards[bearsId]

                withClue("Kamahl should be 7/6 after overrun") {
                    kamahl!!.power shouldBe 7
                    kamahl.toughness shouldBe 6
                }
                withClue("Grizzly Bears should be 5/5 after overrun") {
                    bears!!.power shouldBe 5
                    bears.toughness shouldBe 5
                }

                // Verify trample
                withClue("Kamahl should have trample") {
                    kamahl!!.keywords.contains(Keyword.TRAMPLE) shouldBe true
                }
                withClue("Grizzly Bears should have trample") {
                    bears!!.keywords.contains(Keyword.TRAMPLE) shouldBe true
                }
            }

            test("overrun ability also buffs animated lands") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Kamahl, Fist of Krosa")
                    .withCardOnBattlefield(1, "Plains") // land to animate
                    .withLandsOnBattlefield(1, "Forest", 6) // 1 for animate + 5 for overrun
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kamahlId = game.findPermanent("Kamahl, Fist of Krosa")!!
                val plainsId = game.findPermanent("Plains")!!

                val cardDef = cardRegistry.getCard("Kamahl, Fist of Krosa")!!
                val animateAbility = cardDef.script.activatedAbilities[0]
                val overrunAbility = cardDef.script.activatedAbilities[1]

                // First, animate the Plains
                val animateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kamahlId,
                        abilityId = animateAbility.id,
                        targets = listOf(ChosenTarget.Permanent(plainsId))
                    )
                )
                withClue("Animate should succeed: ${animateResult.error}") {
                    animateResult.error shouldBe null
                }
                game.resolveStack()

                // Now activate overrun
                val overrunResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kamahlId,
                        abilityId = overrunAbility.id,
                        targets = emptyList()
                    )
                )
                withClue("Overrun should succeed: ${overrunResult.error}") {
                    overrunResult.error shouldBe null
                }
                game.resolveStack()

                // The animated Plains should now be 4/4 (1+3/1+3) with trample
                val clientState = game.getClientState(1)
                val plains = clientState.cards[plainsId]
withClue("Animated Plains should be 4/4 after overrun") {
                    plains!!.power shouldBe 4
                    plains.toughness shouldBe 4
                }
                withClue("Animated Plains should have trample") {
                    plains!!.keywords.contains(Keyword.TRAMPLE) shouldBe true
                }
            }
        }
    }
}
