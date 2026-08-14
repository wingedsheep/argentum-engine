package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Heartwood Shard — Mirrodin #184
 * {3} · Artifact
 *
 * {3}, {T} or {G}, {T}: Target creature gains trample until end of turn.
 *
 * Modelling notes:
 * - The printed "{3}, {T} or {G}, {T}" is a single ability with two alternative cost options. The
 *   engine has no alternative-cost shape for activated abilities, and modelling it as two separate
 *   activated abilities is functionally identical here: both cost a tap of the Shard, so only one
 *   can ever be activated per untap cycle, and neither ability references the other. The player
 *   picks the cost they can afford straight from the action menu.
 * - The rest of the Shard cycle (Crystal, Granite, Pearl, Skeleton) is the same two-ability shape.
 */
val HeartwoodShard = card("Heartwood Shard") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}, {T} or {G}, {T}: Target creature gains trample until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val t = target("target", Targets.Creature)
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, t)
        description = "{3}, {T}: Target creature gains trample until end of turn."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        val t = target("target", Targets.Creature)
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, t)
        description = "{G}, {T}: Target creature gains trample until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Doug Chaffee"
        flavorText = "Like all other relics, it was left on the Radix by the elves to be destroyed. " +
            "Unlike all other relics, it persisted."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b13328a9-fc03-4c1b-b1b7-61cd41766300.jpg?1783944518"

        ruling("2004-10-04", "You can pay either of the two costs (but not both at the same time) to activate the ability.")
    }
}
