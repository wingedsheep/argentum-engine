package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Guardian of Ghirapur
 * {2}{W}
 * Creature — Angel
 * 3/3
 *
 * Flying
 * When this creature enters, exile up to one other target creature or artifact you control.
 * Return it to the battlefield under its owner's control at the beginning of the next end step.
 *
 * Abuelo's blink shape on an enters trigger: exile, then a delayed end-step `Move` of the same
 * card. "Up to one" is an optional target; "other" excludes the Guardian itself.
 */
val GuardianOfGhirapur = card("Guardian of Ghirapur") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "Flying\nWhen this creature enters, exile up to one other target creature or artifact " +
        "you control. Return it to the battlefield under its owner's control at the beginning of the next end step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(
            TargetFilter(GameObjectFilter.CreatureOrArtifact.youControl()).other(),
            optional = true
        )
        effect = Effects.Exile(permanent) then
            Effects.CreateDelayedTrigger(step = Step.END, effect = Effects.Move(permanent, Zone.BATTLEFIELD))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Cynthia Sheppard"
        flavorText = "She was hope incarnate, an incandescent bulwark against the tide of machines."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9503a1e6-f0bf-44d0-a28b-56fbfcff1ff2.jpg?1783917062"
    }
}
