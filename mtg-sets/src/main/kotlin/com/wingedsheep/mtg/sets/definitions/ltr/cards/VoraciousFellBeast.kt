package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Voracious Fell Beast
 * {4}{B}{B}
 * Creature — Drake Beast
 * 4/4
 *
 * Flying
 * When this creature enters, each opponent sacrifices a creature of their choice. Create a Food
 * token for each creature sacrificed this way.
 *
 * Gap 16 (count of permanents sacrificed by an effect): adds
 * `DynamicAmount.PermanentsSacrificedThisWay`, which reads the resolving effect context's
 * `sacrificedPermanents` (populated by the edict that runs first in this composite — the
 * sibling-rider wiring landed with Gap 17). Food count = that dynamic amount.
 */
val VoraciousFellBeast = card("Voracious Fell Beast") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Drake Beast"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, each opponent sacrifices a creature of their choice. Create a " +
        "Food token for each creature sacrificed this way. (It's an artifact with \"{2}, {T}, " +
        "Sacrifice this token: You gain 3 life.\")"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature,
            1,
            EffectTarget.PlayerRef(Player.EachOpponent)
        ).then(
            CreatePredefinedTokenEffect(
                tokenType = "Food",
                dynamicCount = DynamicAmounts.permanentsSacrificedThisWay()
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "John Tedrick"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9b7d7f8-503d-4660-9a18-6a8e2fcaa25f.jpg?1686968776"
    }
}
