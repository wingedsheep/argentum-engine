package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gods Willing — Theros #16 (canonical printing)
 * {W} · Instant
 *
 * Target creature you control gains protection from the color of your choice until end of turn.
 * Scry 1.
 *
 * The color is chosen on resolution (Mother of Runes' shape); the scry follows regardless.
 */
val GodsWilling = card("Gods Willing") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature you control gains protection from the color of your choice until end of turn. " +
        "(It can't be blocked, targeted, dealt damage, enchanted, or equipped by anything of that color.)\nScry 1."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(creature)) then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Mark Winters"
        flavorText = "Honor the gods of Theros, and they will return the favor."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abafabb3-b2e7-4d78-b4b7-d8f701d3ee8b.jpg?1783939814"
    }
}
