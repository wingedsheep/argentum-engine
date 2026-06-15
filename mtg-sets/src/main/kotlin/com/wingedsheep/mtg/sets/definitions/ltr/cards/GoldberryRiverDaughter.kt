package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Goldberry, River-Daughter
 * {1}{U}
 * Legendary Creature — Nymph
 * 1/3
 *
 * {T}: Move a counter of each kind not on Goldberry from another target permanent you control
 * onto Goldberry.
 * {U}, {T}: Move one or more counters from Goldberry onto another target permanent you control.
 * If you do, draw a card.
 */
val GoldberryRiverDaughter = card("Goldberry, River-Daughter") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Nymph"
    power = 1
    toughness = 3
    oracleText = "{T}: Move a counter of each kind not on Goldberry from another target permanent " +
        "you control onto Goldberry.\n" +
        "{U}, {T}: Move one or more counters from Goldberry onto another target permanent you control. " +
        "If you do, draw a card."

    // {T}: Move a counter of each kind not on Goldberry from another target permanent you control onto Goldberry.
    activatedAbility {
        cost = Costs.Tap
        val source = target(
            "another target permanent you control",
            TargetPermanent(filter = TargetFilter.PermanentYouControl.other())
        )
        effect = Effects.MoveCountersEachKindMissing(
            source = source,
            destination = EffectTarget.Self
        )
    }

    // {U}, {T}: Move one or more counters from Goldberry onto another target permanent you control. If you do, draw a card.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap)
        val destination = target(
            "another target permanent you control",
            TargetPermanent(filter = TargetFilter.PermanentYouControl.other())
        )
        effect = Effects.MoveChosenCountersToTarget(
            source = EffectTarget.Self,
            destination = destination,
            drawCardOnMove = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Marie Magny"
        flavorText = "\"Fear nothing! For tonight you are under the roof of Tom Bombadil.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf4f0a7c-620e-4ed8-8da3-fa6564e8a0cd.jpg?1686968111"
    }
}
