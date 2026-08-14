package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantMadnessToOwnedCards

/**
 * Falkenrath Gorger — Shadows over Innistrad #155.
 * {R}
 * Creature — Vampire Berserker
 * 2/1
 *
 * Each Vampire creature card you own that isn't on the battlefield has madness. The madness cost
 * is equal to its mana cost.
 *
 * The grant is read at the moment of the discard, and the exiled card carries its madness cost
 * from then on — so, per the Oracle rulings, the Gorger leaving the battlefield before the madness
 * trigger resolves doesn't take the cast offer away. Nor does it grant itself madness: the ability
 * only applies while it is on the battlefield, and a card being discarded is not.
 */
val FalkenrathGorger = card("Falkenrath Gorger") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Berserker"
    power = 2
    toughness = 1
    oracleText = "Each Vampire creature card you own that isn't on the battlefield has madness. " +
        "The madness cost is equal to its mana cost. (If you discard a card with madness, discard " +
        "it into exile. When you do, cast it for its madness cost or put it into your graveyard.)"

    staticAbility {
        ability = GrantMadnessToOwnedCards(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/474895d8-1f2c-45bb-b721-4cf7c290eb23.jpg?1783937755"
        ruling("2016-04-08", "If Falkenrath Gorger leaves the battlefield before the madness trigger has resolved for a Vampire card that gained madness with its ability, the madness ability will still let you cast that Vampire card for the appropriate cost even though it no longer has madness.")
        ruling("2016-04-08", "If you discard a Vampire creature card that already has a madness ability, you'll choose which madness ability exiles it. You may choose either the one it normally has or the one it gains from Falkenrath Gorger.")
        ruling("2016-04-08", "Falkenrath Gorger's ability only applies while it's on the battlefield. If you discard it, it won't give itself madness.")
    }
}
