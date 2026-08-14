package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mkm.cards.VengefulTracker
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vengeful Tracker (MKM) — {1}{R} Creature — Human Detective 2/2.
 * "Whenever an opponent sacrifices an artifact, this creature deals 2 damage to them."
 *
 * Exercises the `PermanentsSacrificedEvent(sacrificedBy = Player.EachOpponent)` scope added for this
 * card, on the three axes that scope can go wrong:
 *  - **Whose** sacrifice counts — an opponent's fires it; the Tracker controller's own does not.
 *  - **How many times** it fires — "an artifact" is the per-permanent template (CR 603.2c), so two
 *    artifacts sacrificed in one event fire it twice.
 *  - **Who takes the damage** — `Player.TriggeringPlayer` must resolve to the player who actually
 *    sacrificed, not to the Tracker's controller.
 * The artifact filter is checked too: sacrificing a creature must not fire it.
 */
class VengefulTrackerScenarioTest : ScenarioTestBase() {

    private val scrapPile = card("Scrap Pile") {
        manaCost = "{0}"
        typeLine = "Artifact"
    }

    // {0} sorceries that make their *caster* sacrifice their own permanents, so the sacrificing
    // player is whoever cast the spell.
    private fun selfSacrifice(name: String, filter: GameObjectFilter, count: Int) =
        card(name) {
            manaCost = "{0}"
            typeLine = "Sorcery"
            spell {
                effect = Effects.Sacrifice(filter, count, EffectTarget.PlayerRef(Player.You))
            }
        }

    private val scrapOne = selfSacrifice("Scrap One", GameObjectFilter.Artifact, 1)
    private val scrapTwo = selfSacrifice("Scrap Two", GameObjectFilter.Artifact, 2)
    private val cullOne = selfSacrifice("Cull One", GameObjectFilter.Creature, 1)

    private fun TestGame.resolveAll() {
        var guard = 0
        while ((state.stack.isNotEmpty() || hasPendingDecision()) && guard++ < 60) {
            when (val d = getPendingDecision()) {
                is SelectCardsDecision -> selectCards(d.options.take(d.minSelections))
                null -> resolveStack()
                else ->
                    if (d::class.simpleName?.contains("Mana") == true) submitManaSourcesAutoPay()
                    else error("Unexpected decision: ${d::class.simpleName}")
            }
        }
    }

    init {
        cardRegistry.register(VengefulTracker)
        cardRegistry.register(scrapPile)
        cardRegistry.register(scrapOne)
        cardRegistry.register(scrapTwo)
        cardRegistry.register(cullOne)

        context("Vengeful Tracker — 'whenever an opponent sacrifices an artifact'") {

            test("an opponent's artifact sacrifice deals 2 damage to that opponent") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Vengeful Tracker", summoningSickness = false)
                    .withCardOnBattlefield(2, "Scrap Pile")
                    .withCardInHand(2, "Scrap One")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Scrap One")
                game.resolveAll()

                withClue("the sacrificing opponent takes the 2 damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("the Tracker's controller is untouched") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("your own artifact sacrifice does not fire it") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Vengeful Tracker", summoningSickness = false)
                    .withCardOnBattlefield(1, "Scrap Pile")
                    .withCardInHand(1, "Scrap One")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Scrap One")
                game.resolveAll()

                withClue("'an opponent' excludes the Tracker's own controller") {
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("two artifacts sacrificed at once fire it twice (per-permanent, CR 603.2c)") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Vengeful Tracker", summoningSickness = false)
                    .withCardOnBattlefield(2, "Scrap Pile")
                    .withCardOnBattlefield(2, "Scrap Pile")
                    .withCardInHand(2, "Scrap Two")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Scrap Two")
                game.resolveAll()

                withClue("'an artifact' is per-permanent: two sacrifices, two triggers, 4 damage") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }

            test("sacrificing a creature does not fire it — the filter is artifacts") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Vengeful Tracker", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(2, "Cull One")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Cull One")
                game.resolveAll()

                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
