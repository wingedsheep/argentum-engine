package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Skull Prophet
 * {B}{G}
 * Creature — Human Druid
 * 3/1
 * {T}: Add {B} or {G}.
 * {T}: Mill two cards. (Put the top two cards of your library into your graveyard.)
 */
val SkullProphet = card("Skull Prophet") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Human Druid"
    power = 3
    toughness = 1
    oracleText = "{T}: Add {B} or {G}.\n" +
        "{T}: Mill two cards. (Put the top two cards of your library into your graveyard.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Library.mill(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Nils Hamm"
        flavorText = "\"I study bone because that's where truth dwells. One monster knucklebone is wiser " +
            "than all that damp meat in your head.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f5ec787-79f6-4922-a0f7-debde8f7a4be.jpg?1783931018"
    }
}
