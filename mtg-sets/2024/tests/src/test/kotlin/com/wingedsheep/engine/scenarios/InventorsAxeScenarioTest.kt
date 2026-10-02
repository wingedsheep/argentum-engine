package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.InventorsAxe
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Inventor's Axe (MH3 #126) — {R} Artifact — Equipment.
 *
 * "Flash / When this Equipment enters, you get {E}{E}. / When this Equipment enters, attach it to
 * target creature you control. / Equipped creature gets +2/+0. / Equip—Pay {E}{E}."
 */
class InventorsAxeScenarioTest : ScenarioTestBase() {

    private val equip = InventorsAxe.activatedAbilities.single { it.isEquipAbility }.id

    private fun TestGame.energy(): Int =
        state.getEntity(player1Id)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    private fun TestGame.attachedTo(): EntityId? =
        state.getEntity(findPermanent("Inventor's Axe")!!)?.get<AttachedToComponent>()?.targetId

    /** Cast the Axe, resolve it and both enters triggers, aiming the attach trigger at [host]. */
    private fun TestGame.castAxe(host: EntityId?) {
        castSpell(1, "Inventor's Axe").error shouldBe null
        var guard = 0
        while (guard++ < 30) {
            when (val decision = getPendingDecision()) {
                is OrderObjectsDecision ->
                    submitDecision(OrderedResponse(decision.id, decision.objects)).error shouldBe null
                is ChooseTargetsDecision ->
                    submitDecision(TargetsResponse(decision.id, mapOf(0 to listOfNotNull(host)))).error shouldBe null
                null -> if (state.stack.isEmpty()) return else resolveStack()
                else -> error("Unexpected decision $decision")
            }
        }
        error("Inventor's Axe never finished resolving")
    }

    init {
        context("Inventor's Axe") {

            test("flash: cast on the opponent's turn, it gives {E}{E} and attaches for +2/+0") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Inventor's Axe")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castAxe(bears)

                game.energy() shouldBe 2
                game.attachedTo() shouldBe bears
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 2
            }

            test("with no creature to attach to, the energy still arrives and the Axe stays unattached") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Inventor's Axe")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castAxe(null)

                game.energy() shouldBe 2
                game.attachedTo() shouldBe null
            }

            test("Equip—Pay {E}{E} moves it, spending the energy; with none left it can't equip again") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Inventor's Axe")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castAxe(bears)
                game.energy() shouldBe 2
                val axe = game.findPermanent("Inventor's Axe")!!

                game.execute(
                    ActivateAbility(game.player1Id, axe, equip, targets = listOf(ChosenTarget.Permanent(giant)))
                ).error shouldBe null
                game.resolveStack()

                withClue("equipping paid both energy counters") { game.energy() shouldBe 0 }
                game.attachedTo() shouldBe giant
                game.state.projectedState.getPower(giant) shouldBe 5
                game.state.projectedState.getPower(bears) shouldBe 2

                withClue("no energy left, so the equip cost can't be paid") {
                    game.execute(
                        ActivateAbility(game.player1Id, axe, equip, targets = listOf(ChosenTarget.Permanent(bears)))
                    ).error shouldNotBe null
                }
            }
        }
    }
}
