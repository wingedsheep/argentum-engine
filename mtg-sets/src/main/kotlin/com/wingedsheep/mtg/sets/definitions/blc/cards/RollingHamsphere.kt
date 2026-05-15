package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Rolling Hamsphere
 * {7}
 * Artifact — Vehicle
 * 4/4
 *
 * This Vehicle gets +1/+1 for each Hamster you control.
 * Whenever this Vehicle attacks, create three 1/1 red Hamster creature tokens,
 * then it deals X damage to any target, where X is the number of Hamsters you control.
 * Crew 3
 */
val RollingHamsphere = card("Rolling Hamsphere") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 4
    oracleText = "This Vehicle gets +1/+1 for each Hamster you control.\n" +
        "Whenever this Vehicle attacks, create three 1/1 red Hamster creature tokens, " +
        "then it deals X damage to any target, where X is the number of Hamsters you control.\n" +
        "Crew 3"

    val hamsterCount = DynamicAmount.AggregateBattlefield(
        player = Player.You,
        filter = GameObjectFilter.Creature.withSubtype("Hamster")
    )

    // This Vehicle gets +1/+1 for each Hamster you control.
    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = hamsterCount,
            toughnessBonus = hamsterCount
        )
    }

    // Whenever this Vehicle attacks, create three 1/1 red Hamster tokens, then it deals X
    // damage to any target, where X is the number of Hamsters you control (counted after
    // the new tokens enter, per the Scryfall ruling — X is determined as the ability resolves).
    triggeredAbility {
        trigger = Triggers.Attacks
        val anyTarget = target("any target", Targets.Any)
        effect = Effects.Composite(
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Hamster"),
                count = 3,
                imageUri = "https://cards.scryfall.io/normal/front/7/1/711274ae-1a4e-491c-aa71-b2d29c890578.jpg?1721427571"
            ),
            Effects.DealDamage(
                amount = hamsterCount,
                target = anyTarget,
                damageSource = EffectTarget.Self
            )
        )
    }

    keywordAbility(KeywordAbility.Numeric(Keyword.CREW, 3))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Henry Peters"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04d6e7f2-fb69-477c-b4f1-8a97485dbf32.jpg?1721558940"
        ruling(
            "2024-07-26",
            "The value of X is determined only once, as Rolling Hamsphere's second ability resolves."
        )
    }
}
