package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Little Bear
 * {2}{G}
 * Creature — Bear
 * 3/2
 * Flash
 * When this creature enters, untap another target creature you control. If that creature is a
 * Bear, put a +1/+1 counter on it.
 *
 * The Bear check reads the target at resolution (after the untap), so a creature that only became
 * a Bear on the way in still gets the counter — and one that stopped being a Bear doesn't.
 */
val LittleBear = card("Little Bear") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bear"
    oracleText = "Flash\nWhen this creature enters, untap another target creature you control. " +
        "If that creature is a Bear, put a +1/+1 counter on it."
    power = 3
    toughness = 2
    keywords(Keyword.FLASH)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetCreature(filter = TargetFilter.OtherCreatureYouControl))
        effect = Effects.Composite(
            Effects.Untap(t),
            ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(
                    GameObjectFilter.Creature.withSubtype(Subtype.BEAR),
                ),
                effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t),
            ),
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Tomas Duchek"
        flavorText = "Even the littlest bears joined in important bear meetings and moonlit dances."
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a50858a-33b5-4c45-9c31-5956ae5a33a6.jpg?1785323276"
    }
}
