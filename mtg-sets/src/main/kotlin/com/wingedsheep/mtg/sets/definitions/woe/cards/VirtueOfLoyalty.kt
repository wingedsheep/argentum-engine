package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Virtue of Loyalty // Ardenvale Fealty
 *
 * The end-step trigger iterates the same creature group twice. The first pass puts counters on
 * every creature the controller currently controls; the second untaps those creatures. Ardenvale
 * Fealty uses the normal Adventure face machinery, so a resolving Adventure is exiled and grants
 * permission to cast the enchantment face later.
 */
val VirtueOfLoyalty = card("Virtue of Loyalty") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, put a +1/+1 counter on each creature you " +
        "control. Untap those creatures."

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.Composite(
            Effects.ForEachInGroup(
                GroupFilter.AllCreaturesYouControl,
                AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            ),
            Effects.ForEachInGroup(
                GroupFilter.AllCreaturesYouControl,
                TapUntapEffect(EffectTarget.Self, tap = false)
            )
        )
        description = "At the beginning of your end step, put a +1/+1 counter on each creature " +
            "you control. Untap those creatures."
    }

    adventure("Ardenvale Fealty") {
        manaCost = "{1}{W}"
        typeLine = "Instant — Adventure"
        oracleText = "Create a 2/2 white Knight creature token with vigilance. (Then exile this " +
            "card. You may cast the enchantment later from exile.)"
        spell {
            effect = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Knight"),
                keywords = setOf(Keyword.VIGILANCE),
                imageUri = "https://cards.scryfall.io/normal/front/f/4/f4035134-a162-4651-86c5-ae006b6e0e20.jpg?1783914992"
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "38"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea7e7daf-7c06-4c74-8bcf-e42c1f611861.jpg?1783915127"

        ruling(
            "2023-09-01",
            "If a spell is cast as an Adventure, its controller exiles it instead of putting it " +
                "into its owner's graveyard as it resolves. For as long as it remains exiled, " +
                "that player may cast it as a permanent spell."
        )
        ruling(
            "2023-09-01",
            "You must still follow any timing restrictions and permissions for the permanent " +
                "spell you cast from exile. Normally, you'll be able to cast it only during your " +
                "main phase while the stack is empty."
        )
    }
}
