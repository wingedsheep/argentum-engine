package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Experimental Overload
 * {2}{U}{R}
 * Sorcery
 *
 * Create an X/X blue and red Weird creature token, where X is the number of instant and sorcery
 * cards in your graveyard. Then you may return an instant or sorcery card from your graveyard to
 * your hand. Exile Experimental Overload.
 *
 * X is read once, as the spell resolves, and baked into the token's base P/T (the Tumbleweed Rising
 * [Effects.CreateDynamicToken] shape); the spell is still on the stack then, so it never counts
 * itself. The return is untargeted, so it is a Corpse Churn up-to-one pipeline choice at
 * resolution. `selfExile()` exiles the card whether or not a card was returned.
 */
val ExperimentalOverload = card("Experimental Overload") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Create an X/X blue and red Weird creature token, where X is the number of instant and " +
        "sorcery cards in your graveyard. Then you may return an instant or sorcery card from your " +
        "graveyard to your hand. Exile Experimental Overload."

    spell {
        val x = DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery)
        effect = Effects.Pipeline {
            run(
                Effects.CreateDynamicToken(
                    dynamicPower = x,
                    dynamicToughness = x,
                    colors = setOf(Color.BLUE, Color.RED),
                    creatureTypes = setOf("Weird"),
                    imageUri = "https://cards.scryfall.io/normal/front/2/a/2a0d9c67-69ee-48c4-af4c-18cc3c2ef3cd.jpg?1783930589"
                )
            )
            val spells = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.InstantOrSorcery))
            val chosen = chooseUpTo(
                1,
                from = spells,
                showAllCards = true,
                prompt = "You may return an instant or sorcery card from your graveyard to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        }
        selfExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Lie Setiawan"
        flavorText = "Explosions aren't necessarily failures, just a different kind of success."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f1bace4-a327-4eb6-a6ef-8394e76c06b7.jpg?1783930663"
        ruling("2020-06-23", "The value of X is determined only as Experimental Overload resolves. The Weird's power and toughness don't change if the number of instant and sorcery cards in your graveyard changes.")
        ruling("2020-06-23", "Because Experimental Overload is still on the stack while it's resolving, it doesn't count towards the value of X.")
        ruling("2020-06-23", "You exile Experimental Overload even if you don't return a card to your hand.")
    }
}
