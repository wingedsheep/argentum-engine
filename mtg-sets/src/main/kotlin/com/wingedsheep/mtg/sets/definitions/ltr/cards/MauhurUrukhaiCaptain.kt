package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Mauhúr, Uruk-hai Captain
 * {B}{R}
 * Legendary Creature — Orc Soldier
 * 2/2
 *
 * Menace
 * If one or more +1/+1 counters would be put on an Army, Goblin, or Orc you control,
 * that many plus one +1/+1 counters are put on it instead.
 */
val MauhurUrukhaiCaptain = card("Mauhúr, Uruk-hai Captain") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Orc Soldier"
    power = 2
    toughness = 2
    oracleText = "Menace\n" +
        "If one or more +1/+1 counters would be put on an Army, Goblin, or Orc you control, " +
        "that many plus one +1/+1 counters are put on it instead."

    keywords(Keyword.MENACE)

    replacementEffect(
        ModifyCounterPlacement(
            modifier = 1,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterTypeFilter.PlusOnePlusOne,
                recipient = RecipientFilter.Matching(
                    GameObjectFilter.Permanent.withAnySubtype("Army", "Goblin", "Orc").youControl()
                )
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Javier Charro"
        flavorText = "\"We are the servants of Saruman the Wise, the White Hand: the Hand that gives us Man's-flesh to eat.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65e9a757-7ed4-4cc0-bb6f-a59fa69b32a5.jpg?1686969884"
    }
}
