package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Synth Eradicator — Fallout #67.
 * {2}{R} · Artifact Creature — Synth Soldier · 3/3
 *
 * Haste
 * Whenever this creature attacks, exile the top card of your library. You may get {E}{E} (two
 * energy counters). If you don't, you may play that card this turn.
 * {T}, Pay {E}{E}{E}: This creature deals 3 damage to any target.
 *
 * The attack trigger is Spark of Creativity's shape: gather → exile the top card, then an
 * [Effects.May] whose yes-branch is the energy and whose `otherwise` is the end-of-turn may-play
 * grant, so only declining the energy leaves the exiled card playable. The ping is `{T}` plus a
 * fixed [Costs.PayPlayerCounters] energy payment (Dr. Madison Li's shape).
 */
val SynthEradicator = card("Synth Eradicator") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Synth Soldier"
    power = 3
    toughness = 3
    oracleText = "Haste\n" +
        "Whenever this creature attacks, exile the top card of your library. You may get {E}{E} (two " +
        "energy counters). If you don't, you may play that card this turn.\n" +
        "{T}, Pay {E}{E}{E}: This creature deals 3 damage to any target."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(1)))
            exile(exiled)
            run(Effects.May(
                Effects.GetEnergy(2),
                otherwise = Effects.GrantMayPlayFromExile(from = exiled),
                prompt = "Get {E}{E}? (If you don't, you may play the exiled card this turn.)",
            ))
        }
        description = "Whenever this creature attacks, exile the top card of your library. You may get " +
            "{E}{E}. If you don't, you may play that card this turn."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 3))
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(3, anyTarget)
        description = "{T}, Pay {E}{E}{E}: This creature deals 3 damage to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87fc7b54-26e5-4b0d-b2bb-be8f977f752b.jpg?1783912401"
        ruling(
            "2024-03-08",
            "You pay all costs and follow all normal timing rules for the card played from exile with " +
                "Synth Eradicator's second ability. For example, if the exiled card is a land card, you " +
                "may play it only during your main phase while the stack is empty."
        )
    }
}
