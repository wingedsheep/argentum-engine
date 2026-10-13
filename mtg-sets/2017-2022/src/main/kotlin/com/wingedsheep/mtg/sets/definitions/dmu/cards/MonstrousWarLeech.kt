package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Monstrous War-Leech — Dominaria United #98
 * {3}{B}
 * Creature — Leech Horror
 * * / *
 * Kicker {U}
 * As this creature enters, if it was kicked, mill four cards.
 * Monstrous War-Leech's power and toughness are each equal to the greatest mana value among cards
 * in your graveyard.
 *
 * The mill is a replacement effect (2022-09-09 ruling), not an enters trigger: it is spelled as an
 * [OnEnterRun], which runs synchronously as the permanent enters, before state-based actions are
 * next checked — so a kicked Leech entering with an empty graveyard is sized by the cards it just
 * milled rather than dying as a 0/0 first. The kicked-ness is read from the cast-choices bag the
 * permanent is stamped with on entry; a Leech put onto the battlefield without being cast reads
 * unkicked and mills nothing.
 *
 * The P/T is a characteristic-defining ability over the controller's graveyard (an {X} in a
 * graveyard card's mana cost counts as 0).
 */
val MonstrousWarLeech = card("Monstrous War-Leech") {
    manaCost = "{3}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Leech Horror"
    oracleText = "Kicker {U} (You may pay an additional {U} as you cast this spell.)\n" +
        "As this creature enters, if it was kicked, mill four cards. (To mill a card, put the top card " +
        "of your library into your graveyard.)\n" +
        "Monstrous War-Leech's power and toughness are each equal to the greatest mana value among " +
        "cards in your graveyard."

    dynamicStats(DynamicAmounts.zone(Player.You, Zone.GRAVEYARD).maxManaValue())

    keywordAbility(KeywordAbility.kicker("{U}"))

    replacementEffect(
        OnEnterRun(
            Effects.If(
                condition = Conditions.WasKicked,
                then = Patterns.Library.mill(4),
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "98"
        artist = "Christopher Burdett"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/660d7983-b335-48b4-898d-f182effcf2de.jpg?1783921331"

        ruling(
            "2022-09-09",
            "If Monstrous War-Leech was kicked, it enters the battlefield with power and toughness each " +
                "equal to the highest mana value among cards in your graveyard, including cards that were " +
                "milled due to its replacement effect. For example, if you have no cards in your graveyard " +
                "as it enters and you mill three land cards and a card with mana value 4, Monstrous " +
                "War-Leech is a 4/4 creature entering the battlefield."
        )
        ruling("2022-09-09", "If a card in a graveyard has an {X} in its mana cost, X is 0.")
        ruling(
            "2022-09-09",
            "Monstrous War-Leech's second ability is a replacement effect, not a triggered ability. It " +
                "happens at the same time that Monstrous War-Leech enters the battlefield, it doesn't use " +
                "the stack, and it can't be responded to."
        )
    }
}
