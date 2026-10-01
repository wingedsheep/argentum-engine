package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Abzan Ascendancy
 * {W}{B}{G}
 * Enchantment
 * When this enchantment enters, put a +1/+1 counter on each creature you control.
 * Whenever a nontoken creature you control dies, create a 1/1 white Spirit creature token with flying.
 */
val AbzanAscendancy = card("Abzan Ascendancy") {
    manaCost = "{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, put a +1/+1 counter on each creature you control.\nWhenever a nontoken creature you control dies, create a 1/1 white Spirit creature token with flying."

    spell {}

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.AddCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                count = 1,
                target = EffectTarget.IterationEntity
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().copy(
                    cardPredicates = GameObjectFilter.Creature.cardPredicates + CardPredicate.IsNontoken
                )).dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/7/0/7071930c-689a-44b9-b52d-45027fd14446.jpg?1562639854"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Mark Winters"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9af53a53-d30a-4289-a043-953cd81ee241.jpg?1562790940"
    }
}
