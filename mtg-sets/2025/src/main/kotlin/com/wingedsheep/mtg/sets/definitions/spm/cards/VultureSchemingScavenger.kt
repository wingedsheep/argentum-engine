package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vulture, Scheming Scavenger
 * {5}{U/B}
 * Legendary Creature — Human Artificer Villain
 * 4/6
 * Flying
 * Whenever Vulture attacks, other Villains you control gain flying until end of turn.
 */
val VultureSchemingScavenger = card("Vulture, Scheming Scavenger") {
    manaCost = "{5}{U/B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Artificer Villain"
    oracleText = "Flying\nWhenever Vulture attacks, other Villains you control gain flying until end of turn."
    power = 4
    toughness = 6

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Villain").youControl(), excludeSelf = true),
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Kevin Sidharta"
        flavorText = "\"For all that web-slinging, Spider-Man, you seldom look up.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e29281be-e722-4149-93a0-6dd3f0f64253.jpg?1757377958"
    }
}
