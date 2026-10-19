package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * The unnamed optional additional cost — "As an additional cost to cast this spell, you may
 * <cost>" with a linked "if this spell's additional cost was paid" rider
 * ([KeywordAbility.optionalAdditionalCost] + [Conditions.AdditionalCostWasPaid]).
 *
 * The cost is declared and paid while casting (CR 601.2b, 601.2h), so it is gone even if the spell
 * never resolves; it rides the kicker rail under [ChoiceSlot.ADDITIONAL_COST_PAID], so the spell is
 * never kicked — neither for its own `WasKicked` reads nor for "whenever you cast a kicked spell".
 */
class OptionalAdditionalCostScenarioTest : ScenarioTestBase() {

    private val trinket = card("Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    // Voltage Surge's shape: the rider reads the declaration off the spell on the stack.
    private val surgeBolt = card("Surge Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        keywordAbility(KeywordAbility.optionalAdditionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact)))
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.If(
                condition = Conditions.AdditionalCostWasPaid,
                then = Effects.DealDamage(4, t),
                otherwise = Effects.DealDamage(2, t),
            )
        }
    }

    // The permanent shape: the declaration rides the resolving permanent into its enters trigger,
    // and a kicked-only rider on the same card stays off.
    private val surgeBear = card("Surge Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.optionalAdditionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact)))
        triggeredAbility {
            trigger = Triggers.self.enters()
            interveningIf = Conditions.AdditionalCostWasPaid
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        }
        triggeredAbility {
            trigger = Triggers.self.enters()
            interveningIf = Conditions.WasKicked
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 10, EffectTarget.Self)
        }
    }

    private val kickedWatcher = card("Kicked Watcher") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.WasKicked))
            effect = Effects.GainLife(3)
        }
    }

    private fun TestGame.castPaying(spell: String, fodder: String, target: EntityId? = null): ExecutionResult {
        val cardId = state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == spell }
        return execute(
            CastSpell(
                playerId = player1Id,
                cardId = cardId,
                targets = listOfNotNull(target?.let { ChosenTarget.Permanent(it) }),
                declaredCostSlot = ChoiceSlot.ADDITIONAL_COST_PAID,
                additionalCostPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(findPermanent(fodder).shouldNotBeNull())
                ),
            )
        )
    }

    private fun board(spell: String, land: String, lands: Int, vararg extra: String) = extra
        .fold(scenario().withPlayers("Caster", "Opponent")) { b, name -> b.withCardOnBattlefield(1, name) }
        .withCardInHand(1, spell)
        .withCardOnBattlefield(1, "Test Trinket")
        .withCardOnBattlefield(2, "Centaur Courser")
        .withLandsOnBattlefield(1, land, lands)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(trinket)
        cardRegistry.register(surgeBolt)
        cardRegistry.register(surgeBear)
        cardRegistry.register(kickedWatcher)

        test("paying the cost sacrifices on cast and turns the rider on") {
            val game = board("Surge Bolt", "Mountain", 1)
            val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()

            game.castPaying("Surge Bolt", "Test Trinket", courser).error shouldBe null
            // Paid as a cost (CR 601.2h): gone while the spell is still on the stack.
            game.isInGraveyard(1, "Test Trinket") shouldBe true
            game.resolveStack()

            game.isOnBattlefield("Centaur Courser") shouldBe false
        }

        test("declining the cost keeps the printed effect") {
            val game = board("Surge Bolt", "Mountain", 1)
            val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()

            game.castSpell(1, "Surge Bolt", courser).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Centaur Courser") shouldBe true
            game.isOnBattlefield("Test Trinket") shouldBe true
        }

        test("the cast menu offers a variant named for the cost") {
            val game = board("Surge Bolt", "Mountain", 1)
            val offered = game.getLegalActions(1).filter { it.actionType == "CastWithKicker" }
            offered.map { it.description } shouldContain "Cast Surge Bolt (Sacrifice an artifact)"
        }

        test("the paid fact rides the permanent into its enters trigger, and it is not kicked") {
            val game = board("Surge Bear", "Forest", 2)

            game.castPaying("Surge Bear", "Test Trinket").error shouldBe null
            game.resolveStack()

            val bear = game.findPermanent("Surge Bear").shouldNotBeNull()
            game.state.getEntity(bear)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("unpaid, the enters trigger never fires") {
            val game = board("Surge Bear", "Forest", 2)

            game.castSpell(1, "Surge Bear").error shouldBe null
            game.resolveStack()

            val bear = game.findPermanent("Surge Bear").shouldNotBeNull()
            (game.state.getEntity(bear)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
        }

        test("a spell whose additional cost was paid is not a kicked spell") {
            val game = board("Surge Bolt", "Mountain", 1, "Kicked Watcher")
            val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()

            game.castPaying("Surge Bolt", "Test Trinket", courser).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 20
        }
    }
}
