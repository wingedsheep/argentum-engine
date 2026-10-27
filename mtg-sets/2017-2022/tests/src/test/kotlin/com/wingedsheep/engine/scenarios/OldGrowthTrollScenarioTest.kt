package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class OldGrowthTrollScenarioTest : ScenarioTestBase() {
    private val troll = "Old-Growth Troll"

    private fun TestGame.killTroll(): TestGame {
        castSpell(1, "Murder", findPermanent(troll)!!).error shouldBe null
        resolveStack()
        return this
    }

    private fun TestGame.grantedAction(forest: com.wingedsheep.sdk.model.EntityId, index: Int): ActivateAbility {
        val trollId = findPermanent(troll)!!
        val grantedId = (state.grantedStaticAbilities.filter { it.entityId == trollId }[index].ability
            as GrantActivatedAbility).ability.id
        return getLegalActions(1).mapNotNull { it.action as? ActivateAbility }
            .single { it.sourceId == forest && it.abilityId == grantedId }
    }

    private fun setup() = scenario().withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, troll)
        .withCardInHand(1, "Murder")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("dies, then returns as an Aura on a Forest you control, keeping its own abilities") {
            val g = setup()
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(2, "Forest", 1)
                .build()
            val trollId = g.findPermanent(troll)!!
            val myForests = g.findPermanents("Forest").filter { g.state.projectedState.getController(it) == g.player1Id }
            g.killTroll()

            val decision = g.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.legalTargets[0]!! shouldContainExactlyInAnyOrder myForests
            g.selectTargets(listOf(myForests[0])).error shouldBe null

            val projected = g.state.projectedState
            g.state.getEntity(trollId)!!.get<AttachedToComponent>()!!.targetId shouldBe myForests[0]
            projected.isCreature(trollId) shouldBe false
            projected.hasType(trollId, "ENCHANTMENT") shouldBe true
            projected.getSubtypes(trollId) shouldBe setOf("Aura")
            // Not "loses all other abilities": trample stays on the Aura.
            projected.hasKeyword(trollId, Keyword.TRAMPLE) shouldBe true
        }

        test("the enchanted Forest taps for {G}{G}") {
            val g = setup().withLandsOnBattlefield(1, "Forest", 1).build()
            val forest = g.findPermanent("Forest")!!
            g.killTroll()
            g.selectTargets(listOf(forest)).error shouldBe null

            g.state = g.state.updateEntity(forest) { it.without<TappedComponent>() }
            g.execute(g.grantedAction(forest, 0)).error shouldBe null
            g.state.getEntity(g.player1Id)!!.get<ManaPoolComponent>()!!.green shouldBe 2
        }

        test("sacrificing the Forest makes a tapped 4/4 trampling Troll Warrior; the Aura doesn't return") {
            val g = setup()
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .build()
            val forest = g.findPermanent("Forest")!!
            g.killTroll()
            g.selectTargets(listOf(forest)).error shouldBe null

            g.state = g.state.updateEntity(forest) { it.without<TappedComponent>() }
            g.execute(g.grantedAction(forest, 1)).error shouldBe null
            g.resolveStack()
            g.checkStateBasedActions()
            g.resolveStack()

            g.isOnBattlefield("Forest") shouldBe false
            g.isInGraveyard(1, troll) shouldBe true
            g.hasPendingDecision() shouldBe false
            val token = g.findPermanents("Troll Warrior Token").single()
            val projected = g.state.projectedState
            projected.getPower(token) shouldBe 4
            projected.getToughness(token) shouldBe 4
            projected.hasKeyword(token, Keyword.TRAMPLE) shouldBe true
            g.state.getEntity(token)!!.has<TappedComponent>() shouldBe true
        }

        test("with no Forest you control, it stays in the graveyard") {
            val g = setup().withLandsOnBattlefield(2, "Forest", 1).build()
            g.killTroll()
            g.hasPendingDecision() shouldBe false
            g.isInGraveyard(1, troll) shouldBe true
        }
    }
}
