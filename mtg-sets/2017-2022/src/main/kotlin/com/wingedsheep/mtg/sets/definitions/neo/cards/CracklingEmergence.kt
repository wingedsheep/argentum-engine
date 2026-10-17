package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AnimateLandGroup
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ReplaceDestructionWith
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Crackling Emergence — Kamigawa: Neon Dynasty #136 (canonical printing)
 * {1}{R} · Enchantment — Aura
 *
 * Enchant land you control
 * Enchanted land is a 3/3 red Spirit creature with haste. It's still a land.
 * If enchanted land would be destroyed, instead sacrifice this Aura and that land gains
 * indestructible until end of turn.
 *
 * The last line is a [ReplaceDestructionWith] over the enchanted land: it covers lethal damage
 * as well as destroy effects, but not a sacrifice of the land. "That land" is the replacement's
 * triggering entity.
 */
val CracklingEmergence = card("Crackling Emergence") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land you control\n" +
        "Enchanted land is a 3/3 red Spirit creature with haste. It's still a land.\n" +
        "If enchanted land would be destroyed, instead sacrifice this Aura and that land gains " +
        "indestructible until end of turn."
    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Land.youControl()))

    staticAbility {
        ability = AnimateLandGroup(
            filter = GroupFilter(GameObjectFilter.Land, scope = Scope.AttachedTo),
            power = 3,
            toughness = 3,
            creatureSubtypes = listOf("Spirit"),
            colors = setOf(Color.RED)
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter(GameObjectFilter.Land, scope = Scope.AttachedTo))
    }

    replacementEffect(
        ReplaceDestructionWith(
            replacementEffect = SacrificeSelfEffect then
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.TriggeringEntity),
            appliesTo = EventPattern.DestroyEvent(GameObjectFilter.Land.attachedToBySource())
        )
    )

    metadata {
        rarity = Rarity.COMMON
        flavorText = "The transition from spiritual to material is sometimes as shocking as it is beautiful."
        collectorNumber = "136"
        artist = "Jason Kang"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f77f987-ebb7-4105-a67c-0987f50fc676.jpg?1783923870"
    }
}
