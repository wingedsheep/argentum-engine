package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Freelance Muscle — Streets of New Capenna #147 (canonical printing)
 * {4}{G} · Creature — Rhino Warrior · 4/4
 *
 * Whenever this creature attacks or blocks, it gets +X/+X until end of turn, where X is the
 * greatest power and/or toughness among other creatures you control.
 *
 * "Greatest power and/or toughness" is the single largest number printed in either box across
 * your other creatures, so X is the [DynamicAmounts.max] of the MAX-power and MAX-toughness
 * aggregates over *other* creatures you control (`excludeSelf = true`), read off projected state.
 * No other creatures leaves both aggregates at 0, so the pump is +0/+0. X is evaluated once as
 * the trigger resolves and locked in for the turn.
 *
 * "Attacks or blocks" is two triggered abilities sharing one effect (the Merfolk Skyscout shape).
 */
private val otherCreatures = DynamicAmounts.battlefield(
    Player.You,
    GameObjectFilter.Creature,
    excludeSelf = true,
)

private val greatestPowerOrToughness = DynamicAmounts.max(
    otherCreatures.maxPower(),
    otherCreatures.maxToughness(),
)

val FreelanceMuscle = card("Freelance Muscle") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rhino Warrior"
    power = 4
    toughness = 4
    oracleText = "Whenever this creature attacks or blocks, it gets +X/+X until end of turn, where X is the greatest power and/or toughness among other creatures you control."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(greatestPowerOrToughness, greatestPowerOrToughness, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.ModifyStats(greatestPowerOrToughness, greatestPowerOrToughness, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Randy Vargas"
        flavorText = "He never asks why. Just who, when, and where."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/570f7c90-109b-4d6d-8205-d158753719e8.jpg?1783923105"
    }
}
