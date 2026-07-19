package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ghost, Spectral Saboteur
 * {2}{U/B}
 * Legendary Creature — Human Rogue Villain — Uncommon (MSH #214)
 * 2/2
 *
 * "Flash"
 * "Intangibility — Ghost can't be blocked."
 *
 * Implementation note: "Intangibility" is an ability word (flavor only, no rules meaning), so the
 * card is a keyword body — [Keyword.FLASH] plus the [AbilityFlag.CANT_BE_BLOCKED] evasion flag.
 */
val GhostSpectralSaboteur = card("Ghost, Spectral Saboteur") {
    manaCost = "{2}{U/B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Rogue Villain"
    power = 2
    toughness = 2
    oracleText = "Flash\nIntangibility — Ghost can't be blocked."

    keywords(Keyword.FLASH)
    flags(AbilityFlag.CANT_BE_BLOCKED)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Lucio Parrillo"
        flavorText = "\"Do not speak foolishly of ghosts . . . or you may soon become one.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c22bb4d5-6b6f-4c43-b005-48ae4042739d.jpg?1783902902"
    }
}
