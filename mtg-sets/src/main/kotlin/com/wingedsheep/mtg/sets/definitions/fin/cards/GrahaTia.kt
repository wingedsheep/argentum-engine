package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * G'raha Tia
 * {4}{W}
 * Legendary Creature — Cat Archer
 * 3/5
 *
 * Reach
 * The Allagan Eye — Whenever one or more other creatures and/or artifacts you control die,
 * draw a card. This ability triggers only once each turn.
 *
 * Implementation notes:
 *  - "The Allagan Eye" is an ability word (flavor only, no rules meaning); kept in the oracle
 *    text / description for display but carries no mechanics.
 *  - Modeled as the batched death trigger [Triggers.OneOrMoreCreaturesYouControlDie] over
 *    [GameObjectFilter.CreatureOrArtifact] with `excludeSelf = true` (the "other" wording).
 *    The batch fires at most once per simultaneous death event (CR 603.3b), so a board wipe
 *    that kills several of your creatures/artifacts draws one card, not one per permanent.
 *  - `oncePerTurn = true` enforces "This ability triggers only once each turn".
 */
val GrahaTia = card("G'raha Tia") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Cat Archer"
    oracleText = "Reach\n" +
        "The Allagan Eye — Whenever one or more other creatures and/or artifacts you control " +
        "die, draw a card. This ability triggers only once each turn."
    power = 3
    toughness = 5

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.OneOrMoreCreaturesYouControlDie(
            filter = GameObjectFilter.CreatureOrArtifact,
            excludeSelf = true,
        )
        oncePerTurn = true
        effect = Effects.DrawCards(1)
        description = "The Allagan Eye — Whenever one or more other creatures and/or artifacts " +
            "you control die, draw a card. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Narendra Bintara Adi"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/076a8eca-ed73-4ee9-aab4-d9d43d394ee6.jpg?1748705832"
    }
}
