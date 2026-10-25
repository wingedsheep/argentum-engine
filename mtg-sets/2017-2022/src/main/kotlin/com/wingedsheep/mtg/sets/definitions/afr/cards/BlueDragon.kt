package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Blue Dragon
 * {5}{U}{U}
 * Creature — Dragon
 * 5/5
 * Flying
 * Lightning Breath — When this creature enters, until your next turn, target creature an opponent
 * controls gets -3/-0, up to one other target creature gets -2/-0, and up to one other target
 * creature gets -1/-0.
 *
 * The Arm the Cathars three-slot idiom: a mandatory first target, then two optional [TargetOther]
 * slots that must differ from the earlier targets. "Other" here means other than the earlier
 * targets, not other than the Dragon — the second and third slots may be any creature, Blue Dragon
 * itself included — so `excludeSource = false`.
 */
val BlueDragon = card("Blue Dragon") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\nLightning Breath — When this creature enters, until your next turn, target creature " +
        "an opponent controls gets -3/-0, up to one other target creature gets -2/-0, and up to one other " +
        "target creature gets -1/-0."
    power = 5
    toughness = 5
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.self.enters()
        val first = target(TargetFilter.Creature.opponentControls())
        // "Other" = other than the earlier targets (TargetOther); the Dragon itself stays legal.
        val second = target(
            TargetOther(TargetObject(filter = TargetFilter.Creature, optional = true), excludeSource = false)
        )
        val third = target(
            TargetOther(TargetObject(filter = TargetFilter.Creature, optional = true), excludeSource = false)
        )
        effect = Effects.ModifyStats(-3, 0, first, Duration.UntilYourNextTurn) then
            Effects.ModifyStats(-2, 0, second, Duration.UntilYourNextTurn) then
            Effects.ModifyStats(-1, 0, third, Duration.UntilYourNextTurn)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98dea71b-2778-4374-8ea2-7fa82f0f6110.jpg?1783926518"
        ruling("2021-07-23", "Blue Dragon's Lightning Breath ability cannot target the same creature multiple times.")
        ruling(
            "2021-07-23",
            "The last two targets are optional, but the first target is not. If an opponent has at least one " +
                "creature the ability could target, you must choose a target to get -3/-0. (Note that the last " +
                "two targets can be any creature, not just one an opponent controls.)"
        )
    }
}
