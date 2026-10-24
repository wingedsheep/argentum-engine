package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Snow Day — Strixhaven: School of Mages #53 (canonical printing)
 * {4}{U}{U} · Instant
 *
 * Tap up to two target creatures. Those creatures don't untap during their controller's next
 * untap step.
 * Draw two cards, then discard a card.
 *
 * The tap-and-freeze half is Saiba Trespassers' shape: each target is tapped and given
 * [AbilityFlag.DOESNT_UNTAP] until after its controller's next untap step, so an already-tapped
 * target is still frozen. The loot follows on the same resolution.
 */
val SnowDay = card("Snow Day") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap up to two target creatures. Those creatures don't untap during their " +
        "controller's next untap step.\n" +
        "Draw two cards, then discard a card."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.ContextTarget(0),
                Duration.UntilAfterAffectedControllersNextUntap()
            )
        ) then
            Effects.DrawCards(2) then
            Patterns.Hand.discardCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Forrest Imel"
        flavorText = "\"Advanced Elemental Manifestations is canceled until all students have " +
            "thawed.\"\n—Prismari dormitory notice"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6b6f17d-45ba-4d60-a963-5eee026f2219.jpg?1783927374"
        ruling("2021-04-16", "If you chose targets for Snow Day as you cast it, and none of those targets are legal as it tries to resolve, it will be removed from the stack and do nothing. You will not draw or discard.")
        ruling("2021-04-16", "You may choose tapped creatures as targets. Those creatures won't untap during their controller's next untap step.")
    }
}
