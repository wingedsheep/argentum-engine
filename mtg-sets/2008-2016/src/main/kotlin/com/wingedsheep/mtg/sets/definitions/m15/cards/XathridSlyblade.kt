package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Xathrid Slyblade — Magic 2015 #124 (canonical printing)
 * {2}{B} · Creature — Human Assassin · 2/1
 *
 * Hexproof
 * {3}{B}: Until end of turn, this creature loses hexproof and gains first strike and deathtouch.
 *
 * Losing hexproof is a real until-end-of-turn keyword removal (layer 6), so for the rest of the
 * turn opponents can target it, and it regains hexproof at cleanup.
 */
val XathridSlyblade = card("Xathrid Slyblade") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Assassin"
    power = 2
    toughness = 1
    oracleText = "Hexproof (This creature can't be the target of spells or abilities your opponents control.)\n" +
        "{3}{B}: Until end of turn, this creature loses hexproof and gains first strike and deathtouch. " +
        "(It deals combat damage before creatures without first strike. Any amount of damage it deals to a creature is enough to destroy it.)"

    keywords(Keyword.HEXPROOF)

    activatedAbility {
        cost = Costs.Mana("{3}{B}")
        effect = Effects.RemoveKeyword(Keyword.HEXPROOF, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
        description = "{3}{B}: Until end of turn, this creature loses hexproof and gains first strike and deathtouch."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Steve Prescott"
        flavorText = "Designed by Rob Pardo"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/909a9252-1e97-4e85-9648-66050f301f8a.jpg?1783939178"
    }
}
