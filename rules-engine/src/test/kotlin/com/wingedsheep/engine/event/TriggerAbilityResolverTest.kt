package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TriggerAbilityResolverTest : FunSpec({
    test("both lookup paths retain grant order, duplicate abilities, and the ungranted base list") {
        fun ability(name: String) = TriggeredAbility(
            AbilityId(name), Triggers.Dies.event,
            effect = LoseLifeEffect(1, EffectTarget.PlayerRef(Player.You)),
        )
        val owner = EntityId.of("owner")
        val targetId = EntityId.of("target")
        val providerId = EntityId.of("provider")
        val base = listOf(ability("base"))
        val temporary = ability("temporary")
        val static = ability("static")
        val grant = GrantTriggeredAbility(static, GroupFilter.AllCreatures)
        val target = CardDefinition.creature("Trigger Target", ManaCost.ZERO, subtypes = emptySet(), power = 1, toughness = 1)
        val provider = CardDefinition.enchantment("Trigger Provider", ManaCost.ZERO,
            script = CardScript(staticAbilities = listOf(grant)))
        val registry = CardRegistry().apply { register(listOf(target, provider)) }
        val abilities = AbilityRegistry().apply { register(target.name, base) }
        val resolver = TriggerAbilityResolver(registry, abilities)
        val zone = ZoneKey(owner, Zone.BATTLEFIELD)
        val ungranted = GameState(
            entities = mapOf(targetId to CardEntityFactory.create(target, owner)),
            zones = mapOf(zone to listOf(targetId)),
        )
        (resolver.getTriggeredAbilities(targetId, target.name, ungranted) === base) shouldBe true
        (resolver.getTriggeredAbilitiesWithProviders(targetId, target.name, ungranted, emptyList()) === base) shouldBe true

        val granted = ungranted.copy(
            entities = ungranted.entities + (providerId to CardEntityFactory.create(provider, owner)),
            zones = mapOf(zone to listOf(targetId, providerId)),
            grantedTriggeredAbilities = List(2) { GrantedTriggeredAbility(targetId, temporary, Duration.Permanent) },
        )
        val expected = base + listOf(temporary, temporary, static)
        resolver.getTriggeredAbilities(targetId, target.name, granted) shouldBe expected
        resolver.getTriggeredAbilitiesWithProviders(
            targetId, target.name, granted,
            listOf(TriggerIndex.GrantProviderEntry(grant, owner, providerId)),
        ) shouldBe expected
    }
})
