package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Will of the Naga — Fate Reforged #58 (canonical printing)
 * {4}{U}{U} · Instant
 *
 * Delve
 * Tap up to two target creatures. Those creatures don't untap during their controller's next
 * untap step.
 *
 * Delve on a Frost Breath body: "up to two target" is one optional requirement taking zero to two
 * creatures; each is tapped and gets DOESNT_UNTAP keyed to its own (current) controller's next
 * untap step, so a creature that changes controllers skips its new controller's untap (ruling).
 */
val WillOfTheNaga = card("Will of the Naga") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)\nTap up to two target creatures. Those creatures don't untap during their controller's next untap step."

    keywords(Keyword.DELVE)

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.ContextTarget(0),
                Duration.UntilAfterAffectedControllersNextUntap()
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Wayne Reynolds"
        flavorText = "The charm of a naga is undeniable."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fed1c4d-f11e-476a-bbb2-06ffc6e261f3.jpg?1783938700"
        ruling("2014-11-24", "Will of the Naga can target creatures that are already tapped. Those creatures won't untap during their controller's next untap step.")
        ruling("2014-11-24", "Will of the Naga tracks the creatures, but not their controllers. If any of those creatures changes controllers before its first controller's next untap step, then it won't untap during its new controller's next untap step.")
        ruling("2014-11-24", "If you chose two targets and one is an illegal target as Will of the Naga resolves, that creature won't become tapped and it won't be stopped from untapping during its controller's next untap step. It won't be affected by Will of the Naga in any way.")
    }
}
