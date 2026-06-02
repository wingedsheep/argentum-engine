package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Ioreth of the Healing House
 * {2}{U}
 * Legendary Creature — Human Cleric
 * 1/4
 * {T}: Untap another target permanent.
 * {T}: Untap two other target legendary creatures.
 */
val IorethOfTheHealingHouse = card("Ioreth of the Healing House") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Cleric"
    power = 1
    toughness = 4
    oracleText = "{T}: Untap another target permanent.\n{T}: Untap two other target legendary creatures."

    // {T}: Untap another target permanent.
    activatedAbility {
        cost = Costs.Tap
        val permanent = target(
            "another target permanent",
            TargetPermanent(filter = TargetFilter.Permanent.other())
        )
        effect = Effects.Untap(permanent)
    }

    // {T}: Untap two other target legendary creatures.
    activatedAbility {
        cost = Costs.Tap
        target(
            "two other target legendary creatures",
            TargetCreature(filter = TargetFilter.Creature.legendary().other(), count = 2)
        )
        effect = Effects.Composite(
            listOf(
                Effects.Untap(EffectTarget.ContextTarget(0)),
                Effects.Untap(EffectTarget.ContextTarget(1))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Wei Guan"
        flavorText = "\"I have been too busy with this and that to heed all the crying and shouting. All I hope is that those murdering devils do not come to this House and trouble the sick.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03ab74cd-978a-49eb-9d38-bc8b472b3cef.jpg?1686968152"
    }
}
