package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fortified Beachhead
 * Land
 *
 * As this land enters, you may reveal a Soldier card from your hand. This land enters tapped
 * unless you revealed a Soldier card this way or you control a Soldier.
 * {T}: Add {W} or {U}.
 * {5}, {T}: Soldiers you control get +1/+1 until end of turn.
 *
 * The Lorwyn reveal-land shape ([Effects.MayRevealCardFromHand] inside [OnEnterRun], the
 * `otherwise` rider tapping the land) gated by "you control a Soldier": controlling one already
 * keeps the land untapped, so the optional reveal can change nothing and is skipped. The land
 * itself is not a Soldier, so it never satisfies its own condition. The reveal filter reads the
 * creature type on any card (Kindred cards count), as the Lorwyn lands do.
 */
val FortifiedBeachhead = card("Fortified Beachhead") {
    typeLine = "Land"
    colorIdentity = "WU"
    oracleText = "As this land enters, you may reveal a Soldier card from your hand. This land enters " +
        "tapped unless you revealed a Soldier card this way or you control a Soldier.\n" +
        "{T}: Add {W} or {U}.\n" +
        "{5}, {T}: Soldiers you control get +1/+1 until end of turn."

    replacementEffect(
        OnEnterRun(
            Effects.If(
                condition = Conditions.Not(
                    Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.SOLDIER))
                ),
                then = Effects.MayRevealCardFromHand(
                    filter = GameObjectFilter.Any.withSubtype(Subtype.SOLDIER),
                    otherwise = Effects.Tap(EffectTarget.Self),
                ),
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.SOLDIER).youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity),
        )
        description = "Soldiers you control get +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "262"
        artist = "Christian Dimitrov"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e248204c-865d-42d9-b745-8ff73225b4a1.jpg?1783920008"
    }
}
