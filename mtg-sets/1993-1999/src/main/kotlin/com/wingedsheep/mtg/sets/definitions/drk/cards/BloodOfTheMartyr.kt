package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RedirectNextDamageEffect
import com.wingedsheep.sdk.scripting.effects.RedirectScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blood of the Martyr
 * {W}{W}{W}
 * Instant
 * Until end of turn, if damage would be dealt to any creature, you may have that damage dealt to
 * you instead.
 *
 * A turn-long, class-wide redirection shield: `RedirectScope.CONTINUOUS` so it is never consumed by
 * use and expires only with the turn, and `creaturesOnly` so it protects *every* creature rather
 * than a fixed list — including creatures that arrive after it resolves, and never a player.
 *
 * `optional = true` is the printed "you may": before any damage that the shield could catch is
 * dealt, the caster is asked about **each instance separately** — so a sweeper that hits four
 * creatures asks four times, and the caster can soak the damage aimed at their own blocker while
 * letting the damage aimed at an opponent's creature through. The whole simultaneous batch of a
 * combat damage step (CR 510.2) is settled question by question before any of it is dealt — which is
 * where the rules put the choice anyway, since a replacement effect applies as the event *would*
 * happen rather than after it (CR 614.1). An instance that was never asked about counts as
 * declined, so the shield never redirects onto the caster behind their back.
 */
val BloodOfTheMartyr = card("Blood of the Martyr") {
    manaCost = "{W}{W}{W}"
    typeLine = "Instant"
    oracleText = "Until end of turn, if damage would be dealt to any creature, you may have that " +
        "damage dealt to you instead."

    spell {
        effect = RedirectNextDamageEffect(
            protectedTargets = emptyList(),
            redirectTo = EffectTarget.Controller,
            scope = RedirectScope.CONTINUOUS,
            creaturesOnly = true,
            optional = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Christopher Rush"
        flavorText = "The willow knows what the storm does not: that the power to endure harm outlives the power to inflict it."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22d4761d-acf2-4cb3-86a8-a3f30420a92e.jpg?1783947950"
    }
}
