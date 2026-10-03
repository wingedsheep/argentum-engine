package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Stump Stomp {1}{R/G} // Burnwillow Clearing
 * Sorcery
 * Target creature you control deals damage equal to its power to target creature or planeswalker
 * you don't control.
 * //
 * Land
 * This land enters tapped.
 * {T}: Add {R} or {G}.
 */
private val StumpStompFront = card("Stump Stomp") {
    manaCost = "{1}{R/G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Target creature you control deals damage equal to its power to target creature or planeswalker you don't control."

    spell {
        val yours = target(TargetFilter.Creature.youControl())
        val theirs = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.DealDamage(DynamicAmounts.powerOf(yours), theirs, damageSource = yours)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "259"
        artist = "Volkan Baǵa"
        flavorText = "\"Don't be a superstitious coward. What's a tree going to do, slowly fall on us?\"\n—Sebastian the Logger"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49974246-0a3b-4ec9-b5ea-2a89df9bb0b5.jpg?1783911225"
    }
}

private val BurnwillowClearingBack = card("Burnwillow Clearing") {
    typeLine = "Land"
    colorIdentity = "RG"
    oracleText = "This land enters tapped.\n{T}: Add {R} or {G}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
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
        collectorNumber = "259"
        artist = "Volkan Baǵa"
        flavorText = "The loggers never questioned why the last crew had abandoned camp."
        imageUri = "https://cards.scryfall.io/normal/back/4/9/49974246-0a3b-4ec9-b5ea-2a89df9bb0b5.jpg?1783911225"
    }
}

val StumpStomp: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = StumpStompFront,
    backFace = BurnwillowClearingBack,
)
