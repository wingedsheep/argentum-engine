package com.wingedsheep.mtg.sets.definitions.bng.cards

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
 * Everflame Eidolon — a 1/1 firebreather on its own body; bestowed, the Aura's static hands the
 * host +1/+1 and the {R} pump lands on the enchanted creature instead. "If it's an Aura" is read
 * at resolution against the source's projected subtypes (bestow makes it an Aura in layer 4).
 */
val EverflameEidolon = card("Everflame Eidolon") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Spirit"
    power = 1
    toughness = 1
    oracleText = "Bestow {2}{R} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "{R}: This creature gets +1/+0 until end of turn. If it's an Aura, enchanted creature gets +1/+0 until end of turn instead.\n" +
        "Enchanted creature gets +1/+1."

    keywordAbility(KeywordAbility.bestow("{2}{R}"))

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.If(
            Conditions.EntityMatches(EffectTarget.Self, GameObjectFilter.Any.withSubtype("Aura")),
            then = Effects.ModifyStats(1, 0, EffectTarget.EnchantedCreature),
            otherwise = Effects.ModifyStats(1, 0, EffectTarget.Self)
        )
    }

    staticAbility { ability = ModifyStats(1, 1) }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a018d5ea-5229-40ca-b7c5-31d3e8ebb894.jpg"
    }
}
