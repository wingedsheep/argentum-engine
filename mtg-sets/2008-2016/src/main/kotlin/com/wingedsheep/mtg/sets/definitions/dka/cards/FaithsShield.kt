package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Faith's Shield
 * {W}
 * Instant
 * Target permanent you control gains protection from the color of your choice until end of turn.
 * Fateful hour — If you have 5 or less life, instead you and each permanent you control gain
 * protection from the color of your choice until end of turn.
 *
 * One colour choice covers every grant: the fateful-hour branch wraps the player grant and the
 * per-permanent grants in a single `ChooseColorThen`. Fateful hour is checked on resolution.
 */
val FaithsShield = card("Faith's Shield") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target permanent you control gains protection from the color of your choice until end of turn.\n" +
        "Fateful hour — If you have 5 or less life, instead you and each permanent you control gain protection " +
        "from the color of your choice until end of turn."

    spell {
        val permanent = target(TargetFilter.PermanentYouControl)
        effect = Effects.If(
            condition = Conditions.LifeAtMost(5),
            then = Effects.ChooseColorThen(
                Effects.GrantProtectionFromChosenColor(EffectTarget.Controller) then
                    Effects.ForEachInGroup(
                        GroupFilter(GameObjectFilter.Permanent.youControl()),
                        Effects.GrantProtectionFromChosenColor(EffectTarget.IterationEntity)
                    )
            ),
            otherwise = Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(permanent))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65eb5920-3b03-4300-bc77-0fba5e6abe69.jpg?1783940856"
    }
}
