package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Axgard Armory
 * Land
 * This land enters tapped.
 * {T}: Add {W}.
 * {1}{R}{R}{W}, {T}, Sacrifice this land: Search your library for an Aura card and/or an Equipment
 * card, reveal them, put them into your hand, then shuffle.
 *
 * One of Kaldheim's five "realm" lands ([GatesOfIstfell]'s shape). "An Aura card and/or an
 * Equipment card" is two independent up-to-one searches ([com.wingedsheep.mtg.sets.definitions.lgn.cards.CorpseHarvester]'s
 * shape) — either may find nothing — with a single shuffle after the second.
 */
val AxgardArmory = card("Axgard Armory") {
    manaCost = ""
    colorIdentity = "RW"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {W}.\n" +
        "{1}{R}{R}{W}, {T}, Sacrifice this land: Search your library for an Aura card and/or an Equipment card, reveal them, put them into your hand, then shuffle."

    replacementEffect(EntersTapped())

    // {T}: Add {W}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE, 1)
        manaAbility = true
    }

    // {1}{R}{R}{W}, {T}, Sacrifice this land: Search for an Aura card and/or an Equipment card.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}{R}{W}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Enchantment.withSubtype("Aura"),
            reveal = true,
            shuffleAfter = false
        ) then Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.withSubtype("Equipment"),
            reveal = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9c77d35-8418-48fb-b7d7-7cfa763545c5.jpg?1783928180"
    }
}
