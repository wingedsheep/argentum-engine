package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Dal R'El, Augment Orphan
 * {1}{R}{G}
 * Legendary Creature — Human Mutant Rogue
 * 3/3
 * Landfall — Whenever a land you control enters, add {R} or {G}.
 * {3}, Sacrifice a land: Draw a card.
 *
 * Landfall is Lotus Cobra's shape; the colour is chosen as the trigger resolves, via
 * `AddManaOfChoice(Specific(R, G))` as on Klothys. A land entering isn't a mana ability, so this
 * trigger uses the stack (not a triggered mana ability).
 */
val DalRElAugmentOrphan = card("Dal R'El, Augment Orphan") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Mutant Rogue"
    power = 3
    toughness = 3
    oracleText = "Landfall — Whenever a land you control enters, add {R} or {G}.\n" +
        "{3}, Sacrifice a land: Draw a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddManaOfChoice(ManaColorSet.Specific(setOf(Color.RED, Color.GREEN)))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Sacrifice(GameObjectFilter.Land))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Stella Spente"
        flavorText = "\"Maybe home isn't always a place. It's the people you trust the most.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0da52d3a-dd83-4662-8e6d-3f4d4d389c3d.jpg?1792508298"
    }
}
