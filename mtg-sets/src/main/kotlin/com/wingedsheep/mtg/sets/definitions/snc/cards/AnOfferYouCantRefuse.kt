package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * An Offer You Can't Refuse
 * {U}
 * Instant
 * Counter target noncreature spell. Its controller creates two Treasure tokens.
 * (They're artifacts with "{T}, Sacrifice this token: Add one mana of any color.")
 */
val AnOfferYouCantRefuse = card("An Offer You Can't Refuse") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target noncreature spell. Its controller creates two Treasure tokens. " +
        "(They're artifacts with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        target("target noncreature spell", Targets.NoncreatureSpell)
        // Resolve the Treasures while the spell is still on the stack so `TargetController`
        // can read its controller (same pattern as Undermine). The tokens are created even
        // if the spell itself can't be countered.
        effect = Effects.CreateTreasure(2, controller = EffectTarget.TargetController)
            .then(Effects.CounterSpell())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Dallas Williams"
        flavorText = "Elspeth wanted answers. Xander needed a spy. Their paths were destined to cross."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9d349f3-5be2-4b1f-a4c3-ba94822cf0cf.jpg?1782701632"
    }
}
