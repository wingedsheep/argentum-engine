package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rooftop Nuisance
 * {2}{U}
 * Sorcery
 * Casualty 1 (As you cast this spell, you may sacrifice a creature with power 1 or greater. When you do, copy this spell and you may choose a new target for the copy.)
 * Tap target creature. It doesn't untap during its controller's next untap step.
 * Draw a card.
 *
 * Casualty 1 is the printed [KeywordAbility.Casualty]; the copy also draws a card. The untap lock
 * is DOESNT_UNTAP through the affected creature's controller's next untap step (as Celestial Regulator).
 */
val RooftopNuisance = card("Rooftop Nuisance") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Casualty 1 (As you cast this spell, you may sacrifice a creature with power 1 or greater. When you do, copy this spell and you may choose a new target for the copy.)\n" +
        "Tap target creature. It doesn't untap during its controller's next untap step.\n" +
        "Draw a card."

    keywordAbility(KeywordAbility.casualty(1))

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap(),
            ) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7a84781-b40c-41e0-a0b9-86e466c9b12d.jpg?1783923139"
    }
}
