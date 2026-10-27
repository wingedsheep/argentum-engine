package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Renegade Reaper — Kaldheim #386
 * {2}{B} · Creature — Angel Berserker · Uncommon · 2/3
 *
 * Flying
 * When this creature enters, mill four cards. If at least one Angel card is milled this way, you
 * gain 4 life.
 *
 * Mills four into the "milled" collection ([Patterns.Library.mill]) and gates the life gain on that
 * collection containing any card with the Angel subtype (Old Rutstein shape).
 */
val RenegadeReaper = card("Renegade Reaper") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Angel Berserker"
    power = 2
    toughness = 3
    oracleText = "Flying\nWhen this creature enters, mill four cards. If at least one Angel card is " +
        "milled this way, you gain 4 life. (To mill a card, put the top card of your library into " +
        "your graveyard.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(4) then
            Effects.If(
                condition = Conditions.CollectionContainsMatch(
                    Patterns.Library.milled,
                    GameObjectFilter.Any.withSubtype(Subtype.ANGEL)
                ),
                then = Effects.GainLife(4)
            )
        description = "When this creature enters, mill four cards. If at least one Angel card is " +
            "milled this way, you gain 4 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "386"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5675655-dc9f-45ad-9cd0-ce28fba1f972.jpg?1783928118"
    }
}
