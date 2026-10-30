package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Master Transmuter — Conflux #31
 * {3}{U} · Artifact Creature — Human Artificer · 1/2
 *
 * {U}, {T}, Return an artifact you control to its owner's hand: You may put an artifact card from
 * your hand onto the battlefield.
 *
 * The bounce is `Costs.ReturnToHand(Artifact)`, whose activated-ability candidate pool includes the
 * source itself — Master Transmuter may return itself to pay its own cost (ruling). The composite
 * pays in printed order, so the {T} lands before the bounce. `putFromHand`'s `ChooseUpTo(1)` is the
 * "you may", and since the hand is read at resolution the card just returned is a legal choice
 * (ruling: it comes back as a new object).
 */
val MasterTransmuter = card("Master Transmuter") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Human Artificer"
    power = 1
    toughness = 2
    oracleText = "{U}, {T}, Return an artifact you control to its owner's hand: You may put an " +
        "artifact card from your hand onto the battlefield."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{U}"),
            Costs.Tap,
            Costs.ReturnToHand(GameObjectFilter.Artifact)
        )
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Artifact,
            count = 1,
            prompt = "You may put an artifact card from your hand onto the battlefield",
        )
        description = "{U}, {T}, Return an artifact you control to its owner's hand: You may put an " +
            "artifact card from your hand onto the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Chippy"
        flavorText = "\"Wasted potential surrounds us. Lend me that bauble, and let me see what it " +
            "can be made to be.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/5/252482b2-aaa7-49f3-af8c-30923ca98994.jpg?1783942487"
        ruling(
            "2020-08-07",
            "Master Transmuter can be returned to its owner's hand to pay the cost of its activated ability."
        )
        ruling(
            "2020-08-07",
            "The artifact card you put onto the battlefield when the ability resolves may be the same " +
                "card that you returned to your hand when you paid the cost. If so, it returns to the " +
                "battlefield as a new object with no relation to its previous existence."
        )
    }
}
