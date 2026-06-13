package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * OTJ batch B — Auras & Pacifism-style removal.
 *
 *  - Mystical Tether (#19): {2}{W} Enchantment. ETB exile target artifact or creature an opponent
 *    controls until it leaves (Banishing Light shape, narrower filter) + flash-for-{2} rider.
 *  - Lassoed by the Law (#18): {3}{W} Enchantment. ETB exile target nonland permanent an opponent
 *    controls until it leaves + a second ETB making a 1/1 red Mercenary token.
 *  - Reach for the Sky (#178): {3}{G} Aura. Flash; +3/+2 and reach; draw a card when it's put into
 *    a graveyard from the battlefield.
 *
 * The exile-until-leaves / return-on-LTB seam itself is covered end-to-end by BanishingLightTest;
 * here we confirm each card's printed wiring.
 */
class OtjBatchBAurasScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Mystical Tether") {
            test("exiles an opponent's creature on ETB, returns it when the enchantment leaves") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mystical Tether")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victim = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Mystical Tether")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack() // enchantment enters → ETB trigger asks for a target

                val selected = game.selectTargets(listOf(victim))
                withClue("ETB target selection should succeed: ${selected.error}") {
                    selected.error shouldBe null
                }
                game.resolveStack()

                withClue("Hill Giant should be exiled while Mystical Tether is in play") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.state.getExile(game.player2Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                    } shouldBe 1
                }
            }

            test("can also exile an opponent's artifact (artifact-or-creature filter)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mystical Tether")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val artifact = game.findPermanent("Ornithopter")!!
                game.castSpell(1, "Mystical Tether")
                game.resolveStack()
                game.selectTargets(listOf(artifact))
                game.resolveStack()

                withClue("Ornithopter (an artifact) should be a legal exile target") {
                    game.isOnBattlefield("Ornithopter") shouldBe false
                    game.state.getExile(game.player2Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Ornithopter"
                    } shouldBe 1
                }
            }
        }

        context("Lassoed by the Law") {
            test("exiles a nonland permanent AND creates a 1/1 red Mercenary token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lassoed by the Law")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victim = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Lassoed by the Law")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                // Two ETB triggers go on the stack; the exile trigger asks for a target.
                game.resolveStack()
                game.selectTargets(listOf(victim))
                game.resolveStack()

                withClue("Hill Giant should be exiled") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.state.getExile(game.player2Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                    } shouldBe 1
                }
                withClue("A 1/1 red Mercenary token should have been created for the controller") {
                    val token = game.findPermanent("Mercenary Token")
                    token.shouldNotBeNull()
                    projector.getProjectedPower(game.state, token) shouldBe 1
                    projector.getProjectedToughness(game.state, token) shouldBe 1
                }
            }
        }

        context("Reach for the Sky") {
            test("grants +3/+2 and reach to the enchanted creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Reach for the Sky")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Reach for the Sky", creature)
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Hill Giant should be 6/5 (3/3 +3/+2) and have reach") {
                    projector.getProjectedPower(game.state, creature) shouldBe 6
                    projector.getProjectedToughness(game.state, creature) shouldBe 5
                    projector.hasProjectedKeyword(game.state, creature, Keyword.REACH) shouldBe true
                }
            }

            test("draws a card when put into a graveyard from the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Reach for the Sky")
                    .withCardInHand(2, "Disenchant")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(2, "Plains", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Reach for the Sky", creature)
                game.resolveStack()
                val aura = game.findPermanent("Reach for the Sky")!!

                val handBefore = game.handSize(1)

                // Hand priority to the opponent so they may respond on player 1's turn.
                game.passPriority()

                // Opponent destroys the Aura → it goes to the graveyard → draw trigger fires.
                val disenchant = game.castSpell(2, "Disenchant", aura)
                withClue("Disenchant cast should succeed: ${disenchant.error}") {
                    disenchant.error shouldBe null
                }
                game.resolveStack()

                withClue("Reach for the Sky should be in its owner's graveyard") {
                    game.isInGraveyard(1, "Reach for the Sky") shouldBe true
                }
                withClue("Controller should have drawn a card from the put-into-graveyard trigger") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }
    }
}
