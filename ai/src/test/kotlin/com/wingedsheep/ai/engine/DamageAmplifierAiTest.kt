package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DamageAmplifierAiTest : ScenarioTestBase() {
    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private fun definition(name: String) = cardRegistry.getCard(name)!!

    init {
        cardRegistry.register(card("Amplifier Suppression") {
            typeLine = "Enchantment"
            staticAbility { ability = LoseAllAbilities(GroupFilter.AllCreatures) }
        })

        cardRegistry.register(card("AI Damage Barrier") {
            typeLine = "Enchantment"
            replacementEffect(PreventDamage(amount = 1, appliesTo = EventPattern.DamageEvent(recipient = Recipient.You)))
        })

        test("the affected AI applies prevention before multiplication to preserve life") {
            val game = scenario().withPlayers().withActivePlayer(1)
                .withCardOnBattlefield(1, "Angrath's Marauders")
                .withCardOnBattlefield(2, "AI Damage Barrier")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1).build()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            decision.playerId shouldBe game.player2Id
            val response = AIPlayer.create(cardRegistry, game.player2Id, AiProfile.LIVE)
                .respondToDecision(game.state, decision).shouldBeInstanceOf<OptionChosenResponse>()
            game.submitDecision(response).error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            // Preventing one first leaves (3 - 1) * 2; multiplying first would deal five.
            game.getLifeTotal(2) shouldBe 16
        }

        test("AI allocates a shared prevention shield within positive bounds and spends the full shield") {
            val shield = card("AI Shared Damage Shield") {
                manaCost = "{0}"
                typeLine = "Instant"
                spell {
                    val recipient = target(Targets.Any)
                    effect = Effects.PreventNextDamage(5, recipient)
                }
            }
            val d = GameTestDriver().apply {
                registerCards(TestCards.all + listOf(definition("Angrath's Marauders"), shield))
                initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
                passPriorityUntil(Step.PRECOMBAT_MAIN)
            }
            d.putPermanentOnBattlefield(d.player1, "Angrath's Marauders")
            val attackers = List(2) { d.putCreatureOnBattlefield(d.player1, "Centaur Courser") }
            attackers.forEach(d::removeSummoningSickness)
            d.castSpell(d.player1, d.putCardInHand(d.player1, shield.name), listOf(d.player2)).error shouldBe null
            d.bothPass().error shouldBe null
            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(d.player1, attackers, d.player2).error shouldBe null
            d.passPriorityUntil(Step.DECLARE_BLOCKERS)
            d.declareNoBlockers(d.player2).error shouldBe null
            d.bothPass().error shouldBe null
            if (d.pendingDecision is CombatResolutionDecision) d.confirmCombatDamage().error shouldBe null

            // Isolate the numeric allocation: apply the shield before doubling either source.
            val order = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            val prevention = order.options.indexOfFirst { it.contains("prevent up to 5", ignoreCase = true) }
            (prevention >= 0) shouldBe true
            d.submitDecision(order.playerId, OptionChosenResponse(order.id, prevention)).error shouldBe null
            val allocation = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
            allocation.playerId shouldBe d.player2
            allocation.minValue shouldBe 2
            allocation.maxValue shouldBe 3
            val answer = AIPlayer.create(d.cardRegistry, d.player2, AiProfile.LIVE)
                .respondToDecision(d.state, allocation).shouldBeInstanceOf<NumberChosenResponse>()
            (answer.number in allocation.minValue..allocation.maxValue) shouldBe true
            d.submitDecision(allocation.playerId, answer).error shouldBe null

            // Either legal split prevents five of six points before the survivor doubles.
            d.pendingDecision shouldBe null
            d.getLifeTotal(d.player2) shouldBe 18
        }

        test("a turn-scoped one-sided doubler (Insult) reads as a damage amplifier; a plus-N bonus does not") {
            CardIntentAnalyzer.analyze(definition("Insult // Injury")).tags shouldContain IntentTag.DAMAGE_AMPLIFIER
            CardIntentAnalyzer.analyze(definition("Taii Wakeen, Perfect Shot")).tags shouldNotContain IntentTag.DAMAGE_AMPLIFIER
            val symmetric = card("Symmetric Turn Doubler") {
                typeLine = "Sorcery"
                spell { effect = Effects.DoubleDamageThisTurn(EventPattern.DamageEvent()) }
            }
            CardIntentAnalyzer.analyze(symmetric).tags shouldNotContain IntentTag.DAMAGE_AMPLIFIER
        }

        test("one-sided multipliers have intent, a rating term, and permanent value") {
            for (name in listOf("Angrath's Marauders", "Fiery Emancipation", "Twinflame Tyrant")) {
                val card = definition(name)
                val intent = CardIntentAnalyzer.analyze(card)
                intent.tags shouldContain IntentTag.DAMAGE_AMPLIFIER
                intent.staticPriorValue shouldBeGreaterThan 0.5
                LimitedCardRater.explain(card).heuristic.filter { it.part.startsWith("Replacement") }
                    .sumOf { it.value } shouldBeGreaterThan 0.0
            }
            val board = AiCardRating.of(definition("Angrath's Marauders"), emptyList()).board
            board.parts.single { it.label == "Damage amplification" }.value shouldBe 1.5
            board.parts.map { it.label } shouldNotContain "Repeatable removal"
            // Ordinary damage sources are not a scarce resource among Limited decks.
            SynergyAxes.of(definition("Angrath's Marauders")).asks shouldBe emptyList()
        }

        test("symmetric damage and damage to ourselves are not read as one-sided payoffs") {
            val cases = listOf(
                "Symmetric Amplifier" to EventPattern.DamageEvent(),
                "Enemy Amplifier" to EventPattern.DamageEvent(source = GameObjectFilter.Any.opponentControls()),
                "Self Harm Amplifier" to EventPattern.DamageEvent(
                    source = GameObjectFilter.Any.youControl(), recipient = Recipient.You
                ),
            )
            // A noncombat-only or conditional source needs its own fuel/live-state model.
            for (name in listOf("Solphim, Mayhem Dominus", "Neriv, Heart of the Storm", "The Rollercrusher Ride")) {
                CardIntentAnalyzer.analyze(definition(name)).tags shouldNotContain IntentTag.DAMAGE_AMPLIFIER
            }
            for ((name, event) in cases) {
                val definition = card(name) {
                    typeLine = "Enchantment"
                    replacementEffect(DoubleDamage(appliesTo = event))
                }
                CardIntentAnalyzer.analyze(definition).tags shouldNotContain IntentTag.DAMAGE_AMPLIFIER
                LimitedCardRater.explain(definition).heuristic.filter { it.part.startsWith("Replacement") }
                    .sumOf { it.value } shouldBe 0.0
            }
        }

        test("narrow source multipliers ask for their fuel but never the opponent's cards") {
            val own = card("Artifact Amplifier") {
                typeLine = "Enchantment"
                replacementEffect(DoubleDamage(appliesTo = EventPattern.DamageEvent(
                    source = GameObjectFilter.Artifact.youControl()
                )))
            }
            val profile = SynergyAxes.of(own)
            profile.asks.size shouldBe 1
            SynergyAxes.of(definition("Ornithopter")).answers(profile.asks.single()) shouldBe true
            SynergyAxes.of(definition("Grizzly Bears")).answers(profile.asks.single()) shouldBe false
            val enemy = card("Enemy Artifact Amplifier") {
                typeLine = "Enchantment"
                replacementEffect(DoubleDamage(appliesTo = EventPattern.DamageEvent(
                    source = GameObjectFilter.Artifact.opponentControls()
                )))
            }
            SynergyAxes.of(enemy).asks shouldBe emptyList()
        }

        test("Marauders has an ability premium only while its damage multiplier is visible and active") {
            for (hidden in listOf(false, true)) {
                val game = scenario().withPlayers().withCardOnBattlefield(1, "Angrath's Marauders").build()
                val id = game.state.getBattlefield(game.player1Id).single()
                if (hidden) game.state = game.state.updateEntity(id) { it.with(FaceDownComponent) }
                val component = game.state.getEntity(id)!!.get<CardComponent>()!!
                val projected = game.state.projectedState
                val plain = BoardPresence.permanentValue(game.state, projected, id, component)
                BoardPresence.permanentValue(game.state, projected, id, component, intents) shouldBe
                    plain + if (hidden) 0.0 else 1.5
            }
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Angrath's Marauders")
                .withCardOnBattlefield(2, "Amplifier Suppression").build()
            val id = game.state.getBattlefield(game.player1Id).single()
            val component = game.state.getEntity(id)!!.get<CardComponent>()!!
            val projected = game.state.projectedState
            projected.hasLostAllAbilities(id) shouldBe true
            BoardPresence.permanentValue(game.state, projected, id, component, intents) shouldBe
                BoardPresence.permanentValue(game.state, projected, id, component)
        }
    }
}
