package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hoarding Broodlord
 * {5}{B}{B}{B}
 * Creature — Dragon
 * 7/6
 * Convoke
 * Flying
 * When this creature enters, search your library for a card, exile it face down, then shuffle.
 * For as long as that card remains exiled, you may play it.
 * Spells you cast from exile have convoke.
 *
 * The last line is [GrantKeywordToOwnSpells] scoped by `fromZone = EXILE` over every spell — it
 * covers the tutored card and any other exile cast (an Adventure's creature half, an impulse draw).
 */
val HoardingBroodlord = card("Hoarding Broodlord") {
    manaCost = "{5}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Dragon"
    power = 7
    toughness = 6
    oracleText = "Convoke\nFlying\n" +
        "When this creature enters, search your library for a card, exile it face down, then shuffle. " +
        "For as long as that card remains exiled, you may play it.\n" +
        "Spells you cast from exile have convoke."

    keywords(Keyword.CONVOKE, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val library = gather(CardSource.FromZone(Zone.LIBRARY, Player.You), search = true)
            val found = chooseExactly(1, from = library, prompt = "Search your library for a card")
            val exiled = moveTracked(found, CardDestination.ToZone(Zone.EXILE), faceDown = FaceDownMode.HIDDEN)
            run(Effects.ShuffleLibrary())
            run(Effects.GrantMayPlayFromExile(exiled, expiry = MayPlayExpiry.Permanent))
        }
    }

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.CONVOKE,
            spellFilter = GameObjectFilter.Any,
            fromZone = Zone.EXILE
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "110"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/386ce3c9-869d-461c-a3de-c8add3786f73.jpg?1783917006"
    }
}
