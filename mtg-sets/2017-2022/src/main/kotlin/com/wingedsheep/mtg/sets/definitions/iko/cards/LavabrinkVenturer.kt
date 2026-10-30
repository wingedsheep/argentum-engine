package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.Parity
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Lavabrink Venturer
 * {2}{W}
 * Creature — Human Soldier
 * 3/3
 * As this creature enters, choose odd or even. (Zero is even.)
 * This creature has protection from each mana value of the chosen quality.
 *
 * Ashling's Prerogative's shape: an as-enters `ChoiceType.MODE` replacement writes "odd" or
 * "even", and one conditional self-grant per mode turns on `ProtectionScope.ManaValueParity`.
 * With no choice made neither grant is live — no protection, as the ruling says.
 */
val LavabrinkVenturer = card("Lavabrink Venturer") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "As this creature enters, choose odd or even. (Zero is even.)\n" +
        "This creature has protection from each mana value of the chosen quality."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = Parity.entries.map { parity ->
                ModeOption(
                    id = parity.displayName,
                    label = parity.displayName.replaceFirstChar { it.uppercase() },
                    description = "Protection from each ${parity.displayName} mana value."
                )
            }
        )
    )

    for (parity in Parity.entries) {
        staticAbility {
            condition = SourceChosenModeIs(parity.displayName)
            ability = GrantProtectionFromToGroup(ProtectionScope.ManaValueParity(parity), GroupFilter.source())
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Zoltan Boros"
        flavorText = "\"Take one wrong step out here and you're dead. I take only right steps.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38c0d9d6-c9c7-4169-8106-400445643a1a.jpg?1783931089"
        ruling("2020-04-17", "You choose odd or even for Lavabrink Venturer before it enters the battlefield. There's no point at which players can target it before it has gained the appropriate protection abilities.")
        ruling("2020-04-17", "If Lavabrink Venturer somehow has no choice made for its ability, its last ability grants it no protection abilities.")
        ruling("2020-04-17", "For spells with {X} in their mana costs, use the value chosen for X to determine the spell's mana value. If a permanent or card in any other zone has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-04-17", "The mana value of a token that isn't a copy of another object is 0. A token that is a copy of another object has the same mana value as that object.")
    }
}
