package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * The Duras Sisters
 * {B/R}{B/R}
 * Legendary Creature — Klingon Warrior
 * 3/2
 *
 * When The Duras Sisters die, create a 2/1 red Klingon Warrior creature token with haste.
 */
val TheDurasSisters = card("The Duras Sisters") {
    manaCost = "{B/R}{B/R}"
    typeLine = "Legendary Creature — Klingon Warrior"
    colorIdentity = "BR"
    oracleText = "When The Duras Sisters die, create a 2/1 red Klingon Warrior creature token with haste."
    power = 3
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 2, toughness = 1, colors = setOf(Color.RED),
            creatureTypes = setOf("Klingon", "Warrior"), keywords = setOf(Keyword.HASTE)
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Harkalé Linaï"
        flavorText = "B'Etor glowered at her impatient sister. \"Lursa, you fool! You were supposed to kill him *after* we got our payment!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7eb112eb-2abf-4ba3-b9bd-b45ffa80f7eb.jpg?1792508321"
    }
}
