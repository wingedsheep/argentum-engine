package com.wingedsheep.mtg.sets.definitions.sok.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Reki, the History of Kamigawa — Saviors of Kamigawa #142
 * {2}{G} · Legendary Creature — Human Shaman 1/2
 *
 * Whenever you cast a legendary spell, draw a card.
 *
 * `Triggers.you.casts` with a legendary spell filter — the same trigger as The Circle of Loyalty.
 */
val RekiTheHistoryOfKamigawa = card("Reki, the History of Kamigawa") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Shaman"
    power = 1
    toughness = 2
    oracleText = "Whenever you cast a legendary spell, draw a card."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.legendary())
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"After his death, monks spent ten years transcribing the tattoos from Reki's body and " +
            "gathering stories from those who spoke with him. Thus the volume you hold was written.\"\n" +
            "—The History of Kamigawa"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86317339-5a39-41eb-8aee-5ea926da8cd4.jpg?1783944138"
        ruling("2005-06-01", "Legendary spells are spells of any type that have the supertype legendary.")
        ruling("2005-06-01", "Lands aren't spells.")
    }
}
