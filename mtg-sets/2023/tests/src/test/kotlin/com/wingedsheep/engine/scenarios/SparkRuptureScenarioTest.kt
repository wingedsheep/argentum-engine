package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dmu.cards.AjaniSleeperAgent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spark Rupture (MAT #5) — {2}{W} Enchantment.
 *
 * "When this enchantment enters, draw a card.
 *  Each planeswalker with one or more loyalty counters on it loses all abilities and is a creature
 *  with power and toughness each equal to the number of loyalty counters on it."
 *
 * The novel part is a multi-layer group static whose filter (planeswalker) is removed by its own
 * Layer-4 part: the CR 613.6 locked set must keep the permanent sized and stripped afterwards.
 */
class SparkRuptureScenarioTest : ScenarioTestBase() {

    private val ajaniPlusOne = AjaniSleeperAgent.activatedAbilities[0].id

    init {
        test("a planeswalker becomes a vanilla creature sized by its loyalty and is no longer a planeswalker") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Spark Rupture")
                .withCardOnBattlefield(1, "Ajani, Sleeper Agent")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ajani = game.findPermanent("Ajani, Sleeper Agent")!!
            val handBefore = game.handSize(1)

            val cast = game.castSpell(1, "Spark Rupture")
            withClue("casting Spark Rupture: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("the enters trigger draws a card (cast one, drew one)") {
                game.handSize(1) shouldBe handBefore
            }

            val projected = game.state.projectedState
            projected.isCreature(ajani) shouldBe true
            projected.isPlaneswalker(ajani) shouldBe false
            withClue("P/T equals its four loyalty counters") {
                projected.getPower(ajani) shouldBe 4
                projected.getToughness(ajani) shouldBe 4
            }

            withClue("it lost all abilities, loyalty abilities included") {
                game.execute(ActivateAbility(game.player1Id, ajani, ajaniPlusOne)).error shouldNotBe null
            }
        }

        test("with no loyalty counters the effect stops applying and it is a planeswalker again") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Spark Rupture")
                .withCardOnBattlefield(1, "Ajani, Sleeper Agent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ajani = game.findPermanent("Ajani, Sleeper Agent")!!
            game.state.projectedState.isCreature(ajani) shouldBe true

            game.state = game.state.updateEntity(ajani) { c -> c.with(CountersComponent()) }

            val projected = game.state.projectedState
            projected.isCreature(ajani) shouldBe false
            projected.isPlaneswalker(ajani) shouldBe true
        }
    }
}
