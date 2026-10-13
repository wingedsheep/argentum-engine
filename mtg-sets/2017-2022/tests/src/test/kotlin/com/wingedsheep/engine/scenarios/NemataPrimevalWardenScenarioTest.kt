package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dmu.cards.NemataPrimevalWarden
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Nemata, Primeval Warden (DMU #209) — {2}{B}{G} 3/4 reach.
 *   If a creature an opponent controls would die, exile it instead. When you do, create a 1/1
 *   green Saproling creature token.
 *   {G}, Sacrifice a Saproling: Nemata gets +2/+2 until end of turn.
 *   {1}{B}, Sacrifice two Saprolings: Draw a card.
 *
 * "When you do" is a reflexive trigger (CR 603.12): the Saproling is not part of the replacement,
 * it goes on the stack after the exile and can be responded to.
 */
class NemataPrimevalWardenScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        val pumpId = NemataPrimevalWarden.activatedAbilities[0].id
        val drawId = NemataPrimevalWarden.activatedAbilities[1].id

        test("an opponent's dying creature is exiled, and the Saproling arrives as a trigger on the stack") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nemata, Primeval Warden")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
            // Resolve only the Bolt: both players pass once.
            game.passPriority()
            game.passPriority()

            withClue("The Bear is exiled instead of dying") {
                game.state.getGraveyard(game.player2Id) shouldNotContain bears
                game.state.getExile(game.player2Id) shouldContain bears
            }
            withClue("The Saproling is not minted inline — its reflexive trigger is on the stack") {
                game.findAllPermanents("Saproling Token").size shouldBe 0
                game.state.stack.size shouldBe 1
            }

            game.resolveStack()
            val saprolings = game.findAllPermanents("Saproling Token")
            saprolings.size shouldBe 1
            game.state.getBattlefield(game.player1Id) shouldContain saprolings.single()
        }

        test("your own creatures die normally and mint nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nemata, Primeval Warden")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
            game.resolveStack()

            game.state.getGraveyard(game.player1Id) shouldContain bears
            game.findAllPermanents("Saproling Token").size shouldBe 0
        }

        test("sacrificing Saprolings pumps Nemata and draws a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nemata, Primeval Warden")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardsInHand(1, "Lightning Bolt", 3)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val nemata = game.findPermanent("Nemata, Primeval Warden")!!

            game.findPermanents("Grizzly Bears").forEach { bear ->
                game.castSpell(1, "Lightning Bolt", targetId = bear).error shouldBe null
                game.resolveStack()
            }
            val saprolings = game.findAllPermanents("Saproling Token")
            saprolings.size shouldBe 3

            val pump = game.execute(
                ActivateAbility(
                    game.player1Id, nemata, pumpId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(saprolings[0])),
                )
            )
            withClue("${pump.error}") { pump.error shouldBe null }
            game.resolveStack()
            projector.getProjectedPower(game.state, nemata) shouldBe 5
            projector.getProjectedToughness(game.state, nemata) shouldBe 6

            val handBefore = game.state.getHand(game.player1Id).size
            val draw = game.execute(
                ActivateAbility(
                    game.player1Id, nemata, drawId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = saprolings.drop(1)),
                )
            )
            withClue("${draw.error}") { draw.error shouldBe null }
            game.resolveStack()
            game.state.getHand(game.player1Id).size shouldBe handBefore + 1
            game.findAllPermanents("Saproling Token").size shouldBe 0
        }
    }
}
