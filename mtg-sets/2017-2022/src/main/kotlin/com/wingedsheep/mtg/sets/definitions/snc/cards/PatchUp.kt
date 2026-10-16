package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Patch Up {2}{W}
 * Sorcery
 *
 * Return up to three target creature cards with total mana value 3 or less
 * from your graveyard to the battlefield.
 *
 * The creature cards are real targets chosen as the spell is cast (CR 601.2c), with the summed
 * mana value capped by [TargetObject.totalManaValueAtMost]. Any target that has left the graveyard
 * by resolution is dropped (CR 608.2b); the rest return together.
 */
val PatchUp = card("Patch Up") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return up to three target creature cards with total mana value 3 or less from your graveyard to the battlefield."

    spell {
        targets(
            TargetFilter.CreatureInYourGraveyard,
            count = 3,
            optional = true,
            totalManaValueAtMost = DynamicAmounts.fixed(3),
        )
        effect = Effects.Pipeline {
            val patched = gather(CardSource.ChosenTargets)
            move(patched, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Scott Murphy"
        flavorText = "\"Don't tell me who sent you or who you work for. The less I know, the better.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2ce30c92-c5f2-45d2-819e-177390bc26f5.jpg?1783923158"
    }
}
