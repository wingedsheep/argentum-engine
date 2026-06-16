package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wylie Duke, Atiin Hero {1}{G}{W}
 * Legendary Creature — Human Ranger
 * 4/2
 *
 * Vigilance
 * Whenever Wylie Duke becomes tapped, you gain 1 life and draw a card.
 *
 * The "becomes tapped" trigger ([Triggers.BecomesTapped]) fires on any way Wylie Duke becomes
 * tapped — tapped for mana/ability cost, tapped by an opponent's effect, etc. Because Wylie Duke
 * has vigilance, attacking does NOT tap it, so attacking alone won't trigger this.
 */
val WylieDukeAtiinHero = card("Wylie Duke, Atiin Hero") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Ranger"
    oracleText = "Vigilance\nWhenever Wylie Duke becomes tapped, you gain 1 life and draw a card."
    power = 4
    toughness = 2
    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.BecomesTapped
        effect = Effects.Composite(
            Effects.GainLife(1),
            Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "239"
        artist = "Ekaterina Burmak"
        flavorText = "\"I've seen a fair few worlds in my travels, and there's always some lowlife " +
            "looking to prey on the innocent. That's where I come in.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc97ffcf-4f51-44cd-8daa-a7dae4592ee5.jpg?1712356248"
    }
}
