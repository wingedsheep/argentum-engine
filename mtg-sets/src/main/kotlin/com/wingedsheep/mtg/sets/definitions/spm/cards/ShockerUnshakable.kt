package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Shocker, Unshakable
 * {4}{R}{R}
 * Legendary Creature — Human Rogue Villain, 5/5
 *
 * During your turn, Shocker has first strike.
 * Vibro-Shock Gauntlets — When Shocker enters, he deals 2 damage to target creature and
 * 2 damage to that creature's controller.
 *
 * The conditional first strike is a time-restricted static keyword grant to self
 * ([ConditionalStaticAbility] over [GrantKeyword] on [Filters.Self], gated by
 * [Conditions.IsYourTurn]). The ETB deals 2 to the target creature and 2 to that creature's
 * controller ([EffectTarget.TargetController], resolved against the single declared target).
 */
val ShockerUnshakable = card("Shocker, Unshakable") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Rogue Villain"
    oracleText = "During your turn, Shocker has first strike.\n" +
        "Vibro-Shock Gauntlets — When Shocker enters, he deals 2 damage to target creature and " +
        "2 damage to that creature's controller."
    power = 5
    toughness = 5

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.DealDamage(2, creature)
            .then(Effects.DealDamage(2, EffectTarget.TargetController))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Kevin Glint"
        flavorText = "Mock him at your peril."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b2c0d9a-364a-4823-aa4c-fe473d4463f0.jpg?1783905333"
    }
}
