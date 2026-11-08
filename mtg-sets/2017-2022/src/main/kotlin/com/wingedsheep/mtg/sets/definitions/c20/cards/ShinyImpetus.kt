package com.wingedsheep.mtg.sets.definitions.c20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Goaded
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Shiny Impetus
 * {2}{R}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+2 and is goaded.
 * Whenever enchanted creature attacks, you create a Treasure token.
 *
 * The same shape as Martial Impetus: [Goaded] scoped to the attached creature makes the Aura's
 * controller its goader for as long as the Aura stays on it, and `Triggers.attached.attacks()` is
 * the Aura's own trigger. The Treasure goes to the Aura's controller, not the attacking creature's
 * controller (2020-04-17 ruling) — a plain `CreateTreasure()` is the trigger controller's.
 */
val ShinyImpetus = card("Shiny Impetus") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2 and is goaded. (It attacks each combat if able and attacks a " +
        "player other than you if able.)\n" +
        "Whenever enchanted creature attacks, you create a Treasure token. (It's an artifact with " +
        "\"{T}, Sacrifice this token: Add one mana of any color.\")"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    staticAbility {
        ability = Goaded()
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0aa80931-0501-4712-a11b-563a3cd1e1c9.jpg?1783931212"
        ruling(
            "2020-04-17",
            "Shiny Impetus causes you to create a Treasure token, not the attacking creature's controller."
        )
    }
}
