package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Storm-Kiln Artist — Strixhaven: School of Mages #115
 * {3}{R} · Creature — Dwarf Shaman · 2/2
 *
 * This creature gets +1/+0 for each artifact you control.
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, create a Treasure token.
 *
 * The artifact-count pump is [NimShrieker]'s continuously recomputed [GrantDynamicStats]; the
 * magecraft trigger is [ArchmageEmeritus]'s `castsOrCopies`. Each Treasure it makes grows it.
 */
val StormKilnArtist = card("Storm-Kiln Artist") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Shaman"
    power = 2
    toughness = 2
    oracleText = "This creature gets +1/+0 for each artifact you control.\n" +
        "Magecraft — Whenever you cast or copy an instant or sorcery spell, create a Treasure token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.CreateTreasure(1)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, create a Treasure token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Manuel Castañón"
        flavorText = "A captured elemental makes for a potent, albeit unstable, power source."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa96b8dc-233a-4884-ab84-235cbc7df0b6.jpg?1783927352"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "Some effects instruct you to copy an instant or sorcery card in a zone other than the stack. These copies do not cause magecraft abilities to trigger. However, most effects that do this also allow you to cast the copy, and casting the copy will cause magecraft abilities to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
