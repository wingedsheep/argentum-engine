package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Murasa Sproutling — Zendikar Rising #196
 * {2}{G} · Creature — Plant Elemental · 3/3
 *
 * Kicker {1}{G}
 * When this creature enters, if it was kicked, return target card with a kicker ability from your
 * graveyard to your hand.
 */
val MurasaSproutling = card("Murasa Sproutling") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Elemental"
    oracleText = "Kicker {1}{G} (You may pay an additional {1}{G} as you cast this spell.)\n" +
        "When this creature enters, if it was kicked, return target card with a kicker ability from your " +
        "graveyard to your hand."
    power = 3
    toughness = 3

    keywordAbility(KeywordAbility.kicker("{1}{G}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        val card = target(TargetFilter(GameObjectFilter.Any.withKicker().ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Move(target = card, destination = Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Svetlin Velinov"
        flavorText = "The seeds of memory grow eternal."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f53472b7-53da-4833-b7f9-204706dfb721.jpg?1783929336"
    }
}
