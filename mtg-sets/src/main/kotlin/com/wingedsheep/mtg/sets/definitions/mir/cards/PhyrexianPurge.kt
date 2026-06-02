package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.dsl.Costs

/**
 * Phyrexian Purge
 * {2}{B}{R}
 * Sorcery
 *
 * This spell costs 3 life more to cast for each target.
 * Destroy any number of target creatures.
 *
 * The life payment is an additional cost paid at cast time; it is not refunded if the
 * spell is countered or if targets become illegal (Scryfall ruling, 2009-10-01).
 *
 * Implementation: "any number of target creatures" uses `unlimited = true`, so the
 * target count has no cap — the client offers every legal target. The per-target life
 * payment uses [Costs.additional.PayLifePerTarget] — the engine multiplies the
 * payment by `action.targets.size` at cast resolution.
 */
val PhyrexianPurge = card("Phyrexian Purge") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "This spell costs 3 life more to cast for each target.\nDestroy any number of target creatures."

    additionalCost(Costs.additional.PayLifePerTarget(amountPerTarget = 3))

    spell {
        target = TargetCreature(unlimited = true)
        effect = ForEachTargetEffect(listOf(Effects.Destroy(EffectTarget.ContextTarget(0))))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "273"
        artist = "Robert Bliss"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/312bbc1b-4c2a-44c1-8e62-c0f94fd2ba8e.jpg?1562718334"
        ruling("2009-10-01", "The life payment is an additional cost, paid at the time you cast Phyrexian Purge. You don't get any life back if the spell is countered, or if one or more targets have become illegal.")
    }
}
