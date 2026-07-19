package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Super Speed (MSH #154) — {R} Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * When this Aura enters, enchanted creature gains first strike until end of turn.
 * Enchanted creature gets +1/+0 and has haste.
 *
 * Implementation notes:
 * - Flash on the Aura itself is the printed [Keyword.FLASH] (it's the *Aura* that gains flash,
 *   not the enchanted creature), so it's declared with [keywords], not a static grant.
 * - The first-strike grant is a one-shot ETB [Effects.GrantKeyword] on
 *   [EffectTarget.EnchantedCreature] with the facade's default `Duration.EndOfTurn` — a
 *   temporary grant, distinct from the two permanent statics below.
 * - The +1/+0 and haste are Layer 7c / Layer 6 statics scoped to [Filters.EnchantedCreature].
 */
val SuperSpeed = card("Super Speed") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "When this Aura enters, enchanted creature gains first strike until end of turn.\n" +
        "Enchanted creature gets +1/+0 and has haste."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = ModifyStats(1, 0, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Nereida"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2aa2e596-e5e0-4c60-8eea-14adda1cdaae.jpg?1783902924"
    }
}
