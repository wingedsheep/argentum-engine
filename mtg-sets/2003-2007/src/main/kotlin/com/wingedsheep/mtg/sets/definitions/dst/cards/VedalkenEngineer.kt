package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Vedalken Engineer
 * {1}{U}
 * Creature — Vedalken Artificer
 * 1/1
 * {T}: Add two mana of any one color. Spend this mana only to cast artifact spells or activate
 * abilities of artifacts.
 *
 * "Two mana of any one color" is [Effects.AddAnyColorMana] with amount 2 — one colour choice, added
 * twice. The spend restriction is Oaken Siren's / Slobad's
 * [ManaRestriction.CardTypeSpellsOrAbilitiesOnly] keyed to artifacts with abilities allowed.
 */
val VedalkenEngineer = card("Vedalken Engineer") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Vedalken Artificer"
    oracleText = "{T}: Add two mana of any one color. Spend this mana only to cast artifact spells or activate abilities of artifacts."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(
            2,
            restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                cardType = CardType.ARTIFACT,
                allowSpells = true,
                allowAbilities = true,
            )
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Lars Grant-West"
        flavorText = "Art is unknown to the vedalken—for them, all creations must serve a purpose."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d06a2d9a-9401-4711-97b6-825652090c4d.jpg"
    }
}
