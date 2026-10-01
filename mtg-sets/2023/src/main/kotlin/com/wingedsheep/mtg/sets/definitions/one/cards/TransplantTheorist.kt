package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Transplant Theorist
 * {3}{U}
 * Artifact Creature — Phyrexian Artificer
 * 2/4
 *
 * Whenever this creature or another artifact you control enters, you may draw a card. If you do,
 * discard a card.
 * {2}: Put target card from your graveyard on the bottom of your library.
 *
 * The Theorist is itself an artifact, so "this creature or another artifact you control" is
 * exactly "an artifact you control" — one enters trigger over your artifacts covers both.
 */
val TransplantTheorist = card("Transplant Theorist") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Phyrexian Artificer"
    power = 2
    toughness = 4
    oracleText = "Whenever this creature or another artifact you control enters, you may draw a card. If you do, discard a card.\n" +
        "{2}: Put target card from your graveyard on the bottom of your library."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).enters()
        effect = Effects.May(Patterns.Hand.loot())
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        val t = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Move(t, Zone.LIBRARY, ZonePlacement.Bottom)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Xavier Ribeiro"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f912c4ec-08e9-4524-a2ba-98f6dfabdc18.jpg?1783918055"
    }
}
