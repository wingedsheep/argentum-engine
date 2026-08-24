package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Pristine Talisman
 * {3}
 * Artifact
 * {T}: Add {C}. You gain 1 life.
 *
 * A mana ability with a non-mana rider: one `manaAbility` on [Costs.Tap] whose effect is an
 * [Effects.Composite] of [Effects.AddColorlessMana] and [Effects.GainLife] (cf. Talisman of
 * Progress, which pairs its mana with damage instead). Because the whole ability is a mana
 * ability it never uses the stack, so the life gain happens as part of the activation.
 */
val PristineTalisman = card("Pristine Talisman") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}. You gain 1 life."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Composite(
            Effects.AddColorlessMana(1),
            Effects.GainLife(1)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Matt Cavotta"
        flavorText = "\"Tools and artisans can be destroyed, but the act of creation is inviolate.\"\n—Elspeth Tirel"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b31d96cf-7276-46c4-ad17-d6a5c85f1315.jpg"
        ruling("2011-06-01", "Pristine Talisman has a mana ability. Its ability doesn't use the stack and can't be responded to.")
    }
}
