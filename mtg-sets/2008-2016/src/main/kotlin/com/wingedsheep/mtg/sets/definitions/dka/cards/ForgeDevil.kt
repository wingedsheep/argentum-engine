package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Forge Devil — Dark Ascension #91 (canonical printing)
 * {R} · Creature — Devil · 1/1
 *
 * When this creature enters, it deals 1 damage to target creature and 1 damage to you.
 *
 * The trigger is mandatory: with no other creature around, the Devil must target itself. The
 * ability targets only the creature, so if that target is illegal on resolution the ability
 * doesn't resolve and you take no damage.
 */
val ForgeDevil = card("Forge Devil") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, it deals 1 damage to target creature and 1 damage to you."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(1, creature) then Effects.DealDamage(1, EffectTarget.Controller)
        description = "When this creature enters, it deals 1 damage to target creature and 1 damage to you."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Austin Hsu"
        flavorText = "Devils infiltrated the lower levels of Thraben Cathedral, making cracks in load-bearing " +
            "pillars and setting fire to precious archives."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63b565a5-d706-47b4-bfa2-deebcc0e2e60.jpg?1783940817"
        ruling(
            "2014-07-18",
            "Forge Devil's ability is mandatory. If it's the only creature on the battlefield, you'll have to " +
                "choose it as the target and have it deal 1 damage to itself and 1 damage to you."
        )
    }
}
