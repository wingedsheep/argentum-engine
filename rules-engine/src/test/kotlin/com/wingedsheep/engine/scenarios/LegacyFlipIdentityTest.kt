package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.FlippedEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.handlers.effects.copy.withRestoredFlipSide
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FlippedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class LegacyFlipIdentityTest : FunSpec({
    val front = card("Legacy Flip Student") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Human"
        power = 1; toughness = 2
        activatedAbility { cost = Costs.Free; effect = Effects.Flip() }
    }
    val back = card("Legacy Flip Teacher") {
        typeLine = "Legendary Creature — Wizard"
        power = 4; toughness = 5
        activatedAbility { cost = Costs.Free; effect = Effects.GainLife(3) }
    }
    val flipCard = CardDefinition.flipCard(front, back)
    val copier = card("Legacy Flip Copier") {
        manaCost = "{U}"
        typeLine = "Creature — Shapeshifter"
        power = 2; toughness = 2
        activatedAbility {
            cost = Costs.Free
            val subject = target(TargetFilter.Creature)
            effect = Effects.EachPermanentBecomesCopyOfTarget(target = subject, affected = EffectTarget.Self)
        }
    }
    val codec = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(flipCard, copier))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun component(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!
    fun activate(d: GameTestDriver, id: EntityId, ability: AbilityId, targets: List<ChosenTarget> = emptyList()) {
        d.submit(ActivateAbility(d.player1, id, ability, targets)).error shouldBe null
        d.bothPass().error shouldBe null
    }
    // Both fields were absent in snapshots captured before flip halves became frozen identity data.
    fun oldShape(value: JsonElement): JsonElement = when (value) {
        is JsonObject -> JsonObject((value - "flipSide" - "copyNumericKeywords")
            .mapValues { oldShape(it.value) })
        is JsonArray -> JsonArray(value.map(::oldShape))
        else -> value
    }
    fun reloadLegacy(d: GameTestDriver) {
        val legacy = oldShape(codec.parseToJsonElement(codec.encodeToString(d.state)))
        d.replaceState(codec.decodeFromString<GameState>(legacy.toString()))
    }

    test("an upright flip card loaded without frozen halves still resolves its printed flip ability") {
        val d = driver()
        val id = d.putPermanentOnBattlefield(d.player1, flipCard.name)
        reloadLegacy(d)
        component(d, id).flipSide shouldBe null
        activate(d, id, front.activatedAbilities.single().id)
        component(d, id).name shouldBe back.name
        component(d, id).manaCost shouldBe flipCard.manaCost
        component(d, id).colors shouldBe setOf(Color.GREEN)
        d.state.projectedState.getPower(id) shouldBe 4
        d.events.filterIsInstance<FlippedEvent>().single().entityId shouldBe id
        activate(d, id, back.activatedAbilities.single().id)
        d.getLifeTotal(d.player1) shouldBe 23
    }

    test("a copy of a loaded flipped permanent acquires its missing upright and alternative halves") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, flipCard.name)
        activate(d, source, front.activatedAbilities.single().id)
        reloadLegacy(d)
        d.state.getEntity(source)!!.get<FlippedComponent>()!!.unflippedCard.flipSide shouldBe null
        val copy = d.putPermanentOnBattlefield(d.player1, copier.name)
        activate(d, copy, copier.activatedAbilities.single().id, listOf(ChosenTarget.Permanent(source)))
        component(d, copy).name shouldBe front.name
        component(d, copy).flipSide!!.name shouldBe back.name
        d.state.getEntity(copy)!!.has<FlippedComponent>() shouldBe false
        activate(d, copy, front.activatedAbilities.single().id)
        component(d, copy).name shouldBe back.name
        d.state.projectedState.getToughness(copy) shouldBe 5
    }

    test("hydration keeps an existing frozen alternative instead of rebuilding its values") {
        val d = driver()
        val id = d.putPermanentOnBattlefield(d.player1, flipCard.name)
        val original = component(d, id)
        val modified = original.copy(flipSide = original.flipSide!!.copy(name = "Frozen Alternative"))
        modified.withRestoredFlipSide(d.cardRegistry) shouldBe modified
    }
})
