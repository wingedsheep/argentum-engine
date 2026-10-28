package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Spikefield Hazard {R} // Spikefield Cave — Zendikar Rising #166
 * Instant // Land · Modal double-faced card
 *
 * Front — Spikefield Hazard: "Spikefield Hazard deals 1 damage to any target. If a permanent dealt
 * damage this way would die this turn, exile it instead."
 * Back — Spikefield Cave: "This land enters tapped. {T}: Add {R}."
 *
 * "Dealt damage this way" gates the exile mark on damage actually dealt (Draconic Intervention's
 * shape): prevented damage leaves no mark. The mark is a no-op on a player, and "die" covers any
 * permanent, so a planeswalker knocked to 0 loyalty is exiled too.
 */
private val SpikefieldHazardFront = card("Spikefield Hazard") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Spikefield Hazard deals 1 damage to any target. If a permanent dealt damage this way " +
        "would die this turn, exile it instead."

    spell {
        val t = target(Targets.Any)
        effect = Effects.Pipeline {
            val dealt = runStoringNumber {
                Effects.DealDamage(1, t, damageDealtVariable = it)
            }
            ifThen(Conditions.CompareAmounts(dealt.amount, ComparisonOperator.GT, 0)) {
                run(Effects.MarkExileOnDeath(t))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Stop screaming! You'll only bring down more spikes.\"\n—Raff Slugeater, goblin shortcutter"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a69541db-3f4e-412f-aa8e-dec1e74f74dc.jpg?1783929354"
    }
}

private val SpikefieldCaveBack = card("Spikefield Cave") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "This land enters tapped.\n{T}: Add {R}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Silence until we're through. Even a whisper's echo can dislodge death from above.\"\n—Raff Slugeater, goblin shortcutter"
        imageUri = "https://cards.scryfall.io/normal/back/a/6/a69541db-3f4e-412f-aa8e-dec1e74f74dc.jpg?1783929354"
    }
}

val SpikefieldHazard: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = SpikefieldHazardFront,
    backFace = SpikefieldCaveBack,
)
