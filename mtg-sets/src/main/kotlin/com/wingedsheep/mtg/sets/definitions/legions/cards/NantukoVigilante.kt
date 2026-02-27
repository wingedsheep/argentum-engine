package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Nantuko Vigilante
 * {3}{G}
 * Creature — Insect Druid Mutant
 * 3/2
 * Morph {1}{G} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When Nantuko Vigilante is turned face up, destroy target artifact or enchantment.
 */
val NantukoVigilante = card("Nantuko Vigilante") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Insect Druid Mutant"
    power = 3
    toughness = 2
    oracleText = "Morph {1}{G} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Nantuko Vigilante is turned face up, destroy target artifact or enchantment."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val t = target("artifact or enchantment", TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment)))
        effect = Effects.Destroy(t)
    }

    morph = "{1}{G}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Alex Horley-Orlandelli"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7474849-a6b4-4f3b-a836-37b88c26047b.jpg?1562941424"
        ruling("2004-10-04", "The trigger occurs when you use the Morph ability to turn the card face up, or when an effect turns it face up. It will not trigger on being revealed or on leaving the battlefield.")
    }
}
