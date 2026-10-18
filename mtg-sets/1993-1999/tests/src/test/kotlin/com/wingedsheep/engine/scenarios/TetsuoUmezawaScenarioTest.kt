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
 * Tetsuo Umezawa — {U}{B}{R} 3/3 Legendary Creature — Human Archer.
 *  - Tetsuo Umezawa can't be the target of Aura spells.
 *  - {U}{B}{B}{R}, {T}: Destroy target tapped or blocking creature.
 *
 * Rulings pinned: Aura spells can't target him — his own controller's included — but an Aura that
 * isn't cast (attached directly) may still enchant him, and a non-Aura spell can target him.
 */
class TetsuoUmezawaScenarioTest : ScenarioTestBase() {

    private val tetsuoAbilityId by lazy {
        cardRegistry.getCard("Tetsuo Umezawa")!!.activatedAbilities[0].id
    }

    init {
        context("Tetsuo Umezawa — can't be the target of Aura spells") {
            test("neither player's Aura spell can target him; an Aura can still target another creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tetsuo Umezawa")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Giant Strength")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tetsuo = game.findPermanent("Tetsuo Umezawa")!!
                withClue("His controller's Aura spell can't target him") {
                    game.castSpell(1, "Giant Strength", tetsuo).error shouldNotBe null
                }
                val bears = game.findPermanent("Grizzly Bears")!!
                val onBears = game.castSpell(1, "Giant Strength", bears)
                withClue("The Aura can target another creature: ${onBears.error}") { onBears.error shouldBe null }
            }

            test("a non-Aura spell can target him") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Tetsuo Umezawa")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tetsuo = game.findPermanent("Tetsuo Umezawa")!!
                val bolt = game.castSpell(1, "Lightning Bolt", tetsuo)
                withClue("Lightning Bolt can target Tetsuo: ${bolt.error}") { bolt.error shouldBe null }
                game.resolveStack()
                game.isInGraveyard(2, "Tetsuo Umezawa") shouldBe true
            }

            test("an Aura that isn't cast can still be attached to him") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tetsuo Umezawa")
                    .withCardAttachedTo(1, "Giant Strength", "Tetsuo Umezawa")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tetsuo = game.findPermanent("Tetsuo Umezawa")!!
                game.passPriority()
                game.isOnBattlefield("Giant Strength") shouldBe true
                game.state.projectedState.getPower(tetsuo) shouldBe 5
            }
        }

        context("Tetsuo Umezawa — destroy target tapped or blocking creature") {
            test("destroys a tapped creature; an untapped, non-blocking creature isn't a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tetsuo Umezawa")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tetsuo = game.findPermanent("Tetsuo Umezawa")!!
                val giant = game.findPermanent("Hill Giant")!!
                val illegal = game.execute(
                    ActivateAbility(game.player1Id, tetsuo, tetsuoAbilityId, listOf(ChosenTarget.Permanent(giant)))
                )
                withClue("An untapped, non-blocking creature isn't a legal target") { illegal.error shouldNotBe null }

                val bears = game.findPermanent("Grizzly Bears")!!
                val legal = game.execute(
                    ActivateAbility(game.player1Id, tetsuo, tetsuoAbilityId, listOf(ChosenTarget.Permanent(bears)))
                )
                withClue("A tapped creature is a legal target: ${legal.error}") { legal.error shouldBe null }
                game.resolveStack()
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }

            test("destroys an untapped creature that is blocking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tetsuo Umezawa")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
                game.passPriority() // the defender passes; the attacker gets priority in the same step

                val tetsuo = game.findPermanent("Tetsuo Umezawa")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.execute(
                    ActivateAbility(game.player1Id, tetsuo, tetsuoAbilityId, listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
        }
    }
}
