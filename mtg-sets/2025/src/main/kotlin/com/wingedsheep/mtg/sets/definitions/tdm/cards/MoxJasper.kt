package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Mox Jasper — Tarkir: Dragonstorm #246
 * {0} · Legendary Artifact
 *
 * {T}: Add one mana of any color. Activate only if you control a Dragon.
 *
 * A classic Mox shell with a Dragon-tribal gate: the tap-for-any-color mana ability via
 * [Effects.AddAnyColorMana] is fenced behind an [ActivationRestriction.OnlyIfCondition]
 * checking "you control a Dragon" — modeled with the general-purpose
 * [Conditions.ControlPermanentOfType] existence check, so the gate re-evaluates each time the
 * ability is offered (and disappears if the last Dragon leaves). The bare tribal noun means any
 * permanent with the subtype, so a Dragon Vehicle or a Dragon artifact turns it on; the adjectival
 * "a Dragon creature" would be [Conditions.ControlCreatureOfType].
 */
val MoxJasper = card("Mox Jasper") {
    manaCost = "{0}"
    typeLine = "Legendary Artifact"
    oracleText = "{T}: Add one mana of any color. Activate only if you control a Dragon."

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddAnyColorMana(1)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.ControlPermanentOfType(Subtype.DRAGON)
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "246"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a851d2d3-7e93-4887-bee5-4d6c9aaf9419.jpg?1743204975"
    }
}
