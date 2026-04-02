package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Baylen, the Haymaker
 * {R}{G}{W}
 * Legendary Creature — Rabbit Warrior
 * 4/3
 * Tap two untapped tokens you control: Add one mana of any color.
 * Tap three untapped tokens you control: Draw a card.
 * Tap four untapped tokens you control: Put three +1/+1 counters on Baylen. It gains trample until end of turn.
 */
val BaylenTheHaymaker = card("Baylen, the Haymaker") {
    manaCost = "{R}{G}{W}"
    typeLine = "Legendary Creature — Rabbit Warrior"
    power = 4
    toughness = 3
    oracleText = "Tap two untapped tokens you control: Add one mana of any color.\nTap three untapped tokens you control: Draw a card.\nTap four untapped tokens you control: Put three +1/+1 counters on Baylen. It gains trample until end of turn."

    activatedAbility {
        cost = AbilityCost.TapPermanents(2, GameObjectFilter.Token)
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.TapPermanents(3, GameObjectFilter.Token)
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = AbilityCost.TapPermanents(4, GameObjectFilter.Token)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self)
            .then(Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00e93be2-e06b-4774-8ba5-ccf82a6da1d8.jpg?1721427006"
    }
}
