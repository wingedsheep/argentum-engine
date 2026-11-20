package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Erebos's Emissary (Theros #86)
 *
 * Everflame Eidolon shape: "If this permanent is an Aura" is read at resolution against the
 * source's projected subtypes (bestow makes it an Aura), so the +2/+2 lands on the enchanted
 * creature instead of on this permanent.
 */
val ErebossEmissary = card("Erebos's Emissary") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Snake"
    power = 3
    toughness = 3
    oracleText = "Bestow {5}{B} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Discard a creature card: This creature gets +2/+2 until end of turn. If this permanent is an Aura, enchanted creature gets +2/+2 until end of turn instead.\n" +
        "Enchanted creature gets +3/+3."

    keywordAbility(KeywordAbility.bestow("{5}{B}"))

    activatedAbility {
        cost = Costs.Discard(GameObjectFilter.Creature)
        effect = Effects.If(
            Conditions.EntityMatches(EffectTarget.Self, GameObjectFilter.Any.withSubtype("Aura")),
            then = Effects.ModifyStats(2, 2, EffectTarget.EnchantedCreature),
            otherwise = Effects.ModifyStats(2, 2, EffectTarget.Self)
        )
    }

    staticAbility { ability = ModifyStats(3, 3) }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5aa96f63-d5c3-47d3-a54d-a67883406fc5.jpg?1783939780"
    }
}
