package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Gloom Ripper
 * {3}{B}{B}
 * Creature — Elf Assassin
 * 4/4
 *
 * When this creature enters, target creature you control gets +X/+0 until end of turn
 * and up to one target creature an opponent controls gets -0/-X until end of turn,
 * where X is the number of Elves you control plus the number of Elf cards in your graveyard.
 */
val GloomRipper = card("Gloom Ripper") {
    manaCost = "{3}{B}{B}"
    typeLine = "Creature — Elf Assassin"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, target creature you control gets +X/+0 until end of turn " +
        "and up to one target creature an opponent controls gets -0/-X until end of turn, where X is " +
        "the number of Elves you control plus the number of Elf cards in your graveyard."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield

        val ally = target(
            "creature you control",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.youControl()))
        )
        val enemy = target(
            "creature an opponent controls",
            TargetCreature(
                optional = true,
                filter = TargetFilter(GameObjectFilter.Creature.opponentControls())
            )
        )

        val elfCount = DynamicAmount.Add(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.withSubtype(Subtype.ELF)).count(),
            DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.withSubtype(Subtype.ELF)).count()
        )

        effect = Effects.ModifyStats(elfCount, DynamicAmount.Fixed(0), ally)
            .then(Effects.ModifyStats(DynamicAmount.Fixed(0), DynamicAmount.Subtract(DynamicAmount.Fixed(0), elfCount), enemy))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Annie Stegg"
        flavorText = "\"I will not rest until I have excised the rot from this land.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9ee5d4f-05d1-4e3a-b8d6-f22d0fddedf7.jpg?1767952022"
    }
}
