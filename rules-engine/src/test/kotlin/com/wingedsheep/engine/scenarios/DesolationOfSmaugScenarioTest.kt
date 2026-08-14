package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Desolation of Smaug (HOB #93) — {2}{R}{R} Sorcery
 *
 * Desolation of Smaug deals 3 damage to each non-Dragon creature.
 * Add four mana in any combination of colors. Spend this mana only to cast Dragon spells.
 *
 * The exclusion is the part that fails silently — a sweep whose filter forgets the "non-Dragon"
 * clause still looks correct on a board with no Dragons. The mana half is checked for arriving at
 * all (four pips, independently colored) rather than for its restriction, which
 * `ManaRestriction.SubtypeSpellsOnly` already owns.
 */
class DesolationOfSmaugScenarioTest : ScenarioTestBase() {

    /**
     * Resolve the spell and answer the four "colour this pip" prompts with [colors]. Stops as soon
     * as the prompts are done — passing priority any further would end the step and empty the pool
     * (CR 500.4), which is exactly what the mana test needs to read before.
     */
    private fun resolveAndColorMana(game: TestGame, colors: List<Color>) {
        game.resolveStack()
        var i = 0
        var guard = 0
        while (game.hasPendingDecision() && guard++ < 12) {
            game.submitDecision(
                ColorChosenResponse(game.getPendingDecision()!!.id, colors.getOrElse(i++) { Color.RED })
            )
        }
    }

    init {
        context("Desolation of Smaug") {

            test("the sweep spares Dragons and kills everything else that can't take 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Desolation of Smaug")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Smaug, the Great Calamity")
                    .withCardOnBattlefield(2, "Goblin-town Flunkies")
                    .withCardOnBattlefield(2, "Ordinary Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Desolation of Smaug").error shouldBe null
                resolveAndColorMana(game, List(4) { Color.RED })
                game.checkStateBasedActions()

                withClue("Smaug is a Dragon — untouched by its own desolation") {
                    game.isOnBattlefield("Smaug, the Great Calamity") shouldBe true
                }
                withClue("A 1/1 non-Dragon takes 3 and dies") {
                    game.isInGraveyard(2, "Goblin-town Flunkies") shouldBe true
                }
                withClue("A 4/5 non-Dragon takes 3 and survives — damage, not destruction") {
                    game.isOnBattlefield("Ordinary Bear") shouldBe true
                }
            }

            test("it adds four Dragons-only mana, each pip colored independently") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Desolation of Smaug")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Desolation of Smaug").error shouldBe null
                resolveAndColorMana(game, listOf(Color.RED, Color.RED, Color.BLUE, Color.GREEN))

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                val restricted = pool?.restrictedMana.orEmpty()

                withClue("Restricted mana never lands in the plain colour counters") {
                    pool?.red shouldBe 0
                }
                withClue("Four mana in any combination — two red, one blue, one green: $restricted") {
                    restricted.count { it.color == Color.RED } shouldBe 2
                    restricted.count { it.color == Color.BLUE } shouldBe 1
                    restricted.count { it.color == Color.GREEN } shouldBe 1
                }
                withClue("Every pip carries the Dragon-spells-only restriction") {
                    restricted.all {
                        it.restriction == ManaRestriction.SubtypeSpellsOnly(setOf("Dragon"))
                    } shouldBe true
                }
            }
        }
    }
}
