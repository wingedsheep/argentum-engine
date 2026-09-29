package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient

val ChampionLancer = card("Champion Lancer") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 3
    oracleText = "Prevent all damage that would be dealt to this creature by creatures."

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Self,
                source = GameObjectFilter.Creature
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Chippy"
        flavorText = "The flash of his lance projects the pure radiance of his honor."
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26b4171b-2d49-4e06-a2fd-9fe3cfd6ce95.jpg?1783946053"
    }
}
