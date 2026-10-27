package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mother of Runes — Urza's Legacy #14 (canonical printing)
 * {W} · Creature — Human Cleric 1/1
 *
 * {T}: Target creature you control gains protection from the color of your choice until end of turn.
 *
 * The color is chosen on resolution (Armored Guardian's shape).
 */
val MotherOfRunes = card("Mother of Runes") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "{T}: Target creature you control gains protection from the color of your choice until end of turn."

    activatedAbility {
        cost = Costs.Tap
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(t))
        description = "{T}: Target creature you control gains protection from the color of your choice until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Scott M. Fischer"
        flavorText = "\"My family protects all families.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b1a46ab-95cb-4c24-924f-fc2afd4fcac7.jpg?1783946251"
    }
}
