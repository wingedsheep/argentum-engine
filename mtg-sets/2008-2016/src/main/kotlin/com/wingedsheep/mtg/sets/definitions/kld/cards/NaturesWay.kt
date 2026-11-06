package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Nature's Way — Kaladesh #162
 * {1}{G} · Sorcery
 *
 * Target creature you control gains vigilance and trample until end of turn. It deals damage
 * equal to its power to target creature you don't control.
 *
 * One-sided bite (Run Over's shape) preceded by two keyword grants on the biter.
 */
val NaturesWay = card("Nature's Way") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control gains vigilance and trample until end of turn. " +
        "It deals damage equal to its power to target creature you don't control."

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.GrantKeyword(Keyword.VIGILANCE, mine) then
            Effects.GrantKeyword(Keyword.TRAMPLE, mine) then
            Effects.DealDamage(
                amount = DynamicAmounts.powerOf(mine),
                target = theirs,
                damageSource = mine,
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Zack Stella"
        flavorText = "Many elves incorporate the beauty of nature into their inventions. Very few capture its savagery."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec122287-fe33-4ac3-803c-f5173cae50a7.jpg?1783937175"
    }
}
