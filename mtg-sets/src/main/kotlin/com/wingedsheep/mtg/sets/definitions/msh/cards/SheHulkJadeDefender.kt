package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * She-Hulk, Jade Defender — Marvel Super Heroes #188 (uncommon)
 * {3}{G} · Legendary Creature — Gamma Hero · 4/4
 *
 * Reach, trample
 * Power-up — {4}{G}{G}: Destroy up to one target artifact or enchantment. Put a +1/+1 counter on
 * She-Hulk. (Activate each power-up ability only once. Reduce the cost by her mana cost if she
 * entered this turn.)
 *
 * "**Up to one** target" is `optional = true`, which drops the requirement's minimum count to
 * zero — the ability can be activated with no target at all when the opponent has no artifact or
 * enchantment, and still resolves to put the counter on. Modelling it as a plain single target
 * would make the ability unactivatable on an empty board, which is the common case in this
 * format.
 *
 * `{4}{G}{G}` − `{3}{G}` = `{1}{G}`, the only power-up in the cycle whose reduction leaves a
 * colored pip behind.
 */
val SheHulkJadeDefender = card("She-Hulk, Jade Defender") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Gamma Hero"
    oracleText = "Reach, trample\n" +
        "Power-up — {4}{G}{G}: Destroy up to one target artifact or enchantment. Put a +1/+1 " +
        "counter on She-Hulk. (Activate each power-up ability only once. Reduce the cost by her " +
        "mana cost if she entered this turn.)"
    power = 4
    toughness = 4

    keywords(Keyword.REACH, Keyword.TRAMPLE)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{4}{G}{G}")
        val victim = target(
            "up to one target artifact or enchantment",
            TargetPermanent(optional = true, filter = TargetFilter.ArtifactOrEnchantment)
        )
        effect = Effects.Composite(
            Effects.Destroy(victim),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Tyler Walpole"
        flavorText = "\"Buddy, you blew through the wrong stop sign.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d22a8d02-023c-4f71-a771-16b5aa2a05d7.jpg?1783902911"
    }
}
