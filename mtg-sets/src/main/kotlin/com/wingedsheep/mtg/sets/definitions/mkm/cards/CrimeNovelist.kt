package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crime Novelist — Murders at Karlov Manor #121
 * {2}{R} · Creature — Goblin Bard · 1/3
 *
 * Whenever you sacrifice an artifact, put a +1/+1 counter on this creature and add {R}.
 *
 * `YouSacrificeA` is the per-permanent form: "whenever you sacrifice **an** artifact" is one trigger
 * per artifact, so sacrificing three at once grows the Novelist by three and produces {R}{R}{R} — the
 * batch form would give one of each. The Novelist is not itself an artifact, so `YouSacrificeA` and
 * `YouSacrificeAnother` would behave identically here; the printed wording is the non-"another" one.
 *
 * Sacrifices made to *pay a cost* count. Costs are paid while the spell or ability is being put on
 * the stack, so cracking a Clue for its own "{2}, Sacrifice this token: Draw a card" triggers this,
 * and — the reason the card is playable at all — the {R} arrives in time to help cast whatever comes
 * next, not the spell whose cost caused it.
 *
 * This is a *triggered* ability that produces mana, not a mana ability: it uses the stack (it has an
 * effect other than producing mana, CR 605.1a), so opponents get priority and the mana lands in the
 * pool during that trigger's resolution. It therefore empties at the end of the step or phase like
 * any other unspent mana.
 */
val CrimeNovelist = card("Crime Novelist") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Bard"
    oracleText = "Whenever you sacrifice an artifact, put a +1/+1 counter on this creature and add {R}."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.YouSacrificeA(GameObjectFilter.Artifact)
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.AddMana(Color.RED)
        )
        description = "Whenever you sacrifice an artifact, put a +1/+1 counter on this creature and add {R}."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Izzy"
        flavorText = "\"But then he peeled off the mask, revealing a sight that made Groja's blood " +
            "run cold—Brulu was actually Vogos in disguise!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14a5cd7c-b0b1-4ffa-a806-bb0e73baffad.jpg?1783912882"
    }
}
