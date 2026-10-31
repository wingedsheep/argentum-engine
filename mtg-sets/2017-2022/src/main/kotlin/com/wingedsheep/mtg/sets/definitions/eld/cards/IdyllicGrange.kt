package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Idyllic Grange
 * Land — Plains
 *
 * ({T}: Add {W}.)
 * This land enters tapped unless you control three or more other Plains.
 * When this land enters untapped, put a +1/+1 counter on target creature you control.
 *
 * Same shape as [MysticSanctuary]: the Plains subtype supplies the {W} ability, and "enters
 * untapped" is the `untapped()` state predicate on the self enters trigger.
 */
val IdyllicGrange = card("Idyllic Grange") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Land — Plains"
    oracleText = "({T}: Add {W}.)\n" +
        "This land enters tapped unless you control three or more other Plains.\n" +
        "When this land enters untapped, put a +1/+1 counter on target creature you control."

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.YouControlOtherAtLeast(3, GameObjectFilter.Land.withSubtype(Subtype.PLAINS))
    ))

    triggeredAbility {
        trigger = Triggers.self.matching(GameObjectFilter.Any.untapped()).enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "246"
        artist = "Howard Lyon"
        flavorText = "Every hero's journey starts somewhere."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca2c611c-3a6f-44b0-9daa-837a465845e0.jpg?1783932578"
        ruling(
            "2019-10-04",
            "Once the common lands (such as Mystic Sanctuary) enter the battlefield tapped, there's no " +
                "way to untap them with a spell or ability to make their last ability trigger.",
        )
        ruling(
            "2019-10-04",
            "As these lands are entering the battlefield, they check for lands that are already on the " +
                "battlefield. They won't see lands that are entering the battlefield at the same time " +
                "(due to Scapeshift, for example).",
        )
        ruling(
            "2019-10-04",
            "If another effect puts these lands onto the battlefield tapped, they enter tapped, even if " +
                "you control enough lands with the appropriate basic land type.",
        )
    }
}
