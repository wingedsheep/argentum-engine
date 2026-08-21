package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blazing Rootwalla — Modern Horizons 2 #115
 * {R} · Creature — Lizard · 1 / 1
 *
 * {R}: This creature gets +2/+0 until end of turn. Activate only once each turn.
 * Madness {0} (If you discard this card, discard it into exile. When you do, cast it for its madness cost or put it into your graveyard.)
 *
 * "Activate only once each turn" is [ActivationRestriction.OncePerTurn] on the ability, not a
 * cost or a condition — the engine refuses the activation outright once the per-turn counter is
 * spent (CR 602.5d), which is what keeps it off the stack rather than fizzling it later.
 *
 * Madness {0} is a real zero cost, not the absence of one: `madness("{0}")` parses to a payable
 * cost, so the madness trigger still asks whether you want to cast the card. `CardBuilder.build()`
 * derives the printed `Keyword.MADNESS` from the keyword ability, so the bare keyword is never
 * written next to it.
 */
val BlazingRootwalla = card("Blazing Rootwalla") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard"
    power = 1
    toughness = 1
    oracleText = "{R}: This creature gets +2/+0 until end of turn. Activate only once each turn.\n" +
        "Madness {0} (If you discard this card, discard it into exile. When you do, cast it for its madness cost or put it into your graveyard.)"

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    madness("{0}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Jokubas Uogintas"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4404fc9c-ef02-479c-9638-0cc163f0b48f.jpg?1783926849"
    }
}
