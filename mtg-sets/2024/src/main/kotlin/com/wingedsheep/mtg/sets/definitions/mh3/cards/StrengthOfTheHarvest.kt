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
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Strength of the Harvest {2}{G/W} // Haven of the Harvest
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 for each creature and/or enchantment you control.
 * //
 * Land
 * This land enters tapped.
 * {T}: Add {G} or {W}.
 */
private val StrengthOfTheHarvestFront = card("Strength of the Harvest") {
    manaCost = "{2}{G/W}"
    colorIdentity = "GW"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 for each creature and/or enchantment you control."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        // "creature and/or enchantment": each permanent counts once, even an enchantment creature.
        val count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.CreatureOrEnchantment).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = count,
            toughnessBonus = count,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "258"
        artist = "Paolo Parente"
        flavorText = "The Kelema Veil flows through Setessa, allowing Karametra's faithful followers direct access to her wisdom and power."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7143aa7-b16d-4e63-910c-6ceec55483f3.jpg?1783911224"
    }
}

private val HavenOfTheHarvestBack = card("Haven of the Harvest") {
    typeLine = "Land"
    colorIdentity = "GW"
    oracleText = "This land enters tapped.\n{T}: Add {G} or {W}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "258"
        artist = "Paolo Parente"
        flavorText = "\"For us, a temple is a part of nature, not something we impose upon it.\"\n—Sythis, Harvest's Hand"
        imageUri = "https://cards.scryfall.io/normal/back/a/7/a7143aa7-b16d-4e63-910c-6ceec55483f3.jpg?1783911224"
    }
}

val StrengthOfTheHarvest: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = StrengthOfTheHarvestFront,
    backFace = HavenOfTheHarvestBack,
)
