package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brave Brawler — Marvel Super Heroes #8 (common)
 * {1}{W} · Creature — Human Warrior Hero · 2/1
 *
 * Lifelink
 * Power-up — {4}{W}: Put two +1/+1 counters on this creature. (Activate each power-up ability
 * only once. Reduce the cost by its mana cost if it entered this turn.)
 *
 * The simplest shape in the power-up cycle (CR 702.193), and the reference for the rest of it:
 * `isPowerUp = true` is the whole of the mechanic. The DSL desugars it to
 * `ActivationRestriction.Once` ("Activate each power-up ability only once") and the engine reduces
 * the activation cost by the permanent's own mana cost while it entered this turn — here
 * `{4}{W}` − `{1}{W}` = `{3}`, so Brave Brawler is a two-mana 2/1 lifelinker that can spend the
 * rest of the turn's mana becoming a 4/3, or wait and pay full price later.
 */
val BraveBrawler = card("Brave Brawler") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior Hero"
    oracleText = "Lifelink\n" +
        "Power-up — {4}{W}: Put two +1/+1 counters on this creature. (Activate each power-up " +
        "ability only once. Reduce the cost by its mana cost if it entered this turn.)"
    power = 2
    toughness = 1

    keywords(Keyword.LIFELINK)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{4}{W}")
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Lee Woo-chul"
        flavorText = "\"You guys want a taste, eh? Knuckle sandwiches for everyone!\"\n—D-Man, Dennis Dunphy"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/2242b5c3-42ef-4be0-a61f-65c93e56fcab.jpg?1783902978"
    }
}
