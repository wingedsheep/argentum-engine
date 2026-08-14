package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithExileCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mimeoplasm, Revered One — Aetherdrift #214.
 *
 * The linked graveyard selection is an as-enters replacement, so the chosen cards are exiled and
 * the counters are present before Mimeoplasm reaches the battlefield. Its activated copy effect
 * reads only that linked exile pile and applies all three printed copy exceptions: 0/0 and this
 * ability.
 */
val MimeoplasmReveredOne = card("Mimeoplasm, Revered One") {
    manaCost = "{X}{B}{G}{U}"
    colorIdentity = "UBG"
    typeLine = "Legendary Creature — Ooze"
    oracleText = "As Mimeoplasm enters, exile up to X creature cards from your graveyard. It enters " +
        "with three +1/+1 counters on it for each creature card exiled this way.\n" +
        "{2}: Mimeoplasm becomes a copy of target creature card exiled with it, except it's 0/0 and has this ability."
    power = 0
    toughness = 0

    replacementEffect(
        EntersWithExileCounters(
            filter = GameObjectFilter.Creature,
            sourceZone = Zone.GRAVEYARD,
            maxCards = DynamicAmount.XValue,
            counterType = CounterTypeFilter.PlusOnePlusOne,
            countersPerCard = 3,
        )
    )

    activatedAbility {
        cost = Costs.Mana("{2}")
        val creatureCard = target(
            "target creature card exiled with Mimeoplasm",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Creature.exiledWithSource(),
                    zone = Zone.EXILE,
                )
            )
        )
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = creatureCard,
            affected = EffectTarget.Self,
            sourceFromAnyZone = true,
            retainActivatingAbility = true,
            exceptions = CopyExceptions(powerOverride = 0, toughnessOverride = 0),
        )
        description = "{2}: Mimeoplasm becomes a copy of target creature card exiled with it, " +
            "except it's 0/0 and has this ability."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "214"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34e4c342-dc22-4e9c-81fc-a691ae9e21c1.jpg?1783907855"
    }
}
