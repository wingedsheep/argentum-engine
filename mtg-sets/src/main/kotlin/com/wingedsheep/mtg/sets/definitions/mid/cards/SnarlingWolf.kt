package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Snarling Wolf
 * {G}
 * Creature — Wolf
 * 1/1
 *
 * {1}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn.
 *
 * Canonical printing is Innistrad: Midnight Hunt (earliest real set); Innistrad: Crimson Vow
 * gets a reprint row.
 */
val SnarlingWolf = card("Snarling Wolf") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    oracleText = "{1}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "199"
        artist = "Ilse Gort"
        flavorText = "\"Oh, thank the angels. It's not a werewolf, just a regular wo—\"\n—Bruno, Ulvenwald guide, last words"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b614b21-69ed-4788-97d7-ad502b634abb.jpg?1783925573"
    }
}
