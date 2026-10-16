package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FilteredAnyTargetScenarioTest : ScenarioTestBase() {
    private val filter = GameObjectFilter.Any.wasDealtDamageThisTurn()
    private val bolt = card("Filtered Test Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val recipient = target(Targets.Any(filter))
            effect = Effects.DealDamage(1, recipient)
        }
    }
    private val siege = card("Damage History Siege") {
        manaCost = "{3}"
        typeLine = "Battle — Siege"
        startingDefense = 8
    }
    private val hexproofCreature = card("History Hexproof Creature") {
        manaCost = "{2}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 4
        keywords(Keyword.HEXPROOF)
    }
    private val witherer = card("History Witherer") {
        manaCost = "{2}"
        typeLine = "Creature — Elemental"
        power = 1
        toughness = 3
        keywords(Keyword.WITHER)
    }

    private fun TestGame.checkEligible(id: EntityId) {
        services.targetFinder.findLegalTargets(state, Targets.Any(filter), player1Id) shouldContain id
        TargetEnumerationUtils(services.predicateEvaluator).findValidTargets(state, player1Id, Targets.Any(filter)) shouldContain id
    }

    init {
        cardRegistry.register(bolt)
        cardRegistry.register(siege)
        cardRegistry.register(witherer)
        cardRegistry.register(hexproofCreature)

        for (targetKind in listOf("player", "planeswalker", "battle")) {
            test("combat damage history enables filtered targeting of a $targetKind") {
                val builder = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Filtered Test Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                if (targetKind == "planeswalker") builder.withCardOnBattlefield(2, "Jace Beleren")
                if (targetKind == "battle") builder.withCardOnBattlefield(1, "Damage History Siege")
                val game = builder.build()
                val name = if (targetKind == "planeswalker") "Jace Beleren" else "Damage History Siege"
                val recipient = if (targetKind == "player") game.player2Id else game.findPermanent(name)!!
                if (targetKind == "planeswalker") {
                    game.state = game.state.updateEntity(recipient) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 8)) }
                }
                game.checkStateBasedActions()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                if (targetKind == "player") game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                else game.declareAttackersWithPermanentTargets(permanentAttackers = mapOf("Grizzly Bears" to name)).error shouldBe null
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.checkEligible(recipient)
                val result = if (targetKind == "player") game.castSpellTargetingPlayer(1, "Filtered Test Bolt", 2)
                    else game.castSpell(1, "Filtered Test Bolt", recipient)
                result.error shouldBe null
                game.resolveStack()
                if (targetKind == "player") game.getLifeTotal(2) shouldBe 17
                else game.state.getEntity(recipient)!!.get<CountersComponent>()!!.getCount(
                    if (targetKind == "planeswalker") CounterType.LOYALTY else CounterType.DEFENSE
                ) shouldBe 5
            }
        }

        test("noncombat damage to a battle is recorded and its defense is removed") {
            val game = scenario().withPlayers("Caster", "Opponent").withCardOnBattlefield(1, "Damage History Siege").build()
            val battle = game.findPermanent("Damage History Siege")!!
            game.state = DamageUtils.dealDamageToTarget(zones, game.state, battle, 1, null).state
            game.checkEligible(battle)
            game.state.getEntity(battle)!!.get<CountersComponent>()!!.getCount(CounterType.DEFENSE) shouldBe 7
        }

        test("wither damage qualifies even though it leaves no marked damage") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "History Witherer", summoningSickness = false)
                .withCardOnBattlefield(2, "Hill Giant")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            game.declareAttackers(mapOf("History Witherer" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("History Witherer"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.checkEligible(giant)
            game.state.getEntity(giant)!!.get<DamageComponent>()?.amount shouldBe null
            game.state.getEntity(giant)!!.get<CountersComponent>()!!.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
        }

        test("damage history does not bypass hexproof") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(2, "History Hexproof Creature")
                .withCardInHand(1, "Filtered Test Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()
            val bogle = game.findPermanent("History Hexproof Creature")!!
            game.state = DamageUtils.dealDamageToTarget(zones, game.state, bogle, 1, null).state
            services.targetFinder.findLegalTargets(game.state, Targets.Any(filter), game.player1Id) shouldNotContain bogle
            game.castSpell(1, "Filtered Test Bolt", bogle).error shouldNotBe null
        }

        test("a damaged noncreature land cannot be submitted as an any-target permanent") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withLandsOnBattlefield(2, "Forest", 1)
                .withCardInHand(1, "Filtered Test Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()
            val forest = game.findPermanent("Forest")!!
            game.state = game.state.updateEntity(forest) {
                it.with(com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent(noncombat = true))
            }
            game.castSpell(1, "Filtered Test Bolt", forest).error shouldNotBe null
        }

        test("chosen and random retargeting keep the original spell's damage restriction") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Filtered Test Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            game.state = DamageUtils.dealDamageToTarget(zones, game.state, giant, 1, null).state
            game.state = DamageUtils.dealDamageToTarget(zones, game.state, game.player2Id, 1, null).state
            val spell = game.findCardsInHand(1, "Filtered Test Bolt").single()
            game.castSpell(1, "Filtered Test Bolt", giant).error shouldBe null
            val context = com.wingedsheep.engine.handlers.EffectContext(
                sourceId = null, controllerId = game.player2Id,
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell(spell))
            )
            val changed = com.wingedsheep.engine.handlers.effects.stack.ChangeTargetExecutor(predicateEvaluator = services.predicateEvaluator, targetFinder = services.targetFinder).execute(
                game.state, com.wingedsheep.sdk.scripting.effects.ChangeTargetEffect(), context
            )
            (changed.state.pendingDecision as com.wingedsheep.engine.core.SelectCardsDecision).options shouldBe listOf(game.player2Id)
            val random = com.wingedsheep.engine.handlers.effects.stack.ReselectTargetRandomlyExecutor(predicateEvaluator = services.predicateEvaluator, targetFinder = services.targetFinder).execute(
                game.state, com.wingedsheep.sdk.scripting.effects.ReselectTargetRandomlyEffect,
                context.copy(triggeringEntityId = spell)
            )
            val selected = random.state.getEntity(spell)!!
                .get<com.wingedsheep.engine.state.components.stack.TargetsComponent>()!!.targets.single()
            (selected in listOf(
                com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(giant),
                com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player2Id)
            )) shouldBe true
            val creatureRedirect = com.wingedsheep.engine.handlers.effects.stack.ChangeSpellTargetExecutor(targetFinder = services.targetFinder).execute(
                game.state, com.wingedsheep.sdk.scripting.effects.ChangeSpellTargetEffect(), context
            )
            creatureRedirect.state.pendingDecision shouldBe null
        }
    }
}
