package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Glasswing Grace {3}{W/B}{W/B} // Age-Graced Chapel
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2 and has flying and lifelink.
 * //
 * Land
 * This land enters tapped.
 * {T}: Add {W} or {B}.
 */
private val GlasswingGraceFront = card("Glasswing Grace") {
    manaCost = "{3}{W/B}{W/B}"
    colorIdentity = "WB"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/+2 and has flying and lifelink."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "254"
        artist = "Craig Elliott"
        flavorText = "Serra may be long gone, but Benalia's faithful still draw strength from her blessings."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90630b20-fc83-475f-bcd5-8bcfee0cf241.jpg?1783911225"
    }
}

private val AgeGracedChapelBack = card("Age-Graced Chapel") {
    typeLine = "Land"
    colorIdentity = "WB"
    oracleText = "This land enters tapped.\n{T}: Add {W} or {B}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "254"
        artist = "Maxime Minard"
        flavorText = "\"Empires fall, cities crumble, righteousness remains.\"\n—Chapel inscription"
        imageUri = "https://cards.scryfall.io/normal/back/9/0/90630b20-fc83-475f-bcd5-8bcfee0cf241.jpg?1783911225"
    }
}

val GlasswingGrace: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = GlasswingGraceFront,
    backFace = AgeGracedChapelBack,
)
