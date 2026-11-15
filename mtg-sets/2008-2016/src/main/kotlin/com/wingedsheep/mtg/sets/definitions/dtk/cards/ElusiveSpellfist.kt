package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elusive Spellfist
 * {1}{U}
 * Creature — Human Monk
 * 1/3
 *
 * Whenever you cast a noncreature spell, this creature gets +1/+0 until end of turn and can't be
 * blocked this turn.
 *
 * A prowess-shaped trigger that is not prowess (+1/+0, not +1/+1), so it is a plain
 * `Triggers.you.casts(GameObjectFilter.Noncreature)` trigger. Both halves are end-of-turn
 * floating effects on [EffectTarget.Self]. The evasion grant only matters at declare blockers —
 * an already-blocked Spellfist stays blocked (the 2017-11-17 ruling).
 */
val ElusiveSpellfist = card("Elusive Spellfist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Monk"
    power = 1
    toughness = 3
    oracleText = "Whenever you cast a noncreature spell, this creature gets +1/+0 until end of turn " +
        "and can't be blocked this turn."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        description = "Whenever you cast a noncreature spell, this creature gets +1/+0 until end of turn " +
            "and can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Viktor Titov"
        flavorText = "\"Learn to see an obstacle from a new perspective. Underneath, for example.\"\n—Chanyi, Ojutai monk"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af13b8c2-9d7d-44d8-8934-16458e7267dc.jpg?1783938608"
        ruling("2017-11-17", "Once Elusive Spellfist has been blocked, causing its ability to trigger won't change or undo that block.")
    }
}
