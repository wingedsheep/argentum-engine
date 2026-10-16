package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Evelyn, the Covetous
 * {2}{U/B}{B}{B/R}
 * Legendary Creature — Vampire Rogue
 * 2/5
 *
 * Flash
 * Whenever Evelyn or another Vampire you control enters, exile the top card of each player's
 * library with a collection counter on it.
 * Once each turn, you may play a card from exile with a collection counter on it if it was exiled
 * by an ability you controlled, and you may spend mana as though it were mana of any color to
 * cast it.
 *
 * The permission is filter-defined, not linked to this Evelyn: any collection-countered card an
 * ability you controlled exiled qualifies, including one exiled by an earlier Evelyn. Evelyn is
 * itself a Vampire, so "Evelyn or another Vampire you control" is one Vampire-you-control trigger.
 */
val EvelynTheCovetous = card("Evelyn, the Covetous") {
    manaCost = "{2}{U/B}{B}{B/R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Vampire Rogue"
    power = 2
    toughness = 5
    oracleText = "Flash\n" +
        "Whenever Evelyn or another Vampire you control enters, exile the top card of each " +
        "player's library with a collection counter on it.\n" +
        "Once each turn, you may play a card from exile with a collection counter on it if it was " +
        "exiled by an ability you controlled, and you may spend mana as though it were mana of " +
        "any color to cast it."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype("Vampire")).youControl()).enters()
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(count = 1, player = Player.Each))
            move(
                exiled,
                CardDestination.ToZone(Zone.EXILE),
                addCounterType = CounterType.COLLECTION
            )
        }
    }

    staticAbility {
        ability = MayPlayCardsFromExile(
            filter = GameObjectFilter.Any.withCounter(CounterType.COLLECTION).exiledByYou(),
            withAnyManaColor = true,
            oncePerTurn = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0dad61f-36cd-46af-82b7-a02e04efd676.jpg?1783923087"
    }
}
