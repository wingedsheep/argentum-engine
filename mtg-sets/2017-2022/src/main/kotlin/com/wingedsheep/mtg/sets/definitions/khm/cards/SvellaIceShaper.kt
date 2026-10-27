package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Svella, Ice Shaper — Kaldheim #230.
 *
 *  - "{3}, {T}": creates the predefined Icy Manalith token (a colorless snow artifact with
 *    "{T}: Add one mana of any color.").
 *  - "{6}{R}{G}, {T}": looks at the top four, offers a free cast of one *spell* among them (lands
 *    aren't spells), cast during resolution so its timing is ignored, then bottoms everything still
 *    in the library — including a chosen card that couldn't be cast — in a random order.
 */
val SvellaIceShaper = card("Svella, Ice Shaper") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Snow Creature — Troll Warrior"
    power = 2
    toughness = 4
    oracleText = "{3}, {T}: Create a colorless snow artifact token named Icy Manalith with " +
        "\"{T}: Add one mana of any color.\"\n" +
        "{6}{R}{G}, {T}: Look at the top four cards of your library. You may cast a spell from among " +
        "them without paying its mana cost. Put the rest on the bottom of your library in a random order."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.CreatePredefinedToken("Icy Manalith")
        description = "{3}, {T}: Create a colorless snow artifact token named Icy Manalith with " +
            "\"{T}: Add one mana of any color.\""
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}{R}{G}"), Costs.Tap)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(4, Player.You))
            val chosen = chooseSpell(
                from = looked,
                showAllCards = true,
                prompt = "You may cast a spell from among them without paying its mana cost.",
                selectedLabel = "Cast for free",
                remainderLabel = "Put on bottom",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(from = chosen))
            val notCast = filter(looked, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY))
            toLibraryBottom(notCast, order = CardOrder.Random)
        }
        description = "{6}{R}{G}, {T}: Look at the top four cards of your library. You may cast a spell " +
            "from among them without paying its mana cost. Put the rest on the bottom of your library " +
            "in a random order."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7054d48-5ed3-43a1-85e5-9f306b081b4f.jpg?1783928190"
        ruling("2021-02-05", "The spell you cast with Svella's last ability is cast from your library.")
        ruling("2021-02-05", "If you cast a spell using Svella's ability, you do so as part of the resolution of that ability. You can't wait to cast it later in the turn. Timing permissions based on the card's type are ignored.")
        ruling("2021-02-05", "You'll put the cards you didn't cast on the bottom of your library before the spell you cast resolves.")
    }
}
