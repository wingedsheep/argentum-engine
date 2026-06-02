package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Glarb, Calamity's Augur
 * {B}{G}{U}
 * Legendary Creature — Frog Wizard Noble
 * 2/4
 * Deathtouch
 * You may look at the top card of your library any time.
 * You may play lands and cast spells with mana value 4 or greater from the top of your library.
 * {T}: Surveil 2.
 */
val GlarbCalamitysAugur = card("Glarb, Calamity's Augur") {
    manaCost = "{B}{G}{U}"
    colorIdentity = "UBG"
    typeLine = "Legendary Creature — Frog Wizard Noble"
    power = 2
    toughness = 4
    oracleText = "Deathtouch\nYou may look at the top card of your library any time.\nYou may play lands and cast spells with mana value 4 or greater from the top of your library.\n{T}: Surveil 2."

    keywords(Keyword.DEATHTOUCH)

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    staticAbility {
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(
            spellFilter = GameObjectFilter.Any.manaValueAtLeast(4)
        )
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = LibraryPatterns.surveil(2)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "215"
        artist = "Bram Sels"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffc70b2d-5a3a-49ea-97db-175a62248302.jpg?1721427068"
    }
}
