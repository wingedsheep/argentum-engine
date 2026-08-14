package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ulvenwald Oddity // Ulvenwald Behemoth (Innistrad: Crimson Vow)
 * {2}{G}{G}
 * Creature — Beast // Creature — Beast Horror
 *
 * Front — Ulvenwald Oddity (4/4)
 *   Trample, haste
 *   {5}{G}{G}: Transform this creature.
 *
 * Back — Ulvenwald Behemoth (8/8)
 *   Trample, haste
 *   Other creatures you control get +1/+1 and have trample and haste.
 *
 * The back's anthem is the Elvish Champion idiom: a +1/+1 [ModifyStats] plus two [GrantKeyword]
 * statics, all over [GroupFilter.OtherCreaturesYouControl] (excludeSelf, so the Behemoth's own
 * printed trample/haste aren't double-counted and it isn't pumped). The back is a transformed face
 * with no mana cost, so its color comes from a color indicator (CR 204): `colorIndicator = "G"`.
 */

private val UlvenwaldOddityFront = card("Ulvenwald Oddity") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Trample, haste\n" +
        "{5}{G}{G}: Transform this creature."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    activatedAbility {
        cost = Costs.Mana("{5}{G}{G}")
        effect = TransformEffect(EffectTarget.Self)
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Brent Hollowell"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fdf5fc4-69c8-4a59-9095-c2feefb64371.jpg?1783924806"
    }
}

private val UlvenwaldBehemoth = card("Ulvenwald Behemoth") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Beast Horror"
    power = 8
    toughness = 8
    oracleText = "Trample, haste\n" +
        "Other creatures you control get +1/+1 and have trample and haste."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter.OtherCreaturesYouControl)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.OtherCreaturesYouControl)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.OtherCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Brent Hollowell"
        imageUri = "https://cards.scryfall.io/normal/back/5/f/5fdf5fc4-69c8-4a59-9095-c2feefb64371.jpg?1783924806"
    }
}

val UlvenwaldOddity: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = UlvenwaldOddityFront,
    backFace = UlvenwaldBehemoth,
)
