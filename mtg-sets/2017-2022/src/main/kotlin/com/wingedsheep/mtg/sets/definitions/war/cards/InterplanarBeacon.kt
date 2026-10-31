package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Interplanar Beacon
 * Land
 *
 * Whenever you cast a planeswalker spell, you gain 1 life.
 * {T}: Add {C}.
 * {1}, {T}: Add two mana of different colors. Spend this mana only to cast planeswalker spells.
 */
val InterplanarBeacon = card("Interplanar Beacon") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "Whenever you cast a planeswalker spell, you gain 1 life.\n" +
        "{T}: Add {C}.\n" +
        "{1}, {T}: Add two mana of different colors. Spend this mana only to cast planeswalker spells."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Planeswalker)
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddManaOfDifferentColors(
            2,
            restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.PLANESWALKER)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/132d8e2a-e1dd-4867-8e8c-e48b9450b350.jpg?1783933367"
    }
}
