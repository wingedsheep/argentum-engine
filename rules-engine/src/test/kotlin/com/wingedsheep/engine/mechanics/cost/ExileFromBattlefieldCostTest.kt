package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.matchers.shouldBe

/**
 * An activated ability's "exile a permanent you control" cost (`Costs.ExilePermanentsFixed`) is a
 * full zone transition — counters and Auras fall off the exiled permanent (CR 400.7) — and the
 * resolving ability names what it exiled as `EffectTarget.ExiledAsCost`, read as it last existed on
 * the battlefield (CR 608.2h): its power with counters, the copiable values it had while copying
 * something else, and a token that has since ceased to exist.
 */
class ExileFromBattlefieldCostTest : ScenarioTestBase() {

    private val mirror = card("Exile Mirror") {
        manaCost = "{1}"
        typeLine = "Artifact Creature — Shapeshifter"
        power = 1
        toughness = 1
        oracleText = "Exile another creature you control: This creature becomes a copy of the exiled creature."
        activatedAbility {
            cost = Costs.ExilePermanentsFixed(1, GameObjectFilter.Creature.youControl(), excludeSelf = true)
            effect = Effects.EachPermanentBecomesCopyOfTarget(
                target = EffectTarget.ExiledAsCost(),
                affected = EffectTarget.Self,
            )
        }
    }

    private val gauge = card("Exile Gauge") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        oracleText = "Exile a creature you control: Draw cards equal to the exiled creature's power."
        activatedAbility {
            cost = Costs.ExilePermanentsFixed(1, GameObjectFilter.Creature.youControl())
            effect = Effects.DrawCards(
                DynamicAmounts.propertyOf(EffectTarget.ExiledAsCost(), EntityNumericProperty.Power)
            )
        }
    }

    private val aura = card("Exile Cost Aura") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
    }

    private fun ScenarioTestBase.TestGame.activate(source: EntityId, exiled: EntityId?) {
        val ability = cardRegistry.getCard(state.getEntity(source)!!.get<CardComponent>()!!.cardDefinitionId)!!
            .activatedAbilities.single()
        execute(
            ActivateAbility(
                player1Id, source, ability.id,
                costPayment = exiled?.let { AdditionalCostPayment(exiledCards = listOf(it)) },
            )
        ).error shouldBe null
        resolveStack()
    }

    private fun ScenarioTestBase.TestGame.nameOf(id: EntityId) = state.getEntity(id)?.get<CardComponent>()?.name

    init {
        cardRegistry.register(listOf(mirror, gauge, aura))

        fun board(vararg creatures: String, token: String? = null) = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, "Exile Gauge")
            .apply { creatures.forEach { withCardOnBattlefield(1, it) } }
            .apply { token?.let { withCardOnBattlefield(1, it, isToken = true) } }
            .apply { repeat(6) { withCardInLibrary(1, "Island") } }
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("the exiled permanent leaves with a full zone transition, and its power is read as it last was") {
            val game = board().withCardOnBattlefield(1, "Centaur Courser")
                .withCardAttachedTo(1, "Exile Cost Aura", "Centaur Courser")
                .build()
            val courser = game.findPermanent("Centaur Courser")!!
            game.state = game.state.updateEntity(courser) {
                it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
            }
            val hand = game.handSize(1)
            game.activate(game.findPermanent("Exile Gauge")!!, courser)

            game.handSize(1) shouldBe hand + 4
            game.isInExile(1, "Centaur Courser") shouldBe true
            game.state.getEntity(courser)?.get<CountersComponent>() shouldBe null
            game.findPermanent("Exile Cost Aura") shouldBe null
            game.isInGraveyard(1, "Exile Cost Aura") shouldBe true
        }

        test("with only one creature to exile the payment is forced and still recorded") {
            val game = board("Savannah Lions").build()
            val hand = game.handSize(1)
            game.activate(game.findPermanent("Exile Gauge")!!, exiled = null)
            game.handSize(1) shouldBe hand + 1
        }

        test("a creature that was copying something is copied as what it was copying") {
            val game = board("Exile Mirror", "Exile Mirror", "Centaur Courser").build()
            val (first, second) = game.findPermanents("Exile Mirror")
            game.activate(first, game.findPermanent("Centaur Courser")!!)
            game.nameOf(first) shouldBe "Centaur Courser"

            game.activate(second, first)
            game.nameOf(second) shouldBe "Centaur Courser"
            game.state.projectedState.getPower(second) shouldBe 3
        }

        test("an exiled token that ceased to exist is still copied from its last-known values") {
            val game = board("Exile Mirror", token = "Savannah Lions").build()
            val lions = game.findPermanent("Savannah Lions")!!
            val mirrorId = game.findPermanent("Exile Mirror")!!
            game.activate(mirrorId, lions)

            game.state.getEntity(lions) shouldBe null
            game.nameOf(mirrorId) shouldBe "Savannah Lions"
            game.state.projectedState.getPower(mirrorId) shouldBe 1
        }

        test("an exile-another cost can't be paid with the source itself") {
            val game = board("Exile Mirror", "Centaur Courser").build()
            val mirrorId = game.findPermanent("Exile Mirror")!!
            val ability = mirror.activatedAbilities.single()
            val result = game.execute(
                ActivateAbility(
                    game.player1Id, mirrorId, ability.id,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(mirrorId)),
                )
            )
            (result.error != null) shouldBe true
            game.findPermanent("Exile Mirror") shouldBe mirrorId
        }
    }
}
