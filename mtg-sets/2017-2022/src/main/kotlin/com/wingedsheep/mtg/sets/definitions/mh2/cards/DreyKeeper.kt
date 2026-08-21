package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Drey Keeper — Modern Horizons 2 #194
 * {3}{B}{G} · Creature — Elf Druid · 2 / 2
 *
 * When this creature enters, create two 1/1 green Squirrel creature tokens.
 * {3}{B}: Squirrels you control get +1/+0 and gain menace until end of turn.
 *
 * "Squirrels you control" is a **bare tribal noun**: it names every *permanent* you control with
 * that subtype, not only the creature ones, so the filter is [GameObjectFilter.Permanent]
 * `.withSubtype(...)` rather than `Creature.withSubtype(...)`. The distinction is unobservable
 * with today's card pool — every printed Squirrel is a creature — but a noncreature Squirrel (or
 * a Squirrel land) would be pumped by the printed text, and the creature form is what the
 * differential gate flags as a defect.
 *
 * A pump-a-group effect is [Effects.ForEachInGroup] with [EffectTarget.Self] naming the *iterated*
 * permanent, and the two clauses ("get +1/+0" and "gain menace") are one composite body applied
 * per Squirrel — the group is snapshotted before iteration, so a Squirrel created afterwards this
 * turn is untouched, exactly as the one-shot resolution should behave.
 */
val DreyKeeper = card("Drey Keeper") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Elf Druid"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, create two 1/1 green Squirrel creature tokens.\n" +
        "{3}{B}: Squirrels you control get +1/+0 and gain menace until end of turn."

    // When this creature enters, create two 1/1 green Squirrel creature tokens.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Squirrel"),
            count = 2
        )
    }

    // {3}{B}: Squirrels you control get +1/+0 and gain menace until end of turn.
    activatedAbility {
        cost = Costs.Mana("{3}{B}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.SQUIRREL).youControl()),
            Effects.Composite(
                Effects.ModifyStats(1, 0, EffectTarget.Self),
                Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Kev Walker"
        flavorText = "He gave the squirrels acorns. They gave him vengeance."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/995529d1-e2b0-4cce-bd40-56c7ef3c33da.jpg?1783926817"
    }
}
