package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Seasons Past
 * {4}{G}{G}
 * Sorcery
 * Return any number of cards with different mana values from your graveyard to your hand. Put
 * Seasons Past on the bottom of its owner's library.
 *
 * The cards aren't targeted, so they're chosen as the spell resolves (2016-04-08 ruling): a
 * `chooseAnyNumber` over the graveyard under `SelectionRestriction.OnePerManaValue`, which keeps
 * at most one card of each mana value ({X} counts 0, a land is 0). `selfPutOnBottomOfLibrary()`
 * replaces the CR 608.2n graveyard destination; a countered Seasons Past still goes to the graveyard.
 */
val SeasonsPast = card("Seasons Past") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Return any number of cards with different mana values from your graveyard to your hand. " +
        "Put Seasons Past on the bottom of its owner's library."

    spell {
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
            val chosen = chooseAnyNumber(
                from = graveyard,
                restrictions = listOf(SelectionRestriction.OnePerManaValue),
                prompt = "Return any number of cards with different mana values to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        }
        selfPutOnBottomOfLibrary()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "226"
        artist = "Christine Choi"
        flavorText = "The seeds of new life take root in the soil of old."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/668afd78-3cf5-4daf-8dfb-fca90de0ae5a.jpg?1783937721"
        ruling("2016-04-08", "The phrase \"different mana values\" compares the mana costs of cards in your graveyard to one another, not to Seasons Past. You may return a card with a mana value of 6.")
        ruling("2016-04-08", "If a card in your graveyard has {X} in its mana cost, X is considered to be 0.")
        ruling("2016-04-08", "A card with no mana cost, such as a land, has a mana value of 0.")
        ruling("2016-04-08", "You choose which cards to return as Seasons Past resolves, not as you cast it.")
    }
}
