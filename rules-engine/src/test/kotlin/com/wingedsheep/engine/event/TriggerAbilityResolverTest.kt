package com.wingedsheep.engine.event

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.ManaCost
import io.kotest.matchers.collections.shouldContain
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Color
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
            AbilityId(name), Triggers.self.dies().event,
            effect = LoseLifeEffect(1, EffectTarget.PlayerRef(Player.You)),
        )
        val owner = EntityId.of("owner")
        val targetId = EntityId.of("target")
        val providerId = EntityId.of("provider")
        val base = listOf(ability("base"))
        val temporary = ability("temporary")
        val static = ability("static")
        val grant = GrantTriggeredAbility(static, GroupFilter.AllCreatures)
        val target = CardDefinition.creature("Trigger Target", ManaCost.ZERO, subtypes = emptySet(), power = 1, toughness = 1,
            script = CardScript(triggeredAbilities = base))
        val provider = CardDefinition.enchantment("Trigger Provider", ManaCost.ZERO,
            script = CardScript(staticAbilities = listOf(grant)))
        val registry = CardRegistry().apply { register(listOf(target, provider)) }
        val abilities = AbilityRegistry().apply { register(target.name, base) }
        val resolver = TriggerAbilityResolver(registry, abilities, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        val zone = ZoneKey(owner, Zone.BATTLEFIELD)
        val ungranted = GameState(
            entities = mapOf(targetId to CardEntityFactory.create(target, owner)),
            zones = mapOf(zone to listOf(targetId)),
        )
        (resolver.getTriggeredAbilities(targetId, target.name, ungranted) === base) shouldBe true
        (resolver.getTriggeredAbilitiesWithProviders(targetId, target.name, ungranted, emptyList()) === base) shouldBe true

        val fallback = TriggerAbilityResolver(registry, AbilityRegistry(), predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        (fallback.getTriggeredAbilities(targetId, target.name, ungranted) === base) shouldBe true
        (fallback.getTriggeredAbilitiesWithProviders(targetId, target.name, ungranted, emptyList()) === base) shouldBe true

        val granted = ungranted.copy(
            entities = ungranted.entities + (providerId to CardEntityFactory.create(provider, owner)),
            zones = mapOf(zone to listOf(targetId, providerId)),
            grantedTriggeredAbilities = listOf(
                GrantedTriggeredAbility(providerId, ability("unrelated"), Duration.Permanent),
                GrantedTriggeredAbility(targetId, temporary, Duration.Permanent),
                GrantedTriggeredAbility(targetId, ability("expired"), Duration.WhileAffectedTapped),
                GrantedTriggeredAbility(targetId, temporary, Duration.Permanent),
            ),
        )
        val expected = base + listOf(temporary, temporary, static)
        resolver.getTriggeredAbilities(targetId, target.name, granted) shouldBe expected
        resolver.getTriggeredAbilitiesWithProviders(
            targetId, target.name, granted,
            listOf(TriggerIndex.GrantProviderEntry(grant, owner, providerId)),
        ) shouldBe expected
    }

    test("static grants evaluate their whole filter: a colour predicate gates both the trigger and the ward") {
        val owner = EntityId.of("owner")
        val blueId = EntityId.of("blue")
        val greenId = EntityId.of("green")
        val providerId = EntityId.of("provider")
        val static = TriggeredAbility(
            AbilityId("loot"), Triggers.self.becomesTapped().event,
            effect = LoseLifeEffect(1, EffectTarget.PlayerRef(Player.You)),
        )
        val blueCreatures = GroupFilter(GameObjectFilter.Creature.withColor(Color.BLUE).youControl(), excludeSelf = true)
        val grant = GrantTriggeredAbility(static, blueCreatures)
        val ward = GrantWard(WardCost.Mana("{2}"), blueCreatures)
        val blue = CardDefinition.creature("Blue Target", ManaCost.parse("{U}"), subtypes = emptySet(), power = 1, toughness = 1)
        val green = CardDefinition.creature("Green Target", ManaCost.parse("{G}"), subtypes = emptySet(), power = 1, toughness = 1)
        // Blue itself, so excludeSelf is what keeps it off its own grant.
        val provider = CardDefinition.creature("Blue Provider", ManaCost.parse("{U}"), subtypes = emptySet(), power = 1, toughness = 1,
            script = CardScript(staticAbilities = listOf(grant, ward)))
        val registry = CardRegistry().apply { register(listOf(blue, green, provider)) }
        val resolver = TriggerAbilityResolver(registry, AbilityRegistry(), predicateEvaluator = PredicateEvaluator(cardRegistry = registry))
        val state = GameState(
            entities = mapOf(
                blueId to CardEntityFactory.create(blue, owner),
                greenId to CardEntityFactory.create(green, owner),
                providerId to CardEntityFactory.create(provider, owner),
            ),
            zones = mapOf(ZoneKey(owner, Zone.BATTLEFIELD) to listOf(blueId, greenId, providerId)),
        )
        val providers = listOf(TriggerIndex.GrantProviderEntry(grant, owner, providerId))
        fun ids(list: List<TriggeredAbility>) = list.map { it.id }.filter { it == static.id }

        resolver.getTriggeredAbilities(blueId, blue.name, state).map { it.id } shouldContain static.id
        resolver.getTriggeredAbilitiesWithProviders(blueId, blue.name, state, providers).map { it.id } shouldContain static.id
        resolver.getWardTriggeredAbilities(blueId, blue.name, state).size shouldBe 1

        for ((id, name) in listOf(greenId to green.name, providerId to provider.name)) {
            ids(resolver.getTriggeredAbilities(id, name, state)) shouldBe emptyList()
            ids(resolver.getTriggeredAbilitiesWithProviders(id, name, state, providers)) shouldBe emptyList()
            resolver.getWardTriggeredAbilities(id, name, state) shouldBe emptyList()
        }
    }
})

