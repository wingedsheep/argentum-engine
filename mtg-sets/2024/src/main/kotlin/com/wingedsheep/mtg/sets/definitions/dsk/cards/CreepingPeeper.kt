package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Creeping Peeper
 * {1}{U}
 * Creature — Eye
 * {T}: Add {U}. Spend this mana only to cast an enchantment spell, unlock a door, or turn a
 * permanent face up.
 * 2/1
 *
 * The triple spend restriction is a disjunction of three atomic restrictions via
 * [ManaRestriction.AnyOf]: cast an enchantment spell ([ManaRestriction.CardTypeSpellsOrAbilitiesOnly]
 * for ENCHANTMENT), unlock a door ([ManaRestriction.UnlockDoorOnly]), or turn a permanent face up
 * ([ManaRestriction.TurnPermanentsFaceUpOnly]).
 */
val CreepingPeeper = card("Creeping Peeper") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eye"
    power = 2
    toughness = 1
    oracleText = "{T}: Add {U}. Spend this mana only to cast an enchantment spell, unlock a door, " +
        "or turn a permanent face up."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.BLUE, 1,
            restriction = ManaRestriction.AnyOf(
                listOf(
                    ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                        cardType = CardType.ENCHANTMENT,
                        allowSpells = true,
                        allowAbilities = false,
                    ),
                    ManaRestriction.UnlockDoorOnly,
                    ManaRestriction.TurnPermanentsFaceUpOnly(),
                ),
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {U}. Spend this mana only to cast an enchantment spell, unlock a " +
            "door, or turn a permanent face up."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Maxime Minard"
        flavorText = "\"Be on your guard. Valgavoth has eyes everywhere. And I don't mean that " +
            "figuratively.\"\n—Winter"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7ad59368-1335-4d8e-a254-ccd889933e57.jpg?1726286029"
    }
}
