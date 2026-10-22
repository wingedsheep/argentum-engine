package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.PlayerIs
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.AttachmentKind
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The two reads Curse of Surveillance composes: `AttachmentCount(CURSE)` off a *player* — players
 * hold their Auras' attachment links like permanents — and `PlayerIs(_, Player.EnchantedPlayer)`,
 * which names the player the source Aura is attached to.
 */
class PlayerAttachedCursesTest : FunSpec({
    val predicateEvaluator = PredicateEvaluator(cardRegistry = null)
    val amounts = predicateEvaluator.amounts
    val conditions = predicateEvaluator.conditions
    val you = EntityId.generate()
    val opponent = EntityId.generate()

    /** Auras controlled by [you], each with [subtypes], attached to [host]. */
    fun stateWith(host: EntityId, vararg subtypes: Set<Subtype>): Pair<GameState, List<EntityId>> {
        var state = GameState(turnOrder = listOf(you, opponent))
            .withEntity(you, ComponentContainer())
            .withEntity(opponent, ComponentContainer())
        val ids = subtypes.map { EntityId.generate() }
        subtypes.forEachIndexed { index, types ->
            val aura = ComponentContainer()
                .with(
                    CardComponent(
                        cardDefinitionId = "Aura$index",
                        name = "Aura$index",
                        manaCost = ManaCost.parse("{1}"),
                        typeLine = TypeLine(cardTypes = setOf(CardType.ENCHANTMENT), subtypes = types),
                        ownerId = you
                    )
                )
                .with(OwnerComponent(you))
                .with(ControllerComponent(you))
                .with(AttachedToComponent(host))
            state = state.withEntity(ids[index], aura).addToZone(ZoneKey(you, Zone.BATTLEFIELD), ids[index])
        }
        state = state.updateEntity(host) { it.with(AttachmentsComponent(ids)) }
        return state to ids
    }

    val curse = setOf(Subtype.AURA, Subtype.CURSE)
    val plainAura = setOf(Subtype.AURA)

    test("counts only the Curses attached to the player") {
        val (state, ids) = stateWith(opponent, curse, curse, plainAura)
        val context = EffectContext(sourceId = ids[0], controllerId = you)
        amounts.evaluate(state, DynamicAmounts.cursesAttachedToEnchantedPlayer(), context) shouldBe 2
        amounts.evaluate(
            state,
            DynamicAmount.EntityProperty(EffectTarget.EnchantedPermanent, EntityNumericProperty.AttachmentCount(AttachmentKind.AURA)),
            context
        ) shouldBe 3
    }

    test("a player enchanted only by non-Curse Auras counts zero") {
        val (state, ids) = stateWith(opponent, plainAura)
        val context = EffectContext(sourceId = ids[0], controllerId = you)
        amounts.evaluate(state, DynamicAmounts.cursesAttachedToEnchantedPlayer(), context) shouldBe 0
    }

    test("PlayerIs resolves enchanted player off the source's attachment") {
        val (state, ids) = stateWith(opponent, curse)
        val notEnchanted = NotCondition(PlayerIs(Player.Candidate, Player.EnchantedPlayer))
        fun candidate(player: EntityId) = EffectContext(sourceId = ids[0], controllerId = you, candidatePlayerId = player)
        conditions.evaluate(state, notEnchanted, candidate(opponent)) shouldBe false
        conditions.evaluate(state, notEnchanted, candidate(you)) shouldBe true
    }

    test("an Aura attached to a permanent names no enchanted player") {
        val permanent = EntityId.generate()
        val (withHost, ids) = stateWith(permanent, curse)
        val state = withHost.withEntity(permanent, ComponentContainer().with(AttachmentsComponent(ids)))
        val context = EffectContext(sourceId = ids[0], controllerId = you, candidatePlayerId = opponent)
        conditions.evaluate(state, PlayerIs(Player.Candidate, Player.EnchantedPlayer), context) shouldBe false
    }
})
