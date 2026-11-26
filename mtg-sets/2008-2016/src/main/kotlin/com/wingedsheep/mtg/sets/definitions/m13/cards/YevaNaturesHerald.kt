package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType

/**
 * Yeva, Nature's Herald
 * {2}{G}{G}
 * Legendary Creature — Elf Shaman
 * 4/4
 * Flash
 * You may cast green creature spells as though they had flash.
 *
 * Prophet of Kruphix's controller-only [GrantFlashToSpellType], narrowed to green creature spells.
 */
val YevaNaturesHerald = card("Yeva, Nature's Herald") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Shaman"
    power = 4
    toughness = 4
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "You may cast green creature spells as though they had flash."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Creature.withColor(Color.GREEN),
            controllerOnly = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "197"
        artist = "Eric Deschamps"
        flavorText = "\"I can call an army to my side in the blink of an eye. Best not blink.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80acb6dc-a9bd-4f12-9025-623416bdfc32.jpg?1783940469"
        ruling("2012-07-01", "You still pay the costs for green creature spells you cast.")
        ruling(
            "2012-07-01",
            "Yeva's ability applies to green creature cards in any zone, provided something is allowing you " +
                "to cast them. For example, if the top card of your library is a green creature card and you " +
                "control Garruk's Horde, you can cast that card as though it had flash."
        )
    }
}
