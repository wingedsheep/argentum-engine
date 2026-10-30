package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mantle of the Wolf
 * {3}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +4/+4.
 * When this Aura is put into a graveyard from the battlefield, create two 2/2 green Wolf creature tokens.
 *
 * Audacity's shape: a static [ModifyStats] on the enchanted creature and a self `dies()` trigger
 * ("put into a graveyard from the battlefield", CR 700.4). An Aura spell that fizzles goes to the
 * graveyard from the stack, so it doesn't trigger (the ruling).
 */
val MantleOfTheWolf = card("Mantle of the Wolf") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +4/+4.\n" +
        "When this Aura is put into a graveyard from the battlefield, create two 2/2 green Wolf creature tokens."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(4, 4)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/4/1/411f4bf6-7f09-4e24-b483-0068d2f974e5.jpg?1783931451"
        )
        description = "When this Aura is put into a graveyard from the battlefield, create two 2/2 green Wolf " +
            "creature tokens."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "178"
        artist = "Simon Dominic"
        flavorText = "\"Be fearless, for a furious rebirth awaits you.\"\n—Nylea, god of the hunt"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83a786fa-4b86-40f7-ac58-1a05fb38fcdb.jpg?1783931536"
        ruling(
            "2020-01-24",
            "If the creature Mantle of the Wolf would enchant is an illegal target by the time the Aura spell " +
                "resolves, the entire spell doesn't resolve. It's put into your graveyard from the stack, not the " +
                "battlefield, so its ability won't trigger."
        )
    }
}
