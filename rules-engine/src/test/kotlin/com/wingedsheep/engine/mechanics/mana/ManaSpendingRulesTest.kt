package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.SpendManaAsColor
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.matchers.shouldBe

class ManaSpendingRulesTest : ScenarioTestBase() {
    init {
        val spending = mapOf(Color.RED to setOf(Color.RED, Color.WHITE))
        fun pool(white: Int = 0, red: Int = 0) = ManaPool(white = white, red = red, spendingColors = spending)
        fun cost(value: String) = ManaCost.parse(value)
        test("white pays red but red cannot pay white") {
            pool(white = 1).canPay(cost("{R}")) shouldBe true
            pool(red = 1).canPay(cost("{W}")) shouldBe false
        }
        test("actual spent colors are preserved") {
            val result = pool(white = 1).payPartial(cost("{R}"))
            result.remainingCost.isEmpty() shouldBe true
            result.manaSpent.white shouldBe 1
            result.manaSpent.red shouldBe 0
            result.newPool.white shouldBe 0
        }
        test("augmenting paths protect strict white even when red is first") {
            val result = pool(white = 1, red = 1).payPartial(cost("{R}{W}"))
            result.remainingCost.isEmpty() shouldBe true
            result.manaSpent.white shouldBe 1
            result.manaSpent.red shouldBe 1
        }
        test("partial matching reserves floating white for a strict white pip") {
            val result = pool(white = 1).payPartial(cost("{R}{W}"))
            result.remainingCost shouldBe cost("{R}")
            result.manaSpent.white shouldBe 1
        }
        test("unpaid pips keep the original color") {
            pool().payPartial(cost("{R}")).remainingCost shouldBe cost("{R}")
            pool(white = 1).payPartial(cost("{R}{R}")).remainingCost shouldBe cost("{R}")
        }
        test("hybrid and phyrexian color halves accept white") {
            for (symbol in listOf("{R/U}", "{U/R}", "{R/P}", "{R/U/P}")) {
                pool(white = 1).pay(cost(symbol))?.white shouldBe 0
            }
        }
        test("monocolored hybrid accepts white or its generic alternative") {
            pool(white = 1).pay(cost("{2/R}"))?.white shouldBe 0
            ManaPool(blue = 2, spendingColors = spending).pay(cost("{2/R}"))?.blue shouldBe 0
            pool().payPartial(cost("{2/R}")).remainingCost shouldBe cost("{2/R}")
        }
        test("mono-hybrid fallback cannot consume mana reserved for later strict pips") {
            val result = ManaPool(white = 1, colorless = 1, spendingColors = spending)
                .payPartial(cost("{2/U}{W}"))
            result.remainingCost shouldBe cost("{2/U}")
            result.manaSpent.white shouldBe 1
            result.manaSpent.colorless shouldBe 0
            result.newPool.colorless shouldBe 1
            ManaPool(white = 1, colorless = 2, spendingColors = spending)
                .canPay(cost("{2/U}{W}")) shouldBe true
        }
        test("substitution grants no colorless payment and does not duplicate mana") {
            pool(white = 1).canPay(cost("{C}")) shouldBe false
            pool(white = 1).canPay(cost("{W}{R}")) shouldBe false
            pool(white = 1).canPay(cost("{R}{1}")) shouldBe false
            pool(white = 2).canPay(cost("{R}{1}")) shouldBe true
        }
        test("color restricted X counts actual color") {
            pool(white = 2).xCoverage(2, setOf(Color.RED), null) shouldBe 0
        }
        test("restricted white remains restricted and retains riders and provenance") {
            val tagged = RestrictedManaEntry(color = Color.WHITE, restriction = ManaRestriction.CreatureSpellsOnly)
            val pool = ManaPool(restrictedMana = listOf(tagged), spendingColors = spending)
            pool.canPay(cost("{R}"), SpellPaymentContext(isCreature = false)) shouldBe false
            val result = pool.payPartial(cost("{R}"), SpellPaymentContext(isCreature = true))
            result.remainingCost.isEmpty() shouldBe true
            result.newPool.restrictedMana shouldBe emptyList()
            result.manaSpent.white shouldBe 1
        }
        test("matching remains bounded on a large mixed colored cost") {
            pool(white = 100, red = 100).canPay(cost("{R}".repeat(100) + "{W}".repeat(100))) shouldBe true
        }
        cardRegistry.register(card("Test Spending Permission") {
            manaCost = "{0}"; typeLine = "Artifact"
            staticAbility { ability = SpendManaAsColor(Color.WHITE, Color.RED) }
        })
        fun board() = scenario().withPlayers().withCardOnBattlefield(1, "Test Spending Permission")
            .withLandsOnBattlefield(1, "Plains", 2).withActivePlayer(1).build()
        test("solver produces and records actual white mana for red costs") {
            val game = board()
            val solution = services.manaSolver.solve(game.state, game.player1Id, cost("{R}{W}"))!!
            solution.sources.size shouldBe 2
            solution.manaProduced.values.all { it.color == Color.WHITE } shouldBe true
            services.manaSolver.canPay(game.state, game.player2Id, cost("{R}")) shouldBe false
        }
        test("partial mono-hybrid payment preserves the colored alternative for a land") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Test Spending Permission")
                .withLandsOnBattlefield(1, "Plains", 1).build()
            game.state = game.state.updateEntity(game.player1Id) {
                it.with(com.wingedsheep.engine.state.components.player.ManaPoolComponent(colorless = 1))
            }
            services.manaSolver.canPay(game.state, game.player1Id, cost("{2/R}{1}")) shouldBe true
            val partial = ManaPool(colorless = 1, spendingColors = spending).payPartial(cost("{2/R}{1}"))
            partial.remainingCost shouldBe cost("{2/R}")
            partial.manaSpent.colorless shouldBe 1
        }
        test("partial payment does not select the wrong mono-hybrid generic alternative") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Test Spending Permission")
                .withLandsOnBattlefield(1, "Plains", 1).withLandsOnBattlefield(1, "Island", 1).build()
            game.state = game.state.updateEntity(game.player1Id) {
                it.with(com.wingedsheep.engine.state.components.player.ManaPoolComponent(colorless = 2))
            }
            val partial = ManaPool(colorless = 2, spendingColors = spending).payPartial(cost("{2/R}{2/U}"))
            partial.remainingCost shouldBe cost("{2/R}{2/U}")
            partial.newPool.colorless shouldBe 2
            services.manaSolver.canPay(game.state, game.player1Id, cost("{2/R}{2/U}")) shouldBe true
        }
        test("complete pool payment can use generic mono-hybrid alternatives") {
            val available = ManaPool(colorless = 3, spendingColors = spending)
            available.canPay(cost("{2/R}{1}")) shouldBe true
            available.pay(cost("{2/R}{1}"))?.colorless shouldBe 0
            available.payPartial(cost("{2/R}{1}")).remainingCost.isEmpty() shouldBe true
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Test Spending Permission").build()
            game.state = game.state.updateEntity(game.player1Id) {
                it.with(com.wingedsheep.engine.state.components.player.ManaPoolComponent(colorless = 3))
            }
            services.manaSolver.canPay(game.state, game.player1Id, cost("{2/R}{1}")) shouldBe true
            ManaPool(colorless = 1, spendingColors = spending).canPay(cost("{2/R}")) shouldBe false
        }
        cardRegistry.register(card("Test Red Hybrid Spell") {
            manaCost = "{R}{W/G}"; typeLine = "Sorcery"
        })
        test("solver preserves substitute white for a hybrid when native red is available") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Test Spending Permission")
                .withLandsOnBattlefield(1, "Plains", 1).withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Test Red Hybrid Spell").build()
            val solution = services.manaSolver.solve(game.state, game.player1Id, cost("{R}{W/G}"))!!
            solution.manaProduced.values.map { it.color }.toSet() shouldBe setOf(Color.RED, Color.WHITE)
            services.manaSolver.canPay(game.state, game.player1Id, cost("{R}{W/G}")) shouldBe true
        }
        cardRegistry.register(card("Test Overlapping Spending") {
            manaCost = "{0}"; typeLine = "Artifact"
            staticAbility { ability = SpendManaAsColor(Color.WHITE, Color.RED) }
            staticAbility { ability = SpendManaAsColor(Color.GREEN, Color.RED) }
            staticAbility { ability = SpendManaAsColor(Color.WHITE, Color.BLUE) }
            staticAbility { ability = SpendManaAsColor(Color.BLACK, Color.BLUE) }
        })
        test("solver matches overlapping permissions to distinct actual sources") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Test Overlapping Spending")
                .withLandsOnBattlefield(1, "Plains", 1).withLandsOnBattlefield(1, "Forest", 1).build()
            for (value in listOf("{R}{U}", "{U}{R}")) {
                val solution = services.manaSolver.solve(game.state, game.player1Id, cost(value))!!
                solution.manaProduced.values.map { it.color }.toSet() shouldBe setOf(Color.WHITE, Color.GREEN)
                services.manaSolver.canPay(game.state, game.player1Id, cost(value)) shouldBe true
            }
        }
        test("irrelevant multi-mana source does not disable overlapping color matching") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Test Overlapping Spending")
                .withLandsOnBattlefield(1, "Plains", 1).withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Sol Ring").build()
            for (value in listOf("{R}{U}", "{U}{R}", "{R}{U}{2}")) {
                val solution = services.manaSolver.solve(game.state, game.player1Id, cost(value))!!
                solution.manaProduced.values.mapNotNull { it.color }.toSet() shouldBe setOf(Color.WHITE, Color.GREEN)
                services.manaSolver.canPay(game.state, game.player1Id, cost(value)) shouldBe true
            }
        }
        test("source matching falls back when strict pips need complex production") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Test Spending Permission")
                .withLandsOnBattlefield(1, "Plains", 1).withCardOnBattlefield(1, "Sol Ring").build()
            val solution = services.manaSolver.solve(game.state, game.player1Id, cost("{R}{C}{C}"))!!
            solution.sources.size shouldBe 2
            services.manaSolver.canPay(game.state, game.player1Id, cost("{R}{C}{C}")) shouldBe true
        }
        test("permission follows projected controller") {
            val game = board()
            val id = game.findPermanent("Test Spending Permission")!!
            game.state = game.state.updateEntity(id) { it.with(ControllerComponent(game.player2Id)) }
            ManaSpendingRules.colors(game.state, game.player1Id) shouldBe emptyMap()
            ManaSpendingRules.colors(game.state, game.player2Id)[Color.RED] shouldBe setOf(Color.RED, Color.WHITE)
        }
        cardRegistry.register(card("Test Conditional Spending") {
            manaCost = "{0}"; typeLine = "Artifact"
            staticAbility {
                condition = com.wingedsheep.sdk.dsl.Conditions.SourceIsUntapped
                ability = SpendManaAsColor(Color.WHITE, Color.RED)
            }
        })
        cardRegistry.register(card("Test Spending Creature") {
            manaCost = "{0}"; typeLine = "Creature"; power = 1; toughness = 1
            staticAbility { ability = SpendManaAsColor(Color.WHITE, Color.RED) }
        })
        test("conditional printed permission stops when its source becomes tapped") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Test Conditional Spending").build()
            val id = game.findPermanent("Test Conditional Spending")!!
            ManaSpendingRules.colors(game.state, game.player1Id)[Color.RED] shouldBe setOf(Color.RED, Color.WHITE)
            game.state = game.state.updateEntity(id) { it.with(com.wingedsheep.engine.state.components.battlefield.TappedComponent) }
            ManaSpendingRules.colors(game.state, game.player1Id) shouldBe emptyMap()
        }
        cardRegistry.register(card("Test Ability Suppression") {
            manaCost = "{0}"; typeLine = "Enchantment"
            staticAbility { ability = com.wingedsheep.sdk.scripting.LoseAllAbilities(
                com.wingedsheep.sdk.scripting.filters.unified.GroupFilter(
                    com.wingedsheep.sdk.scripting.GameObjectFilter.Creature)) }
        })
        test("projected removal of all abilities suppresses the permission") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Test Spending Creature")
                .withCardOnBattlefield(2, "Test Ability Suppression").build()
            ManaSpendingRules.colors(game.state, game.player1Id) shouldBe emptyMap()
        }
        test("unrestricted provenance survives substitution until the payment consumes it") {
            val original = ManaPool(white = 1, manaBySource = mapOf(com.wingedsheep.sdk.model.EntityId("land") to 1),
                spendingColors = spending)
            val paid = original.pay(cost("{R}"))!!
            val consumed = paid.consumeProvenance(1)
            consumed.second.sourceIds shouldBe setOf(com.wingedsheep.sdk.model.EntityId("land"))
            consumed.first.manaBySource shouldBe emptyMap()
        }
        cardRegistry.register(card("Test Empty Artifact") { manaCost = "{0}"; typeLine = "Artifact" })
        cardRegistry.register(card("Test Copy Artifact") {
            manaCost = "{0}"; typeLine = "Sorcery"
            spell {
                val subject = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Artifact)
                val model = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Artifact)
                effect = com.wingedsheep.sdk.dsl.Effects.EachPermanentBecomesCopyOfTarget(target = model, affected = subject)
            }
        })
        test("copying and recopying adopt the model's spending permissions") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Test Empty Artifact")
                .withCardOnBattlefield(2, "Test Spending Permission").withCardOnBattlefield(2, "Test Empty Artifact")
                .withCardInHand(1, "Test Copy Artifact").withCardInHand(1, "Test Copy Artifact")
                .withActivePlayer(1).build()
            val subject = game.findPermanents("Test Empty Artifact").first {
                game.state.projectedState.getController(it) == game.player1Id
            }
            val blank = game.findPermanents("Test Empty Artifact").first {
                game.state.projectedState.getController(it) == game.player2Id
            }
            val model = game.findPermanent("Test Spending Permission")!!
            fun copy(target: com.wingedsheep.sdk.model.EntityId) {
                val spell = game.state.getHand(game.player1Id).first()
                game.execute(com.wingedsheep.engine.core.CastSpell(game.player1Id, spell, targets = listOf(
                    com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(subject),
                    com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(target)
                ))).error shouldBe null
                game.resolveStack()
            }
            ManaSpendingRules.colors(game.state, game.player1Id) shouldBe emptyMap()
            copy(model)
            ManaSpendingRules.colors(game.state, game.player1Id)[Color.RED] shouldBe setOf(Color.RED, Color.WHITE)
            copy(blank)
            ManaSpendingRules.colors(game.state, game.player1Id) shouldBe emptyMap()
        }
        test("face down and phased out holders grant no spending permission") {
            val game = board()
            val id = game.findPermanent("Test Spending Permission")!!
            val original = game.state
            game.state = original.updateEntity(id) { it.with(com.wingedsheep.engine.state.components.identity.FaceDownComponent) }
            ManaSpendingRules.colors(game.state, game.player1Id) shouldBe emptyMap()
            game.state = original.updateEntity(id) { it.with(com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent(game.player1Id)) }
            ManaSpendingRules.colors(game.state, game.player1Id) shouldBe emptyMap()
        }
        test("copiable permission data survives serialized saved game state") {
            val game = board()
            val json = kotlinx.serialization.json.Json { allowStructuredMapKeys = true; serializersModule = com.wingedsheep.engine.core.engineSerializersModule }
            val restored = json.decodeFromString(com.wingedsheep.engine.state.GameState.serializer(),
                json.encodeToString(com.wingedsheep.engine.state.GameState.serializer(), game.state))
            ManaSpendingRules.colors(restored, game.player1Id) shouldBe ManaSpendingRules.colors(game.state, game.player1Id)
        }
        test("runtime player grants compose transitively and respect source durations") {
            val game = board()
            val id = game.findPermanent("Test Spending Permission")!!
            game.state = game.state.copy(grantedStaticAbilities = listOf(
                GrantedStaticAbility(game.player1Id, SpendManaAsColor(Color.RED, Color.BLUE), Duration.WhileSourceOnBattlefield(), id)
            ))
            ManaSpendingRules.colors(game.state, game.player1Id)[Color.BLUE] shouldBe setOf(Color.BLUE, Color.RED, Color.WHITE)
            game.state = game.state.copy(grantedStaticAbilities = listOf(
                GrantedStaticAbility(game.player1Id, SpendManaAsColor(Color.RED, Color.BLUE), Duration.WhileSourceOnBattlefield(), game.player2Id)
            ))
            ManaSpendingRules.colors(game.state, game.player1Id)[Color.BLUE] shouldBe null
        }
    }
}
