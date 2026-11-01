package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nikya of the Old Ways
 * {3}{R}{G}
 * Legendary Creature — Centaur Druid
 * 5/5
 * You can't cast noncreature spells.
 * Whenever you tap a land for mana, add one mana of any type that land produced.
 *
 * The prohibition is [PlayersCantCastSpells] turned inward (`affected = Player.You`); the mana
 * doubling is the Heartbeat of Spring shape scoped to your lands, with `color = null` mirroring
 * the type the land produced.
 */
val NikyaOfTheOldWays = card("Nikya of the Old Ways") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Centaur Druid"
    power = 5
    toughness = 5
    oracleText = "You can't cast noncreature spells.\n" +
        "Whenever you tap a land for mana, add one mana of any type that land produced."

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.You,
            spellFilter = GameObjectFilter.Noncreature
        )
    }

    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Land.youControl(),
            color = null // mirror the produced type ("any type that land produced")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Ryan Pancoast"
        flavorText = "\"The hooves of the Raze-Boar will trample the weak—and their city—to dust!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dcdad71-323e-41e0-a1b3-9fd5b753e71c.jpg?1783933642"
        ruling("2019-01-25", "The types of mana are white, blue, black, red, green, and colorless.")
        ruling("2019-01-25", "If you tap a land for more than one mana, you choose one type that was produced and add one mana of that type.")
        ruling("2019-01-25", "Nikya doesn't care about any restrictions or riders your lands put on the mana they produce, such as those of Unclaimed Territory and Guildmages' Forum. It just produces one mana of the appropriate type, with no restrictions or riders.")
    }
}
