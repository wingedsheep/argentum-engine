package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithEquippedCopy

/**
 * Mirrormind Crown
 * {4}
 * Artifact — Equipment
 * As long as this Equipment is attached to a creature, the first time you would
 * create one or more tokens each turn, you may instead create that many tokens
 * that are copies of equipped creature.
 * Equip {2}
 */
val MirrormindCrown = card("Mirrormind Crown") {
    manaCost = "{4}"
    typeLine = "Artifact — Equipment"
    oracleText = "As long as this Equipment is attached to a creature, the first time you would create one or more tokens each turn, you may instead create that many tokens that are copies of equipped creature.\nEquip {2}"

    replacementEffect(
        ReplaceTokenCreationWithEquippedCopy(
            optional = true,
            oncePerTurn = true
        )
    )

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "258"
        artist = "Dan Frazier"
        flavorText = "Forged in Velis Vel, swiped by a clever merrow, and sold to a curious kithkin."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/061d765e-df27-406a-9ba0-b51b0cbb65da.jpg?1767658634"
        ruling("2025-11-17", "The tokens created by Mirrormind Crown's ability copy exactly what was printed on the equipped creature and nothing else (unless that creature is copying something else or is a token).")
        ruling("2025-11-17", "Any enters abilities of the copied creature will trigger when the tokens enter.")
    }
}
