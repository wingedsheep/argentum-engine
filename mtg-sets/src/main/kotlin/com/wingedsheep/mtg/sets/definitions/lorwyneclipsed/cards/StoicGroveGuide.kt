package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Stoic Grove-Guide
 * {4}{B/G}
 * Creature — Elf Druid
 * 5/4
 *
 * {1}{B/G}, Exile this card from your graveyard: Create a 2/2 black and green
 * Elf creature token. Activate only as a sorcery.
 */
val StoicGroveGuide = card("Stoic Grove-Guide") {
    manaCost = "{4}{B/G}"
    typeLine = "Creature — Elf Druid"
    power = 5
    toughness = 4
    oracleText = "{1}{B/G}, Exile this card from your graveyard: Create a 2/2 black and green Elf creature token. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B/G}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK, Color.GREEN),
            creatureTypes = setOf("Elf"),
            imageUri = "https://cards.scryfall.io/normal/front/3/9/39b36f22-21f9-44fe-8a49-bdc859503342.jpg?1767955588"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "243"
        artist = "Tran Nguyen"
        flavorText = "Safewrights stationed within the Creakwood do not fear Shadowmoor's encroaching dark. They embrace it."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d5a3e25-c17a-47b1-a36d-d24d50e5bab3.jpg?1767984401"
    }
}
