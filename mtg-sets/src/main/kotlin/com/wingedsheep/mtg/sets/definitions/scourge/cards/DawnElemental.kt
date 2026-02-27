package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Dawn Elemental
 * {W}{W}{W}{W}
 * Creature — Elemental
 * 3/3
 * Flying
 * Prevent all damage that would be dealt to Dawn Elemental.
 */
val DawnElemental = card("Dawn Elemental") {
    manaCost = "{W}{W}{W}{W}"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 3
    oracleText = "Flying\nPrevent all damage that would be dealt to Dawn Elemental."

    keywords(Keyword.FLYING)

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = GameEvent.DamageEvent(
                recipient = RecipientFilter.Self,
                damageType = DamageType.Any
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Anthony S. Waters"
        flavorText = "\"It was midnight on the Daru Plains, yet it seemed the sun was rising.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd90a303-25fb-460b-bd55-6249f61c361c.jpg?1562537377"
    }
}
