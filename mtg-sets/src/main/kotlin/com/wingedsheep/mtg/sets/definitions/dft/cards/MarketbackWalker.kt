package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Marketback Walker — Aetherdrift #235
 * {X}{X} · Artifact Creature — Construct · 0/0
 *
 * This creature enters with X +1/+1 counters on it.
 * {4}: Put a +1/+1 counter on this creature.
 * When this creature dies, draw a card for each +1/+1 counter on it.
 *
 * The cost prints **two** {X} symbols: the caster announces one value for X (CR 601.2b) and pays
 * it twice, which `ManaCost.xCount` already drives through the solver and the payment path. The
 * counters use the announced X once — [DynamicAmount.XValue] — so a 4/4 costs eight mana.
 *
 * The dies trigger reads [ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT] rather than the
 * live entity: by the time it resolves the Walker is a graveyard card with no counters, so the
 * count must come from last-known information (CR 603.10 / 608.2g). A Walker that dies with zero
 * counters — cast for X=0, which state-based actions kill immediately as a 0/0 — still triggers
 * and simply draws nothing.
 */
val MarketbackWalker = card("Marketback Walker") {
    manaCost = "{X}{X}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 0
    oracleText = "This creature enters with X +1/+1 counters on it.\n" +
        "{4}: Put a +1/+1 counter on this creature.\n" +
        "When this creature dies, draw a card for each +1/+1 counter on it."

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmount.XValue))

    activatedAbility {
        cost = Costs.Mana("{4}")
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "{4}: Put a +1/+1 counter on this creature."
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.DrawCards(
            DynamicAmount.ContextProperty(ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT)
        )
        description = "When this creature dies, draw a card for each +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Svetlin Velinov"
        flavorText = "Many of the inventions built to fight the Phyrexians have since been " +
            "repurposed for civilian use."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2152030-6007-493f-a616-545723a00249.jpg?1783907848"
    }
}
