package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Revitalizing Repast {B/G} // Old-Growth Grove
 * Instant
 * Put a +1/+1 counter on target creature. It gains indestructible until end of turn.
 * //
 * Land
 * This land enters tapped.
 * {T}: Add {B} or {G}.
 */
private val RevitalizingRepastFront = card("Revitalizing Repast") {
    manaCost = "{B/G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature. It gains indestructible until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "256"
        artist = "Raoul Vitale"
        flavorText = "\"Today, we feast! Tomorrow, we fight! Then, we feast again!\"\n—Harald, king of Skemfar"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03522b6b-31ec-4126-8885-5dbb2248688b.jpg?1783911227"
    }
}

private val OldGrowthGroveBack = card("Old-Growth Grove") {
    typeLine = "Land"
    colorIdentity = "BG"
    oracleText = "This land enters tapped.\n{T}: Add {B} or {G}."

    replacementEffect(EntersTapped())

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

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "256"
        artist = "Raoul Vitale"
        flavorText = "The most potent elvish tonics and potions are brewed from the trees containing their imprisoned gods, the Einir."
        imageUri = "https://cards.scryfall.io/normal/back/0/3/03522b6b-31ec-4126-8885-5dbb2248688b.jpg?1783911227"
    }
}

val RevitalizingRepast: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = RevitalizingRepastFront,
    backFace = OldGrowthGroveBack,
)
