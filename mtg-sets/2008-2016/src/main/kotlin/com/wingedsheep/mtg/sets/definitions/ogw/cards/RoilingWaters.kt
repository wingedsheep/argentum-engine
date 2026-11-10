package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Roiling Waters
 * {5}{U}{U}
 * Sorcery
 * Return up to two target creatures your opponents control to their owners' hands.
 * Target player draws two cards.
 */
val RoilingWaters = card("Roiling Waters") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return up to two target creatures your opponents control to their owners' hands. " +
        "Target player draws two cards."

    spell {
        targets(TargetFilter.CreatureOpponentControls, count = 2, optional = true)
        val player = target(Targets.Player)
        effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0))) then
            Effects.DrawCards(2, player)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "David Gaillet"
        flavorText = "\"The Roil may be unpredictable, but it is not uncontrollable.\"\n—Noyan Dar, Tazeem roilmage"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d878c10-de5e-4224-98ce-d49902b5ab8b.jpg"
    }
}
