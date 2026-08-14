package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Out Cold — Murders at Karlov Manor #66
 * {3}{U} · Instant
 *
 * This spell can't be countered. (This includes by the ward ability.)
 * Tap up to two target creatures and put a stun counter on each of them. Investigate.
 *
 * `cantBeCountered` stamps `CantBeCounteredComponent` on the spell, which every counter path —
 * ward included, since ward routes through `StackResolver.counterSpell` — checks before removing
 * the spell from the stack. That is exactly the printed ruling: targeting a warded creature still
 * *offers* the ward cost, and declining leaves Out Cold on the stack to resolve anyway.
 *
 * "Up to two target creatures" is **one** target requirement taking up to two objects
 * (`TargetCreature(count = 2, optional = true)` → `minCount = 0`), not two independent slots — so
 * the two picks must be different creatures (CR 601.2c) and choosing zero is legal. The tap and the
 * stun counter are applied once per chosen creature via [ForEachTargetEffect]; with zero targets
 * chosen the iteration is empty and only the investigate happens.
 *
 * The investigate sits *outside* the per-target iteration: the card investigates once, not once per
 * creature tapped. It is also not conditional on any creature actually being tapped — but per the
 * printed ruling, if targets were chosen and all of them are illegal on resolution the whole spell
 * is countered by the rules and you don't investigate either, which falls out of ordinary
 * fizzle handling rather than anything modelled here.
 */
val OutCold = card("Out Cold") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell can't be countered. (This includes by the ward ability.)\n" +
        "Tap up to two target creatures and put a stun counter on each of them. Investigate. " +
        "(If a permanent with a stun counter would become untapped, remove one from it instead.)"

    cantBeCountered = true

    spell {
        target("up to two target creatures", TargetCreature(count = 2, optional = true))
        effect = Effects.Composite(
            ForEachTargetEffect(
                listOf(
                    Effects.Tap(EffectTarget.ContextTarget(0)),
                    Effects.AddCounters(Counters.STUN, 1, EffectTarget.ContextTarget(0))
                )
            ),
            Effects.Investigate()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Tuan Duong Chu"
        flavorText = "\"We'll get their statements after they defrost.\"\n" +
            "—Dvika of the Foundway Associates"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aabfada0-3c1b-4237-b06c-573071ccd68d.jpg?1783912907"

        ruling(
            "2024-02-02",
            "If you target a creature with ward, you may still pay the ward cost, but Out Cold " +
                "won't be countered even if you don't."
        )
        ruling(
            "2024-02-02",
            "You don't have to choose any targets for Out Cold. However, if you do, and all of " +
                "those creatures are illegal targets at the time Out Cold tries to resolve, it " +
                "won't resolve and none of its effects will happen. You won't investigate."
        )
        ruling(
            "2024-02-02",
            "Clue is an artifact type. Even though it appears on some cards with other permanent " +
                "types, it's never a creature type, a land type, or anything but an artifact type."
        )
    }
}
