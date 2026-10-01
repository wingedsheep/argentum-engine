package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class JadeMonolithScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario().withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Jade Monolith")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Forest")
            .withCardOnBattlefield(1, "Forest")
            .withCardOnBattlefield(2, "Mountain")
            .withCardOnBattlefield(2, "Mountain")
            .withCardInHand(2, "Lightning Bolt")
            .withCardInHand(2, "Lightning Bolt")
            .withCardInHand(2, "Disenchant")
            .withCardOnBattlefield(2, "Plains")
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(2).withPriorityPlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        fun protect(game: TestGame) {
            val bear = game.findPermanent("Grizzly Bears")!!
            game.execute(ActivateAbility(game.player1Id, game.findPermanent("Jade Monolith")!!,
                cardRegistry.getCard("Jade Monolith")!!.script.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(bear)))).error shouldBe null
            game.resolveStack()
        }
        fun chooseBolt(game: TestGame, bolt: com.wingedsheep.sdk.model.EntityId) {
            when (val decision = game.getPendingDecision()) {
                is SelectCardsDecision -> game.selectCards(listOf(bolt)).error shouldBe null
                is ChooseOptionDecision -> game.submitDecision(OptionChosenResponse(decision.id,
                    decision.options.indexOf("Lightning Bolt"))).error shouldBe null
                else -> error("Expected a damage source choice")
            }
        }
        test("chooses a spell during resolution and redirects that spell once") {
            val game = board(); val bear = game.findPermanent("Grizzly Bears")!!
            game.castSpell(2, "Lightning Bolt", bear).error shouldBe null
            val bolt = game.state.stack.last()
            game.passPriority().error shouldBe null
            protect(game)
            (game.getPendingDecision() as SelectCardsDecision).options.contains(bolt) shouldBe true
            chooseBolt(game, bolt)
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
            game.findPermanent("Grizzly Bears") shouldBe bear
            game.state.floatingEffects.none { it.effect.modification is SerializableModification.RedirectNextDamage } shouldBe true
            game.castSpell(2, "Lightning Bolt", bear).error shouldBe null
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldBe null
            game.getLifeTotal(1) shouldBe 17
        }
        test("ability survives destruction of Jade Monolith before it resolves") {
            val game = board(); val bear = game.findPermanent("Grizzly Bears")!!
            game.castSpell(2, "Lightning Bolt", bear).error shouldBe null
            val bolt = game.state.stack.last(); game.passPriority().error shouldBe null
            game.execute(ActivateAbility(game.player1Id, game.findPermanent("Jade Monolith")!!,
                cardRegistry.getCard("Jade Monolith")!!.script.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(bear)))).error shouldBe null
            game.passPriority().error shouldBe null
            game.castSpell(2, "Disenchant", game.findPermanent("Jade Monolith")!!).error shouldBe null
            game.resolveStack()
            game.findPermanent("Jade Monolith") shouldBe null
            chooseBolt(game, bolt)
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
            game.findPermanent("Grizzly Bears") shouldBe bear
        }
        test("illegal creature target fizzles without asking for a source") {
            val game = board(); val bear = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.copy(priorityPlayerId = game.player1Id)
            game.execute(ActivateAbility(game.player1Id, game.findPermanent("Jade Monolith")!!,
                cardRegistry.getCard("Jade Monolith")!!.script.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(bear)))).error shouldBe null
            game.passPriority().error shouldBe null
            game.castSpell(2, "Lightning Bolt", bear).error shouldBe null
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldBe null
            game.hasPendingDecision() shouldBe false
            game.state.floatingEffects.isEmpty() shouldBe true
        }
        test("chosen attacker combat damage is redirected while the blocker survives") {
            val game = scenario().withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Jade Monolith").withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Forest").withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(2).withPriorityPlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS).build()
            game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
            game.state = game.state.copy(priorityPlayerId = game.player1Id)
            protect(game)
            game.selectCards(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(1) shouldBe 17
            game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.findPermanent("Hill Giant").shouldNotBeNull()
        }
        test("can protect an opponent creature and sends damage to the activating player") {
            val game = board(); val bear = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.removeFromZone(com.wingedsheep.engine.state.ZoneKey(game.player1Id, com.wingedsheep.sdk.core.Zone.BATTLEFIELD), bear)
                .addToZone(com.wingedsheep.engine.state.ZoneKey(game.player2Id, com.wingedsheep.sdk.core.Zone.BATTLEFIELD), bear)
                .updateEntity(bear) { it.with(com.wingedsheep.engine.state.components.identity.ControllerComponent(game.player2Id)) }
                .copy(priorityPlayerId = game.player1Id)
            protect(game)
            game.selectCards(listOf(game.findPermanent("Mountain")!!)).error shouldBe null
            val mod = game.state.floatingEffects.single().effect.modification as SerializableModification.RedirectNextDamage
            mod.redirectToId shouldBe game.player1Id
            game.state.floatingEffects.single().effect.affectedEntities shouldBe setOf(bear)
        }
    }
}
