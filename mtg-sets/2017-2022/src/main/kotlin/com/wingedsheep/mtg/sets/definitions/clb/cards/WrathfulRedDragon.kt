package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wrathful Red Dragon
 * {3}{R}{R}
 * Creature — Dragon
 * 5/5
 * Flying
 * Whenever a Dragon you control is dealt damage, it deals that much damage to any target that
 * isn't a Dragon.
 *
 * The damaged Dragon is the source of the damage ("it deals"), so it is the triggering entity
 * named as `damageSource`. A Dragon killed by the damage that triggered this still deals it, by
 * last-known information (CR 608.2h).
 */
val WrathfulRedDragon = card("Wrathful Red Dragon") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever a Dragon you control is dealt damage, it deals that much damage to any target " +
        "that isn't a Dragon."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.withSubtype("Dragon").youControl()).isDealtDamage()
        val t = target(
            AnyTarget(
                descriptionOverride = "any target that isn't a Dragon",
                // Not(HasSubtype), not NotSubtype: a player has no type line, so only the negated
                // form lets "isn't a Dragon" hold for players too.
                filter = GameObjectFilter.Any.withCardPredicate(
                    CardPredicate.Not(CardPredicate.HasSubtype(Subtype.DRAGON))
                ),
            )
        )
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggerDamageAmount(),
            target = t,
            damageSource = EffectTarget.TriggeringEntity
        )
        description = "Whenever a Dragon you control is dealt damage, it deals that much damage " +
            "to any target that isn't a Dragon."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Dan Murayama Scott"
        flavorText = "\"We'll put it all back! The gems, the gold, the chalice that would've looked " +
            "great on my mantle; we'll put everythi—\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acd207de-eec3-4d82-b8b9-b1e60a7bad12.jpg?1783922724"
    }
}
