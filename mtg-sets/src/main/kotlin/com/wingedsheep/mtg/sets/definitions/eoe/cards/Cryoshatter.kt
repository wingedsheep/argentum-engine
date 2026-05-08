package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Zone

/**
 * Cryoshatter
 * {U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets -5/-0.
 * When enchanted creature becomes tapped or is dealt damage, destroy it.
 */
val Cryoshatter = card("Cryoshatter") {
    manaCost = "{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets -5/-0.\nWhen enchanted creature becomes tapped or is dealt damage, destroy it."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(-5, 0, GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.EnchantedPermanentBecomesTapped
        effect = MoveToZoneEffect(
            target = EffectTarget.EnchantedCreature,
            destination = Zone.GRAVEYARD,
            byDestruction = true
        )
    }

    triggeredAbility {
        trigger = Triggers.EnchantedCreatureTakesDamage
        effect = MoveToZoneEffect(
            target = EffectTarget.EnchantedCreature,
            destination = Zone.GRAVEYARD,
            byDestruction = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Jeremy Wilson"
        flavorText = "Illvoi are masters of the deep freeze, though conditions must be perfect to avoid going to pieces."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b62b1e2-9e43-4a66-a647-7e5de2871f2a.jpg?1752946762"
    }
}
