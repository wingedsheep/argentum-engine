package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Rancor
 * {G}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+0 and has trample.
 * When this Aura is put into a graveyard from the battlefield, return it to its owner's hand.
 *
 * "Put into a graveyard from the battlefield" is `Triggers.self.dies()` (same event for a
 * noncreature permanent), the Spine of Ish Sah idiom — including state-based Aura cleanup when the
 * enchanted creature leaves.
 */
val Rancor = card("Rancor") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+0 and has trample.\n" +
        "When this Aura is put into a graveyard from the battlefield, return it to its owner's hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Kev Walker"
        flavorText = "Hatred outlives the hateful."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59e256c2-38df-4012-9308-ce17dd889e5f.jpg?1783946229"
    }
}
