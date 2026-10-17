package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Majestic Metamorphosis
 * {2}{U}
 * Instant
 * Until end of turn, target artifact or creature becomes a 4/4 Angel artifact creature and gains
 * flying.
 * Draw a card.
 *
 * "Becomes a 4/4 Angel artifact creature" sets the card types (CR 205.1a): the target is an
 * artifact creature and nothing else — so every other permanent card type is removed — and its
 * creature types are replaced by Angel. Supertypes, abilities and colors are untouched (the
 * card's rulings: it "retains its original abilities and colors").
 */
val MajesticMetamorphosis = card("Majestic Metamorphosis") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, target artifact or creature becomes a 4/4 Angel artifact " +
        "creature and gains flying.\nDraw a card."

    spell {
        val permanent = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature))
        effect = Effects.BecomeCreature(
            target = permanent,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.FLYING),
            creatureTypes = setOf("Angel"),
            addTypes = setOf(CardType.ARTIFACT.name),
            removeTypes = setOf(
                CardType.ENCHANTMENT.name,
                CardType.LAND.name,
                CardType.PLANESWALKER.name,
                CardType.BATTLE.name,
                CardType.KINDRED.name,
            ),
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Ben Hill"
        flavorText = "New Capenna's statues hold Old Capenna's secrets."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcaebea6-9e3f-4ac7-abe1-5445912f5b44.jpg?1783923143"
    }
}
