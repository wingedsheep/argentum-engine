package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tal Shiar Analyst
 * {2}{R}
 * Creature — Romulan Scientist
 * 3/2
 *
 * {T}: This creature deals 1 damage to each opponent.
 * Whenever you cast a noncreature spell, untap this creature.
 *
 * Shivan Gorge's untargeted each-opponent ping on a bare tap cost, plus Zephyr Scribe's
 * noncreature-cast untap trigger, so each noncreature spell refunds one more activation.
 */
val TalShiarAnalyst = card("Tal Shiar Analyst") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Romulan Scientist"
    power = 3
    toughness = 2
    oracleText = "{T}: This creature deals 1 damage to each opponent.\n" +
        "Whenever you cast a noncreature spell, untap this creature."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.Untap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Lucas Durham"
        flavorText = "\"If the scanning apparatus can be modified to transmit on encrypted Romulan frequencies, it could be the perfect eavesdropper.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66daae4f-8024-4d87-b42f-e5271d29e646.jpg?1793361345"
    }
}
