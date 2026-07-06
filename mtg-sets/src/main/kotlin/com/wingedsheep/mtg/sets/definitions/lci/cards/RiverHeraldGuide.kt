package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Rarity

/**
 * River Herald Guide — {2}{G}
 * Creature — Merfolk Scout
 * 3/1
 * Vigilance
 * When this creature enters, it explores.
 */
val RiverHeraldGuide = card("River Herald Guide") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Merfolk Scout"
    oracleText = "Vigilance\nWhen this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"
    power = 3
    toughness = 1

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "209"
        artist = "David Astruga"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ea7e323-f329-4928-b25a-c0b44c5ac058.jpg?1782694442"
    }
}
