package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateAdditionalToken
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Queen Allenal of Ruadach — Dominaria United #210
 * {G}{W}{W} · Legendary Creature — Elf Noble · * / *
 *
 * Queen Allenal of Ruadach's power and toughness are each equal to the number of creatures you control.
 * If one or more creature tokens would be created under your control, those tokens plus a 1/1 white
 * Soldier creature token are created instead.
 *
 * The P/T is a characteristic-defining `dynamicStats` over creatures you control (it counts itself).
 * The replacement is [CreateAdditionalToken] at its default per-event scale — one Soldier per creation
 * event however many creature tokens it makes — gated to creature tokens by the event's token filter,
 * so a Treasure or Clue alone adds nothing. The Soldier shares the original creation's "tapped" rider
 * (ruling) but none of the original tokens' abilities.
 */
val QueenAllenalOfRuadach = card("Queen Allenal of Ruadach") {
    manaCost = "{G}{W}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Elf Noble"
    oracleText = "Queen Allenal of Ruadach's power and toughness are each equal to the number of creatures you control.\n" +
        "If one or more creature tokens would be created under your control, those tokens plus a 1/1 white Soldier creature token are created instead."
    power = 0
    toughness = 0
    dynamicStats(DynamicAmounts.creaturesYouControl())

    replacementEffect(
        CreateAdditionalToken(
            additionalTokenType = "Soldier",
            inheritTapped = true,
            appliesTo = EventPattern.TokenCreationEvent(tokenFilter = GameObjectFilter.Creature)
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b44b4e8b-7675-4c6a-a16a-92f8b6a0259f.jpg?1783921281"
        ruling(
            "2022-09-09",
            "The additional Soldier token won't have any abilities the other tokens were created with. Anything else specified in the effect creating the tokens (such as tapped, attacking, \"That token gains haste,\" or \"Exile that token at end of combat\") applies to both the original tokens and the Soldier."
        )
    }
}
