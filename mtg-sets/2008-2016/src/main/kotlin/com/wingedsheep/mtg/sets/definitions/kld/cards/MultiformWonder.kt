package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Multiform Wonder — Kaladesh #223
 * {5} · Artifact Creature — Construct · 3/3
 *
 * When this creature enters, you get {E}{E}{E} (three energy counters).
 * Pay {E}: This creature gains your choice of flying, vigilance, or lifelink until end of turn.
 * Pay {E}: This creature gets +2/-2 or -2/+2 until end of turn.
 *
 * Both "your choice" picks happen on resolution (see ruling), so each ability is a
 * resolution-time `ChooseAction` — Golem Artisan's keyword shape and Pemmin's Aura's
 * +X/-X-or-−X/+X shape on Hightide Hermit's energy cost.
 */
val MultiformWonder = card("Multiform Wonder") {
    manaCost = "{5}"
    typeLine = "Artifact Creature — Construct"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you get {E}{E}{E} (three energy counters).\n" +
        "Pay {E}: This creature gains your choice of flying, vigilance, or lifelink until end of turn.\n" +
        "Pay {E}: This creature gets +2/-2 or -2/+2 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 1)
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "It gains flying until end of turn",
                effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self),
            ),
            EffectChoice(
                label = "It gains vigilance until end of turn",
                effect = Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self),
            ),
            EffectChoice(
                label = "It gains lifelink until end of turn",
                effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self),
            )
        ))
        description = "Pay {E}: This creature gains your choice of flying, vigilance, or lifelink until end of turn."
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 1)
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "It gets +2/-2 until end of turn",
                effect = Effects.ModifyStats(2, -2, EffectTarget.Self),
            ),
            EffectChoice(
                label = "It gets -2/+2 until end of turn",
                effect = Effects.ModifyStats(-2, 2, EffectTarget.Self),
            )
        ))
        description = "Pay {E}: This creature gets +2/-2 or -2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f6956c1-36e2-4c9c-9f69-00b459f094d8.jpg?1783937152"
        ruling(
            "2016-09-20",
            "As you activate one of Multiform Wonder's two activated abilities, you must announce which one " +
                "you're activating. However, you don't choose that ability's effect until it resolves.",
        )
    }
}
