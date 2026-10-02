package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

class ForcefieldScenarioTest : ScenarioTestBase() {
    init {
        val duelist = card("Forcefield Test Duelist") {
            typeLine = "Creature — Human"; power = 3; toughness = 3
            keywords(Keyword.DOUBLE_STRIKE, Keyword.HEXPROOF)
        }
        cardRegistry.register(duelist)
        fun board(attacker: String = "Hill Giant", blocker: Boolean = false) = scenario()
            .withPlayers("Defender", "Attacker")
            .withCardOnBattlefield(1, "Forcefield")
            .withLandsOnBattlefield(1, "Plains", 3)
            .withCardOnBattlefield(2, attacker, summoningSickness = false)
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(2).withPriorityPlayer(2)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .apply { if (blocker) withCardOnBattlefield(1, "Grizzly Bears") }.build()
        fun unblocked(game: TestGame, attacker: String = "Hill Giant") {
            game.declareAttackers(mapOf(attacker to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
        }
        fun activate(game: TestGame) {
            if (game.state.priorityPlayerId != game.player1Id) game.passPriority().error shouldBe null
            game.state.priorityPlayerId shouldBe game.player1Id
            game.execute(ActivateAbility(game.player1Id, game.findPermanent("Forcefield")!!,
                cardRegistry.getCard("Forcefield")!!.script.activatedAbilities.single().id)).error shouldBe null
            game.resolveStack()
        }
        fun choose(game: TestGame, attacker: String = "Hill Giant") {
            val decision = game.state.pendingDecision as SelectCardsDecision
            decision.useTargetingUI shouldBe true
            game.selectCards(listOf(game.findPermanent(attacker)!!)).error shouldBe null
        }
        fun finish(game: TestGame) = game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        test("paid untargeted source choice leaves one combat damage and shows a player badge") {
            val game = board(); unblocked(game); activate(game)
            val decision = game.state.pendingDecision as SelectCardsDecision
            decision.options shouldBe listOf(game.findPermanent("Hill Giant")!!)
            choose(game)
            val badge = game.getClientState(1).players.single { it.playerId == game.player1Id }.activeEffects
                .single { it.effectId.startsWith("prevent_next_damage_leaving_amount") }
            badge.name shouldBe "Leave 1 from Hill Giant"
            badge.description!!.contains("combat damage") shouldBe true
            finish(game)
            game.getLifeTotal(1) shouldBe 19
        }
        test("can activate before blockers but cannot yet choose an unblocked source") {
            val game = board()
            game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
            game.passPriority().error shouldBe null
            activate(game)
            game.state.pendingDecision shouldBe null
            game.state.floatingEffects.none { it.effect.modification is SerializableModification.PreventNextDamageLeavingAmount } shouldBe true
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            finish(game)
            game.getLifeTotal(1) shouldBe 17
        }
        test("blocked trampler is not an eligible source even after its blocker dies") {
            val game = board("Craw Wurm", blocker = true)
            // Trample is given using the ordinary card script machinery.
            game.state = services.effectExecutorRegistry.execute(game.state, Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.ContextTarget(0)),
                com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = game.player2Id,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Craw Wurm")!!)))).state
            game.declareAttackers(mapOf("Craw Wurm" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Craw Wurm"))).error shouldBe null
            val destroy = services.effectExecutorRegistry.execute(game.state, Effects.Destroy(EffectTarget.ContextTarget(0)),
                com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = game.player2Id,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!))))
            destroy.error shouldBe null
            game.state = destroy.state
            activate(game)
            game.state.pendingDecision shouldBe null
            finish(game)
            game.getLifeTotal(1) shouldBe 14
        }
        test("hexproof does not stop source choice and double strike spends one shield in first strike") {
            val game = board(duelist.name); unblocked(game, duelist.name)
            activate(game); choose(game, duelist.name); finish(game)
            game.getLifeTotal(1) shouldBe 16
        }
        test("one first-strike damage leaves the shield for a larger regular hit") {
            val game = board("Mons's Goblin Raiders")
            val goblin = game.findPermanent("Mons's Goblin Raiders")!!
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.ContextTarget(0)),
                com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = game.player2Id,
                    targets = listOf(ChosenTarget.Permanent(goblin)))).state
            unblocked(game, "Mons's Goblin Raiders")
            activate(game); choose(game, "Mons's Goblin Raiders")
            game.passUntilPhase(Phase.COMBAT, Step.FIRST_STRIKE_COMBAT_DAMAGE)
            game.getLifeTotal(1) shouldBe 19
            game.state.floatingEffects.count { it.effect.modification is SerializableModification.PreventNextDamageLeavingAmount } shouldBe 1
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.ModifyStats(3, 0, EffectTarget.ContextTarget(0)),
                com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = game.player2Id,
                    targets = listOf(ChosenTarget.Permanent(goblin)))).state
            finish(game)
            game.getLifeTotal(1) shouldBe 18
        }
        test("shield survives Forcefield leaving play before combat damage") {
            val game = board(); unblocked(game); activate(game); choose(game)
            game.state = services.effectExecutorRegistry.execute(game.state, Effects.Destroy(EffectTarget.ContextTarget(0)),
                com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = game.player2Id,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Forcefield")!!)))).state
            game.findPermanent("Forcefield") shouldBe null
            finish(game); game.getLifeTotal(1) shouldBe 19
        }
        test("damage from the chosen creature outside combat leaves the shield unused") {
            val game = board(); unblocked(game); activate(game); choose(game)
            val giant = game.findPermanent("Hill Giant")!!
            val result = DamageUtils.applyDamagePreventionShields(game.state, game.player1Id, 5,
                isCombatDamage = false, sourceId = giant, predicateEvaluator = services.predicateEvaluator)
            result.remainingDamage shouldBe 5
            game.state = result.state
            finish(game); game.getLifeTotal(1) shouldBe 19
        }
        test("face-down chosen source badge hides its printed identity for every viewer") {
            val game = board(); val giant = game.findPermanent("Hill Giant")!!
            game.state = game.state.updateEntity(giant) { it.with(FaceDownComponent) }
            unblocked(game); activate(game); choose(game)
            val views = listOf(game.getClientState(1), game.getClientState(2),
                stateTransformer.transform(game.state, game.player1Id, isSpectator = true))
            for (view in views) {
                val badge = view.players.single { it.playerId == game.player1Id }.activeEffects
                    .single { it.effectId.startsWith("prevent_next_damage_leaving_amount") }
                badge.name shouldBe "Leave 1 from Face-down creature"
                badge.description!!.contains("Hill Giant") shouldBe false
            }
            finish(game); game.getLifeTotal(1) shouldBe 19
        }
    }
}
