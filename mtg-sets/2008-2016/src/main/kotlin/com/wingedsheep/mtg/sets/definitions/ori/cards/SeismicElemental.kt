package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Seismic Elemental (Magic Origins #161)
 * {3}{R}{R}
 * Creature — Elemental
 * 4/4
 *
 * When this creature enters, creatures without flying can't block this turn.
 *
 * The can't-block is a floating group restriction (`CantBlockGroup`), not a snapshot: it changes no
 * characteristics, so the affected set is re-evaluated when blockers are declared — creatures that
 * lose flying or enter later this turn can't block, and one that gains flying can. It affects every
 * creature without flying, including the controller's own.
 */
val SeismicElemental = card("Seismic Elemental") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, creatures without flying can't block this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CantBlockGroup(
            GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING))
        )
        description = "When this creature enters, creatures without flying can't block this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Jasper Sandner"
        flavorText = "\"Trying to stop it would be foolish. If you feel it coming—run!\"\n—Sergeant Ryda"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a60d957b-f71a-403e-82fe-30ea87d1e142.jpg?1783938326"
        ruling(
            "2015-06-22",
            "No creature without flying will be able to block that turn, including creatures that lose " +
                "flying after Seismic Elemental's ability resolves and creatures without flying that enter " +
                "the battlefield after that ability resolves."
        )
    }
}
