package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.GrantAdditionalTypesToGroup
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ygra, Eater of All
 * {3}{B}{G}
 * Legendary Creature — Elemental Cat
 * 6/6
 *
 * Ward—Sacrifice a Food.
 * Other creatures are Food artifacts in addition to their other types and
 * have "{2}, {T}, Sacrifice this permanent: You gain 3 life."
 * Whenever a Food is put into a graveyard from the battlefield, put two
 * +1/+1 counters on Ygra.
 */
val YgraEaterOfAll = card("Ygra, Eater of All") {
    manaCost = "{3}{B}{G}"
    typeLine = "Legendary Creature — Elemental Cat"
    power = 6
    toughness = 6
    oracleText = "Ward—Sacrifice a Food.\nOther creatures are Food artifacts in addition to their other types and have \"{2}, {T}, Sacrifice this permanent: You gain 3 life.\"\nWhenever a Food is put into a graveyard from the battlefield, put two +1/+1 counters on Ygra."

    // Ward — Sacrifice a Food
    keywordAbility(KeywordAbility.WardSacrifice(GameObjectFilter.Any.withSubtype("Food")))

    // Other creatures are Food artifacts in addition to their other types
    staticAbility {
        ability = GrantAdditionalTypesToGroup(
            filter = Filters.Group.otherCreatures,
            addCardTypes = listOf("ARTIFACT"),
            addSubtypes = listOf("Food")
        )
    }

    // Other creatures have "{2}, {T}, Sacrifice this permanent: You gain 3 life."
    staticAbility {
        ability = GrantActivatedAbilityToCreatureGroup(
            ability = ActivatedAbility(
                cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf),
                effect = Effects.GainLife(3)
            ),
            filter = Filters.Group.otherCreatures
        )
    }

    // Whenever a Food is put into a graveyard from the battlefield,
    // put two +1/+1 counters on Ygra
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Any.withSubtype("Food"),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "241"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9ac7673-eae8-4c4b-889e-5025213a6151.jpg?1721427242"
    }
}
