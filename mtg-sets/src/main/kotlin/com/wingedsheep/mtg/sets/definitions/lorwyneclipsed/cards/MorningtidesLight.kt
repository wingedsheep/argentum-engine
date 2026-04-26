package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Morningtide's Light
 * {3}{W}
 * Sorcery
 *
 * Exile any number of target creatures. At the beginning of the next end step, return those
 * cards to the battlefield tapped under their owners' control.
 * Until your next turn, prevent all damage that would be dealt to you.
 * Exile Morningtide's Light.
 */
val MorningtidesLight = card("Morningtide's Light") {
    manaCost = "{3}{W}"
    typeLine = "Sorcery"
    oracleText = "Exile any number of target creatures. At the beginning of the next end step, return those cards to the battlefield tapped under their owners' control.\nUntil your next turn, prevent all damage that would be dealt to you.\nExile Morningtide's Light."

    spell {
        selfExile()

        target("any number of target creatures", TargetCreature(count = 99, optional = true))

        effect = ForEachTargetEffect(
            effects = listOf(
                MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.EXILE),
                CreateDelayedTriggerEffect(
                    step = Step.END,
                    effect = MoveToZoneEffect(
                        target = EffectTarget.ContextTarget(0),
                        destination = Zone.BATTLEFIELD,
                        placement = ZonePlacement.Tapped
                    )
                )
            )
        ).then(
            PreventDamageEffect(
                target = EffectTarget.Controller,
                duration = Duration.UntilYourNextTurn
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "27"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/181ee045-5650-479a-8c03-015b38fdcd63.jpg?1767690057"
        ruling("2025-11-17", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2025-11-17", "Auras attached to the exiled creatures will be put into their owners' graveyards. Equipment attached to the exiled creatures will become unattached and remain on the battlefield. Any counters on the exiled creatures will cease to exist.")
    }
}
