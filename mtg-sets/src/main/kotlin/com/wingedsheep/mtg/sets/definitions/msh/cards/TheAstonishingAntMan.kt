package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Astonishing Ant-Man — Marvel Super Heroes #204
 * {G}{U} · Legendary Creature — Human Scientist Hero · 1/1
 *
 * Whenever you draw a card, put a +1/+1 counter on The Astonishing Ant-Man.
 * {2}{G}, {T}, Remove any number of +1/+1 counters from The Astonishing Ant-Man: Create that
 * many 1/1 green Insect creature tokens.
 *
 * Modeling notes:
 *  - The trigger is [Triggers.YouDraw], which fires once per card drawn (CR 121.2) — a
 *    multi-card draw puts on that many counters, one trigger at a time. Cards put into hand
 *    without the word "draw" (CR 121.5) don't trigger it.
 *  - "Remove any number of +1/+1 counters from ~" is the Retribution of the Ancients cost shape:
 *    [Costs.RemoveXCounters] with the count left at its [DynamicAmount.XValue] default and the
 *    filter narrowed to the source permanent itself
 *    (`self = true`), so the counters come off Ant-Man's
 *    own counters. "Any number" includes zero, which the X cost already allows — and X = 0
 *    creates no tokens.
 *  - "That many" is the same X, read back by the dynamic-count [Effects.CreateToken] overload,
 *    so the token count always matches the counters actually paid.
 */
val TheAstonishingAntMan = card("The Astonishing Ant-Man") {
    manaCost = "{G}{U}"
    colorIdentity = "UG"
    typeLine = "Legendary Creature — Human Scientist Hero"
    power = 1
    toughness = 1
    oracleText = "Whenever you draw a card, put a +1/+1 counter on The Astonishing Ant-Man.\n" +
        "{2}{G}, {T}, Remove any number of +1/+1 counters from The Astonishing Ant-Man: " +
        "Create that many 1/1 green Insect creature tokens."

    triggeredAbility {
        trigger = Triggers.YouDraw
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you draw a card, put a +1/+1 counter on The Astonishing Ant-Man."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{G}"),
            Costs.Tap,
            // `self` — the counters come off Ant-Man himself. The filter-based form would ask the
            // player to distribute the removal across matching permanents, which never resolves for
            // a self-scoped cost.
            Costs.RemoveXCounters(
                counterType = Counters.PLUS_ONE_PLUS_ONE,
                self = true,
            ),
        )
        effect = Effects.CreateToken(
            count = DynamicAmount.XValue,
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            imageUri = "https://cards.scryfall.io/normal/front/e/5/e5aa36ec-5f3a-405d-9a65-5a56a44dcee3.jpg?1783902801",
        )
        description = "{2}{G}, {T}, Remove any number of +1/+1 counters from The Astonishing " +
            "Ant-Man: Create that many 1/1 green Insect creature tokens."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d98073e-2828-4365-a8d6-f631aac0cca9.jpg?1783902906"
    }
}
