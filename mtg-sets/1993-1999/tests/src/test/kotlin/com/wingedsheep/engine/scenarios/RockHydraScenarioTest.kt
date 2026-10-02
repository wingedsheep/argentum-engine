package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.matchers.shouldBe

class RockHydraScenarioTest : ScenarioTestBase() {
    init {
        val shelter = card("Hydra Test Shelter") {
            typeLine = "Enchantment"
            staticAbility { ability = ModifyStats(0, 5, GroupFilter(GameObjectFilter.Creature.youControl())) }
        }
        cardRegistry.register(shelter)
        val noPrevention = card("Hydra Test No Prevention") {
            typeLine = "Enchantment"
            replacementEffect(com.wingedsheep.sdk.scripting.DamageCantBePrevented())
        }
        cardRegistry.register(noPrevention)
        fun board(count: Int = 4, support: Boolean = false, unpreventable: Boolean = false,
                  step: Step = Step.PRECOMBAT_MAIN) = scenario().withPlayers("Hydra", "Opponent")
            .withCardOnBattlefield(1, "Rock Hydra").withLandsOnBattlefield(1, "Mountain", 8)
            .withCardInHand(1, "Shock").withCardInHand(1, "Lightning Bolt")
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .apply {
                if (support) withCardOnBattlefield(1, shelter.name)
                if (unpreventable) withCardOnBattlefield(2, noPrevention.name)
            }.withActivePlayer(1).inPhase(if (step == Step.UPKEEP) Phase.BEGINNING else Phase.PRECOMBAT_MAIN, step)
            .build().also { game ->
                val id = game.findPermanent("Rock Hydra")!!
                game.state = game.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to count))) }
            }
        fun count(game: TestGame) = game.state.getEntity(game.findPermanent("Rock Hydra")!!)!!
            .get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE)
        fun damage(game: TestGame) = game.state.getEntity(game.findPermanent("Rock Hydra")!!)!!
            .get<DamageComponent>()?.amount ?: 0
        fun activate(game: TestGame, index: Int) = game.execute(ActivateAbility(game.player1Id,
            game.findPermanent("Rock Hydra")!!, cardRegistry.getCard("Rock Hydra")!!.script.activatedAbilities[index].id))
        test("X is captured as entry counters and zero X dies") {
            for (x in listOf(0, 4)) {
                val game = scenario().withPlayers("One", "Two").withCardInHand(1, "Rock Hydra")
                    .withLandsOnBattlefield(1, "Mountain", 6).withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                game.castXSpell(1, "Rock Hydra", x).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Rock Hydra") shouldBe (x > 0)
                if (x > 0) {
                    count(game) shouldBe x
                    game.state.projectedState.getPower(game.findPermanent("Rock Hydra")!!) shouldBe x
                } else game.isInGraveyard(1, "Rock Hydra") shouldBe true
            }
        }
        test("damage removes one counter per point without marking prevented damage") {
            val game = board()
            game.castSpell(1, "Shock", game.findPermanent("Rock Hydra")!!).error shouldBe null
            game.resolveStack()
            count(game) shouldBe 2
            damage(game) shouldBe 0
        }
        test("damage above counters is dealt rather than fully prevented") {
            val game = board(count = 1, support = true)
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Rock Hydra")!!).error shouldBe null
            game.resolveStack()
            count(game) shouldBe 0
            damage(game) shouldBe 2
        }
        test("no counters means no prevention while another effect supplies toughness") {
            val game = board(count = 0, support = true)
            game.castSpell(1, "Shock", game.findPermanent("Rock Hydra")!!).error shouldBe null
            game.resolveStack()
            count(game) shouldBe 0
            damage(game) shouldBe 2
        }
        test("unpreventable damage removes counters and is still marked in full") {
            val game = board(count = 4, support = true, unpreventable = true)
            game.castSpell(1, "Shock", game.findPermanent("Rock Hydra")!!).error shouldBe null
            game.resolveStack()
            count(game) shouldBe 2
            damage(game) shouldBe 2
        }
        test("paid prevention preserves counters and multiple activations accumulate") {
            val game = board()
            repeat(2) { activate(game, 0).error shouldBe null; game.resolveStack() }
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Rock Hydra")!!).error shouldBe null
            game.resolveStack()
            count(game) shouldBe 3
            damage(game) shouldBe 0
        }
        test("paid prevention survives unpreventable damage and protects the next preventable hit") {
            val game = board(count = 4, support = true)
            activate(game, 0).error shouldBe null; game.resolveStack()
            val id = game.findPermanent("Rock Hydra")!!
            game.state = game.state.updateEntity(id) { it.with(
                com.wingedsheep.engine.state.components.battlefield.DamageUnpreventableThisTurnComponent) }
            game.castSpell(1, "Shock", id).error shouldBe null; game.resolveStack()
            count(game) shouldBe 2
            damage(game) shouldBe 2
            game.state = game.state.updateEntity(id) { it.without<
                com.wingedsheep.engine.state.components.battlefield.DamageUnpreventableThisTurnComponent>() }
            game.castSpell(1, "Lightning Bolt", id).error shouldBe null; game.resolveStack()
            count(game) shouldBe 0
            damage(game) shouldBe 2
        }
        test("paid shield lasts until cleanup and does not persist into another turn") {
            val game = board()
            activate(game, 0).error shouldBe null; game.resolveStack()
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            // Return to our main phase so Shock's mana can be supplied normally.
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            game.castSpell(1, "Shock", game.findPermanent("Rock Hydra")!!).error shouldBe null
            game.resolveStack()
            count(game) shouldBe 2
        }
        test("refill is legal only during its controller's upkeep") {
            val main = board()
            (activate(main, 1).error != null) shouldBe true
            val upkeep = board(step = Step.UPKEEP)
            activate(upkeep, 1).error shouldBe null; upkeep.resolveStack()
            count(upkeep) shouldBe 5
            upkeep.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            upkeep.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            upkeep.state.activePlayerId shouldBe upkeep.player2Id
            upkeep.passPriority().error shouldBe null
            (activate(upkeep, 1).error != null) shouldBe true
        }
        test("paid shield also preserves counters in combat") {
            val game = scenario().withPlayers("Hydra", "Opponent")
                .withCardOnBattlefield(1, "Rock Hydra").withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS).build()
            val id = game.findPermanent("Rock Hydra")!!
            game.state = game.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 4))) }
            activate(game, 0).error shouldBe null; game.resolveStack()
            game.declareAttackers(mapOf("Rock Hydra" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Rock Hydra"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            count(game) shouldBe 3
            damage(game) shouldBe 0
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
        test("only damage remaining after counters supplies lifelink and deathtouch") {
            for (counters in listOf(1, 4)) {
                val game = scenario().withPlayers("Hydra", "Opponent")
                    .withCardOnBattlefield(1, "Rock Hydra").withCardOnBattlefield(1, shelter.name)
                    .withCardOnBattlefield(2, "Vampire Nighthawk")
                    .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                    .withActivePlayer(1).inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS).build()
                val id = game.findPermanent("Rock Hydra")!!
                game.state = game.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to counters))) }
                game.declareAttackers(mapOf("Rock Hydra" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Vampire Nighthawk" to listOf("Rock Hydra"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.getLifeTotal(2) shouldBe if (counters == 1) 21 else 20
                game.isOnBattlefield("Rock Hydra") shouldBe (counters == 4)
                if (counters == 4) count(game) shouldBe 2
            }
        }
        test("combat prevention spends counters across two attackers and preserves outgoing assigned power") {
            val game = scenario().withPlayers("Hydra", "Opponent")
                .withCardOnBattlefield(1, "Rock Hydra").withCardOnBattlefield(1, shelter.name)
                .withCardOnBattlefield(2, "Grizzly Bears").withCardOnBattlefield(2, "Centaur Courser")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS).build()
            val id = game.findPermanent("Rock Hydra")!!
            game.state = game.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3))) }
            game.declareAttackers(mapOf("Rock Hydra" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Rock Hydra"), "Centaur Courser" to listOf("Rock Hydra"))).error shouldBe null
            repeat(12) {
                if (game.state.pendingDecision is CombatResolutionDecision) game.submitDefaultCombatDamage().error shouldBe null
                else if (game.state.step != Step.END_COMBAT) game.passPriority().error shouldBe null
            }
            count(game) shouldBe 0
            damage(game) shouldBe 2
            // Three power was assigned before the counters were removed.
            (game.isInGraveyard(2, "Grizzly Bears") || game.isInGraveyard(2, "Centaur Courser")) shouldBe true
        }
    }
}
