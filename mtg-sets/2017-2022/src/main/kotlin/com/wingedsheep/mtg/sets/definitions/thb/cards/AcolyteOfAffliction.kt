package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Acolyte of Affliction
 * {2}{B}{G}
 * Creature — Human Cleric
 * 2/3
 * When this creature enters, mill two cards, then you may return a permanent card from your
 * graveyard to your hand.
 *
 * Eccentric Farmer's shape: mill first so the milled cards are legal picks (per the ruling, the
 * returned card isn't targeted), then a `chooseUpTo(1)` over the graveyard's permanent cards.
 */
val AcolyteOfAffliction = card("Acolyte of Affliction") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, mill two cards, then you may return a permanent card " +
        "from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            run(Patterns.Library.mill(2))
            val permanents = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, Filters.Permanent))
            val chosen = chooseUpTo(
                1,
                from = permanents,
                showAllCards = true,
                prompt = "You may return a permanent card from your graveyard to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Chase Stone"
        flavorText = "The best healers are intimately familiar with death."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a14afed6-ca42-442d-ba86-621179e6957c.jpg?1783931526"
        ruling(
            "2020-01-24",
            "The card you return with Acolyte of Affliction's ability may be one that you just put " +
                "into your graveyard with its ability. This is because the ability doesn't target the " +
                "card that you'll return."
        )
    }
}
