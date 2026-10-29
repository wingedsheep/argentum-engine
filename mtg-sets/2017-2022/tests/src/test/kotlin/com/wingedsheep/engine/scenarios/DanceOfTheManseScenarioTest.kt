package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dance of the Manse (ELD #186, {X}{W}{U}, Sorcery).
 *
 *   Return up to X target artifact and/or non-Aura enchantment cards each with mana value X or
 *   less from your graveyard to the battlefield. If X is 6 or more, those permanents are 4/4
 *   creatures in addition to their other types.
 *
 * Pins the X-linked pieces: the target cap and the mana-value cap both read the announced X, and
 * the 4/4 animation applies only when X is 6 or more.
 */
class DanceOfTheManseScenarioTest : ScenarioTestBase() {

    private fun board(lands: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Dance of the Manse")
        .withCardInGraveyard(1, "Sol Ring")          // artifact, MV 1
        .withCardInGraveyard(1, "Seal of Cleansing") // non-Aura enchantment, MV 1
        .withCardInGraveyard(1, "Mind Stone")        // artifact, MV 2
        .withCardInGraveyard(1, "Darksteel Ingot")   // artifact, MV 3
        .withCardInGraveyard(1, "Pacifism")          // Aura, MV 2
        .withLandsOnBattlefield(1, "Plains", lands - 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.gy(name: String) =
        ChosenTarget.Card(findCardsInGraveyard(1, name).single(), player1Id, Zone.GRAVEYARD)

    private fun TestGame.cast(x: Int, vararg names: String) =
        execute(
            CastSpell(
                player1Id,
                findCardsInHand(1, "Dance of the Manse").single(),
                targets = names.map { gy(it) },
                xValue = x,
            )
        )

    init {
        context("Dance of the Manse") {

            test("X = 2 returns up to two artifact/enchantment cards as noncreatures") {
                val game = board(lands = 4)
                game.cast(2, "Sol Ring", "Seal of Cleansing").error shouldBe null
                game.resolveStack()

                val ring = game.findPermanent("Sol Ring")
                val seal = game.findPermanent("Seal of Cleansing")
                ring shouldNotBe null
                seal shouldNotBe null
                withClue("below X = 6 they stay noncreature permanents") {
                    game.state.projectedState.isCreature(ring!!) shouldBe false
                    game.state.projectedState.isCreature(seal!!) shouldBe false
                }
                game.isInGraveyard(1, "Mind Stone") shouldBe true
            }

            test("more targets than X are rejected") {
                val game = board(lands = 4)
                game.cast(2, "Sol Ring", "Seal of Cleansing", "Mind Stone").error shouldNotBe null
            }

            test("a card with mana value above X can't be targeted") {
                val game = board(lands = 4)
                game.cast(2, "Darksteel Ingot").error shouldNotBe null
            }

            test("an Aura card can't be targeted") {
                val game = board(lands = 4)
                game.cast(2, "Pacifism").error shouldNotBe null
            }

            test("X = 6 returns them as 4/4 creatures in addition to their other types") {
                val game = board(lands = 8)
                game.cast(6, "Sol Ring", "Seal of Cleansing", "Darksteel Ingot").error shouldBe null
                game.resolveStack()

                for (name in listOf("Sol Ring", "Seal of Cleansing", "Darksteel Ingot")) {
                    val id = game.findPermanent(name)
                    withClue(name) {
                        id shouldNotBe null
                        val projected = game.state.projectedState
                        projected.isCreature(id!!) shouldBe true
                        projected.getPower(id) shouldBe 4
                        projected.getToughness(id) shouldBe 4
                    }
                }
                withClue("they keep their other card types") {
                    game.state.projectedState.hasType(game.findPermanent("Darksteel Ingot")!!, "ARTIFACT") shouldBe true
                    game.state.projectedState.hasType(game.findPermanent("Seal of Cleansing")!!, "ENCHANTMENT") shouldBe true
                }
            }
        }
    }
}
