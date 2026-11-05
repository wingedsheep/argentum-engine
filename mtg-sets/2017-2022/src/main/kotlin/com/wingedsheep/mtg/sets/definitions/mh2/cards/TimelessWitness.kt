package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.eternalize
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Timeless Witness — Modern Horizons 2 #179 (canonical printing)
 * {2}{G}{G} · Creature — Human Shaman · 2/1
 *
 * When this creature enters, return target card from your graveyard to your hand.
 * Eternalize {5}{G}{G}
 *
 * Eternalize (CR 702.129) is the shared `eternalize(cost)` helper — `eternalizeAbility(cost)` builds the
 * sorcery-speed graveyard ability that exiles the card as a cost and makes a 4/4 black Zombie token copy
 * with no mana cost, marked `Keyword.ETERNALIZE` so "whenever you activate an eternalize ability" sees it.
 */
val TimelessWitness = card("Timeless Witness") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, return target card from your graveyard to your hand.\n" +
        "Eternalize {5}{G}{G} ({5}{G}{G}, Exile this card from your graveyard: Create a token that's a copy " +
        "of it, except it's a 4/4 black Zombie Human Shaman with no mana cost. Eternalize only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val card = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.ReturnToHand(card)
    }

    eternalize("{5}{G}{G}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f22c07e2-91d2-4edb-bd2b-cca6d4cefcc9.jpg?1783926823"
        ruling("2021-06-18", "Once you've activated an eternalize ability, the card is immediately exiled. Opponents can't try to stop the ability by exiling the card.")
        ruling("2021-06-18", "The token copies exactly what was printed on the original card and nothing else, except the characteristics specifically modified by eternalize. It doesn't copy any information about the object the card was before it was put into your graveyard.")
        ruling("2021-06-18", "The token is a Zombie in addition to its other types and is black instead of its other colors. Its base power and toughness are 4/4. It has no mana cost, and thus its mana value is 0. These are copiable values of the token that other effects may copy.")
    }
}
