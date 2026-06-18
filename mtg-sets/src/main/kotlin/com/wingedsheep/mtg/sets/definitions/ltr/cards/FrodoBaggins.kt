package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Frodo Baggins
 * {G}{W}
 * Legendary Creature — Halfling Scout
 * 1/3
 *
 * Whenever Frodo Baggins or another legendary creature you control enters, the Ring tempts you.
 * As long as Frodo Baggins is your Ring-bearer, it must be blocked if able.
 *
 * The "must be blocked if able" half uses the new `MustBeBlocked` static ability (the static
 * counterpart of `MustBeBlockedEffect`), wrapped in `ConditionalStaticAbility` gated on
 * `SourceIsRingBearer`; `BlockPhaseManager` now honors that static alongside the floating
 * must-be-blocked modifications.
 */
val FrodoBaggins = card("Frodo Baggins") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Halfling Scout"
    power = 1
    toughness = 3
    oracleText = "Whenever Frodo Baggins or another legendary creature you control enters, the Ring " +
        "tempts you.\n" +
        "As long as Frodo Baggins is your Ring-bearer, it must be blocked if able."

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.legendary().youControl(),
            binding = TriggerBinding.ANY
        )
        effect = Effects.TheRingTemptsYou()
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = MustBeBlocked(allCreatures = false),
            condition = Conditions.SourceIsRingBearer
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Ekaterina Burmak"
        flavorText = "\"Few have ever come hither through greater peril or on an errand more urgent.\"\n—Elrond"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de1c0399-9db8-4901-b72e-0010eb9b92b0.jpg?1686969787"
    }
}
