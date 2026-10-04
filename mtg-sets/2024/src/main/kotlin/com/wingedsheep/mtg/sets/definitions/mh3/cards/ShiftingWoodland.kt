package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shifting Woodland — Modern Horizons 3 #228
 * Land
 *
 * This land enters tapped unless you control a Forest.
 * {T}: Add {G}.
 * Delirium — {2}{G}{G}: This land becomes a copy of target permanent card in your graveyard until
 * end of turn. Activate only if there are four or more card types among cards in your graveyard.
 *
 * The copy ability is Likeness Looter's shape with an end-of-turn duration: `affected = Self`
 * narrows the copy to this land and `sourceFromAnyZone` lets the copy source stay in the graveyard
 * (only its printed copiable values are taken, per the rulings). No `retainActivatingAbility` —
 * unlike Likeness Looter, the copy does not keep this ability. The delirium gate is an
 * `ActivationRestriction.OnlyIfCondition`, so the ability isn't offered without delirium.
 */
val ShiftingWoodland = card("Shifting Woodland") {
    typeLine = "Land"
    colorIdentity = "G"
    oracleText = "This land enters tapped unless you control a Forest.\n" +
        "{T}: Add {G}.\n" +
        "Delirium — {2}{G}{G}: This land becomes a copy of target permanent card in your graveyard " +
        "until end of turn. Activate only if there are four or more card types among cards in your graveyard."

    replacementEffect(EntersTapped(
        unlessCondition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Forest"))
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}{G}")
        val permanentCard = target(TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = permanentCard,
            affected = EffectTarget.Self,
            sourceFromAnyZone = true,
            duration = Duration.EndOfTurn,
        )
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.Delirium()))
        description = "Delirium — {2}{G}{G}: This land becomes a copy of target permanent card in your " +
            "graveyard until end of turn. Activate only if there are four or more card types among " +
            "cards in your graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/059164e1-894d-4586-9800-e60d6fbd6eb6.jpg?1783911237"
        ruling(
            "2024-06-07",
            "You must already control a Forest as Shifting Woodland enters the battlefield for it to enter " +
                "untapped. If it enters the battlefield at the same time as a Forest when you control no other " +
                "Forests, it will enter tapped."
        )
        ruling(
            "2024-06-07",
            "Shifting Woodland copies exactly what was printed on the original card and nothing else. It " +
                "doesn't copy any information about the object the card was before it was put into your graveyard."
        )
        ruling(
            "2024-06-07",
            "Any effects that applied to Shifting Woodland before it becomes a copy of another card will " +
                "continue to apply after it becomes a copy. The same is true of any counters that are on " +
                "Shifting Woodland."
        )
        ruling("2024-06-07", "If a card in your graveyard has {X} in its mana cost, X is considered to be 0.")
        ruling(
            "2024-06-07",
            "Because Shifting Woodland isn't entering the battlefield when it becomes a copy of a card, any " +
                "\"When [this creature] enters the battlefield\" or \"[This creature] enters the battlefield " +
                "with\" abilities of the copied card won't apply."
        )
    }
}
