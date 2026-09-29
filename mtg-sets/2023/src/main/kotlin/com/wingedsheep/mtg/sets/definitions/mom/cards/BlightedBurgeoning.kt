package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Blighted Burgeoning — March of the Machine #177
 * {2}{G} · Enchantment — Aura
 *
 * Enchant land
 * When this Aura enters, incubate 2.
 * Whenever enchanted land is tapped for mana, its controller adds an additional one mana of any color.
 *
 * The mana rider is Fertile Ground's [AdditionalManaOnTap] with `anyColor = true`.
 */
val BlightedBurgeoning = card("Blighted Burgeoning") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "When this Aura enters, incubate 2. (Create an Incubator token with two +1/+1 counters on it and " +
        "\"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)\n" +
        "Whenever enchanted land is tapped for mana, its controller adds an additional one mana of any color."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Incubate(2)
    }

    staticAbility {
        ability = AdditionalManaOnTap(amount = DynamicAmounts.fixed(1), anyColor = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177"
        artist = "Piotr Foksowicz"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/007324b3-b8a9-4a32-a5a1-ad78f3a07bc3.jpg?1783916976"
    }
}
