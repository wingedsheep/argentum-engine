package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AnimateLandGroup
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Nissa's Zendikon — Zendikar Rising #197 (canonical printing)
 * {3}{G} · Enchantment — Aura
 *
 * Enchant land
 * Enchanted land is a 4/4 Elemental creature with reach and haste. It's still a land.
 * When enchanted land dies, return that card to its owner's hand.
 *
 * The animation is [AnimateLandGroup] over the attached land (colorless — the printed text adds no
 * colour), with reach and haste granted alongside. The dies trigger looks back at what the Aura
 * was attached to, so "that card" is the land card in its owner's graveyard.
 */
val NissasZendikon = card("Nissa's Zendikon") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land is a 4/4 Elemental creature with reach and haste. It's still a land.\n" +
        "When enchanted land dies, return that card to its owner's hand."
    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Land))

    staticAbility {
        ability = AnimateLandGroup(
            filter = GroupFilter(GameObjectFilter.Land, scope = Scope.AttachedTo),
            power = 4,
            toughness = 4,
            creatureSubtypes = listOf("Elemental")
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH, GroupFilter(GameObjectFilter.Land, scope = Scope.AttachedTo))
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter(GameObjectFilter.Land, scope = Scope.AttachedTo))
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.ReturnToHand(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "197"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd37ea0e-ff72-4522-aa5b-d46bff832a34.jpg?1783929336"
    }
}
