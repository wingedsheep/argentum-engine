package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dragonstorm Globe — Tarkir: Dragonstorm #241
 * {3} · Artifact
 *
 * Each Dragon you control enters with an additional +1/+1 counter on it.
 * {T}: Add one mana of any color.
 *
 * The first ability is a self-replacement on entering Dragons (Rule 614): an
 * [EntersWithDynamicCounters] whose `appliesTo` matches Dragon creatures you control moving
 * to the battlefield, adding one extra +1/+1 counter (mirrors Grumgully, the Generous). The
 * second is a standard tap-for-any-color mana ability via [Effects.AddAnyColorMana].
 */
val DragonstormGlobe = card("Dragonstorm Globe") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "Each Dragon you control enters with an additional +1/+1 counter on it.\n" +
        "{T}: Add one mana of any color."

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmount.Fixed(1),
            // otherOnly = true routes this through the battlefield-scan path in
            // EntersWithCountersHelper, so the artifact grants counters to *other*
            // permanents (Dragons you control) as they enter, not to itself.
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().withSubtype(Subtype.DRAGON),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddAnyColorMana(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "241"
        artist = "Adrián Rodríguez Pérez"
        flavorText = "Jeskai scholars fashioned devices to forecast the behavior of the dragonstorms."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f50aa6e-ce6a-4479-9725-202926245f2c.jpg?1743204954"
    }
}
