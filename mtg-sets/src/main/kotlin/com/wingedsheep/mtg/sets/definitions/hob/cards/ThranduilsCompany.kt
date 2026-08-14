package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Thranduil's Company
 * {2}{G}{U}
 * Creature — Elf Soldier
 * 3/4
 * As long as you control another Elf, you may play an additional land on each of your turns.
 * Landfall — Whenever a land you control enters, put two +1/+1 counters on target creature you
 * control. It gains vigilance until end of turn.
 *
 *  - The land-drop clause is a **continuous static gated on a different Elf** — the Bolg's Company
 *    shape: [GrantAdditionalLandDrop] wrapped in the builder's `condition`, evaluated continuously.
 *    Lose the other Elf after playing the extra land and nothing is taken back, but lose it before
 *    and the extra drop is simply unavailable. [Conditions.YouControl]`(excludeSelf = true)` is what
 *    makes it *another* Elf; this card is itself an Elf and must not satisfy its own condition.
 *  - **Landfall** is [Triggers.LandYouControlEnters] — every land entering under your control, not
 *    just the ones you play, and it fires for the extra land drop this card grants too.
 *  - The counters and the vigilance share one target `t`, so an illegal target fizzles both halves.
 */
val ThranduilsCompany = card("Thranduil's Company") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Elf Soldier"
    oracleText = "As long as you control another Elf, you may play an additional land on each of " +
        "your turns.\n" +
        "Landfall — Whenever a land you control enters, put two +1/+1 counters on target creature " +
        "you control. It gains vigilance until end of turn."
    power = 3
    toughness = 4

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
        condition = Conditions.YouControl(
            GameObjectFilter.Creature.withSubtype(Subtype.ELF),
            excludeSelf = true
        )
    }

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        val t = target(
            "target creature you control to get two +1/+1 counters and vigilance",
            TargetCreature(filter = TargetFilter.CreatureYouControl)
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, t)
            .then(Effects.GrantKeyword(Keyword.VIGILANCE, t))
        description = "Landfall — Whenever a land you control enters, put two +1/+1 counters on " +
            "target creature you control. It gains vigilance until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "168"
        artist = "Irina Nordsol"
        flavorText = "They escaped at times to ride or run over open lands by moonlight or starlight."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abdb9d4e-e6ca-409b-b589-0cf71724340b.jpg?1785412736"
    }
}
