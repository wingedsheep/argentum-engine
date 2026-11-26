package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nicol Bolas, Planeswalker — Conflux #120 (canonical printing; reprinted in M13).
 *
 * Current Oracle targets "player or planeswalker" for the −9. The discard and sacrifice follow
 * the target's kind (as Rakdos's Return does): a targeted player acts directly through the
 * target slot; a targeted planeswalker's controller acts through [EffectTarget.TargetController].
 * The counts are fixed at seven, independent of how much damage is actually dealt.
 */
val NicolBolasPlaneswalker = card("Nicol Bolas, Planeswalker") {
    manaCost = "{4}{U}{B}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Planeswalker — Bolas"
    startingLoyalty = 5
    oracleText = "+3: Destroy target noncreature permanent.\n" +
        "−2: Gain control of target creature.\n" +
        "−9: Nicol Bolas deals 7 damage to target player or planeswalker. That player or that " +
        "planeswalker's controller discards seven cards, then sacrifices seven permanents of their choice."

    loyaltyAbility(+3) {
        val permanent = target(TargetFilter.NoncreaturePermanent)
        effect = Effects.Destroy(permanent)
    }

    loyaltyAbility(-2) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature)
    }

    loyaltyAbility(-9) {
        val recipient = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(7, recipient) then
            Effects.If(
                condition = Conditions.TargetIsPlayer(0),
                then = Effects.Discard(7, recipient) then
                    Effects.Sacrifice(GameObjectFilter.Permanent, 7, recipient),
                otherwise = Effects.Discard(7, EffectTarget.TargetController) then
                    Effects.Sacrifice(GameObjectFilter.Permanent, 7, EffectTarget.TargetController)
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "120"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48ee3939-bc12-4275-a446-9de36f0b4672.jpg?1783942466"
        ruling("2012-07-01", "If the damage from Nicol Bolas's third ability is prevented or redirected, the rest of the effect will still happen. Specifically, the number of cards discarded and permanents sacrificed isn't tied to the amount of damage dealt.")
        ruling("2012-07-01", "For the third ability, if the player is an illegal target when the ability tries to resolve, the ability doesn't resolve and none of its effects will happen. No damage will be dealt, no cards will be discarded, and no permanents will be sacrificed.")
        ruling("2009-02-01", "The control-change effect has no duration. It will last until the game ends or the affected creature leaves the battlefield. It may be superseded by a later control-change effect.")
    }
}
