package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Boneyard Lurker
 * {2}{B}{G}
 * Creature — Nightmare Beast
 * 4/4
 * Mutate {2}{B/G}{B/G}
 * Whenever this creature mutates, return target permanent card from your graveyard to your hand.
 */
val BoneyardLurker = card("Boneyard Lurker") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Nightmare Beast"
    power = 4
    toughness = 4
    oracleText = "Mutate {2}{B/G}{B/G} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Whenever this creature mutates, return target permanent card from your graveyard to your hand."

    keywordAbility(KeywordAbility.mutate("{2}{B/G}{B/G}"))

    triggeredAbility {
        trigger = Triggers.self.mutates()
        val card = target(TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Move(card, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37e4df5b-ec53-4f8a-8c26-272b3177c0a6.jpg?1783931028"
    }
}
