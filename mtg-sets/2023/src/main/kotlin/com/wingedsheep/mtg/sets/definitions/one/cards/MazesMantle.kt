package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Maze's Mantle
 * {2}{G}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * When this Aura enters, if enchanted creature has toxic, that creature gains hexproof until end
 * of turn.
 * Enchanted creature gets +2/+2.
 *
 * "If enchanted creature has toxic" is an intervening-if (checked on trigger and on resolution),
 * read through `withKeyword(TOXIC)` — which matches printed or granted toxic N off the projected
 * `TOXIC_<n>` keyword.
 */
val MazesMantle = card("Maze's Mantle") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "When this Aura enters, if enchanted creature has toxic, that creature gains hexproof until " +
        "end of turn.\n" +
        "Enchanted creature gets +2/+2."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.EnchantedPermanentMatches(
            GameObjectFilter.Creature.withKeyword(Keyword.TOXIC)
        )
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.EnchantedCreature)
        description = "When this Aura enters, if enchanted creature has toxic, that creature gains " +
            "hexproof until end of turn."
    }

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "L.A. Draws"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc79106b-9aa2-4add-b9ca-e3c7aafd9821.jpg?1783918013"
    }
}
