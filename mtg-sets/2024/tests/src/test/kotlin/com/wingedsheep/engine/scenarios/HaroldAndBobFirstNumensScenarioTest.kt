package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class HaroldAndBobFirstNumensScenarioTest : ScenarioTestBase() {
    private val harold = "Harold and Bob, First Numens"

    private fun TestGame.killHarold(): TestGame {
        castSpell(1, "Murder", findPermanent(harold)!!).error shouldBe null
        resolveStack()
        return this
    }

    init {
        test("dies, then returns as a legendary Aura on the Forest you choose, losing reach and vigilance") {
            val g = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, harold)
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(2, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val haroldId = g.findPermanent(harold)!!
            val myForests = g.findPermanents("Forest").filter { g.state.projectedState.getController(it) == g.player1Id }
            g.killHarold()

            val decision = g.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.legalTargets[0]!! shouldContainExactlyInAnyOrder myForests
            g.selectTargets(listOf(myForests[1])).error shouldBe null

            val projected = g.state.projectedState
            g.state.getEntity(haroldId)!!.get<AttachedToComponent>()!!.targetId shouldBe myForests[1]
            projected.hasType(haroldId, "ENCHANTMENT") shouldBe true
            projected.isCreature(haroldId) shouldBe false
            projected.getSubtypes(haroldId) shouldBe setOf("Aura")
            projected.getSupertypes(haroldId) shouldBe setOf("LEGENDARY")
            projected.getColors(haroldId) shouldBe setOf("GREEN")
            projected.getKeywords(haroldId).isEmpty() shouldBe true
        }

        test("the enchanted Forest taps for three mana of one color and gives you two rad counters") {
            val g = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, harold)
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val forest = g.findPermanent("Forest")!!
            g.killHarold()
            g.selectTargets(listOf(forest)).error shouldBe null

            val haroldId = g.findPermanent(harold)!!
            val grantedId = (g.state.grantedStaticAbilities.single { it.entityId == haroldId }.ability
                as GrantActivatedAbility).ability.id
            val action = g.getLegalActions(1).mapNotNull { it.action as? ActivateAbility }
                .single { it.sourceId == forest && it.abilityId == grantedId }
            // Murder's auto-payment may have tapped the Forest; untap it for the activation.
            g.state = g.state.updateEntity(forest) { it.without<TappedComponent>() }
            g.execute(action).error shouldBe null
            val color = g.getPendingDecision()
            if (color is ChooseColorDecision) g.submitDecision(ColorChosenResponse(color.id, Color.RED)).error shouldBe null
            g.state.getEntity(g.player1Id)!!.get<ManaPoolComponent>()!!.red shouldBe 3
            g.state.getEntity(g.player1Id)!!.get<CountersComponent>()!!.getCount(CounterType.RAD) shouldBe 2
        }

        test("with no Forest you control, it stays in the graveyard") {
            val g = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, harold)
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(2, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            g.killHarold()
            g.hasPendingDecision() shouldBe false
            g.isInGraveyard(1, harold) shouldBe true
        }
        test("when the enchanted Forest changes control, Harold and Bob is put into the graveyard (CR 704.5m)") {
            val g = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, harold)
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val forest = g.findPermanent("Forest")!!
            g.killHarold()
            g.selectTargets(listOf(forest)).error shouldBe null
            g.checkStateBasedActions()
            g.isOnBattlefield(harold) shouldBe true

            // "Enchant Forest you control": an opponent's Forest is no longer a legal host.
            g.state = g.state.updateEntity(forest) { it.with(ControllerComponent(g.player2Id)) }
            g.checkStateBasedActions()
            g.isInGraveyard(1, harold) shouldBe true
        }
    }
}
