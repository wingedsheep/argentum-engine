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
 * Harmonious Emergence — Kamigawa: Neon Dynasty #190 (canonical printing)
 * {3}{G} · Enchantment — Aura
 *
 * Enchant land you control
 * Enchanted land is a 4/5 green Spirit creature with vigilance and haste. It's still a land.
 * If enchanted land would be destroyed, instead sacrifice this Aura and that land gains
 * indestructible until end of turn.
 *
 * The last line is a [ReplaceDestructionWith] over the enchanted land: it covers lethal damage
 * as well as destroy effects, but not a sacrifice of the land. "That land" is the replacement's
 * triggering entity.
 */
val HarmoniousEmergence = card("Harmonious Emergence") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land you control\n" +
        "Enchanted land is a 4/5 green Spirit creature with vigilance and haste. It's still a land.\n" +
        "If enchanted land would be destroyed, instead sacrifice this Aura and that land gains " +
        "indestructible until end of turn."
    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Land.youControl()))

    staticAbility {
        ability = AnimateLandGroup(
            filter = GroupFilter(GameObjectFilter.Land, scope = Scope.AttachedTo),
            power = 4,
            toughness = 5,
            creatureSubtypes = listOf("Spirit"),
            colors = setOf(Color.GREEN)
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter(GameObjectFilter.Land, scope = Scope.AttachedTo))
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
        flavorText = "Where the spirit and mortal realms overlap, the laws of reality are rewritten."
        collectorNumber = "190"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c92ff968-b436-4313-8375-8a3bb41f9892.jpg?1783923848"
    }
}
