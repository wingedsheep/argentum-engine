package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.SourceIsModified
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ian the Reckless — Fallout #59
 * {1}{R} · Legendary Creature — Human Warrior · 2/1
 *
 * Whenever Ian the Reckless attacks, if it's modified, you may have it deal damage equal to its
 * power to you and any target.
 *
 * "If it's modified" is an intervening-if (CR 603.4) on [SourceIsModified], rechecked on
 * resolution. If Ian has left the battlefield by then, both the recheck and its power read last-known
 * information, so a modified Ian killed in response still deals its last power to both.
 */
val IanTheReckless = card("Ian the Reckless") {
    manaCost = "{1}{R}"
    typeLine = "Legendary Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "Whenever Ian the Reckless attacks, if it's modified, you may have it deal damage equal to its power to you and any target. (Equipment, Auras you control, and counters are modifications.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = SourceIsModified
        val anyTarget = target(Targets.Any)
        effect = Effects.May(
            Effects.DealDamage(DynamicAmounts.sourcePower(), EffectTarget.Controller) then
                Effects.DealDamage(DynamicAmounts.sourcePower(), anyTarget)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Lorenzo Mastroianni"
        flavorText = "\"I may not be a very good fighter any more, but I'm good enough to best the likes of you!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cd46b6a-eb20-4ffa-94e0-47d1d2d46087.jpg?1783912404"
        ruling("2024-03-08", "If Ian the Reckless isn't modified when it attacks, its ability won't trigger at all. When its ability tries to resolve, if it isn't modified at that time, the ability won't resolve.")
        ruling("2024-03-08", "If Ian the Reckless leaves the battlefield before its last ability resolves, as long as it was modified when it was last on the battlefield, use its power as it last existed on the battlefield to determine how much damage that ability deals.")
    }
}
