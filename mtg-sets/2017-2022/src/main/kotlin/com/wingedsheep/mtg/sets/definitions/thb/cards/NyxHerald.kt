package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Nyx Herald
 * {2}{G}
 * Enchantment Creature — Centaur Shaman
 * 2/3
 * At the beginning of combat on your turn, target enchanted creature or enchantment creature you
 * control gets +1/+1 and gains trample until end of turn.
 *
 * The target is Karametra's Blessing's "enchanted creature or enchantment creature" union, scoped
 * to creatures you control (Satsuki's `(A or B).youControl()` shape — the controller predicate is
 * checked before the union's branches). Nyx Herald itself is a legal target. Once the ability has
 * resolved the bonus stays even if the creature stops qualifying (ruling).
 */
val NyxHerald = card("Nyx Herald") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Centaur Shaman"
    power = 2
    toughness = 3
    oracleText = "At the beginning of combat on your turn, target enchanted creature or enchantment " +
        "creature you control gets +1/+1 and gains trample until end of turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(
            TargetFilter(
                (
                    GameObjectFilter.Creature.enchanted() or
                        GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsEnchantment)
                    ).youControl()
            )
        )
        effect = Effects.ModifyStats(1, 1, creature) then Effects.GrantKeyword(Keyword.TRAMPLE, creature)
        description = "At the beginning of combat on your turn, target enchanted creature or enchantment " +
            "creature you control gets +1/+1 and gains trample until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Mark Zug"
        flavorText = "\"A piercing call, bright as the stars, rang out across the valley, calling the Pheres " +
            "band to war, to blood and savagery.\"\n—Luphea of Setessa, *Histories*"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/866dcef0-8d3e-4126-963a-05598d8a9e79.jpg?1783931532"
        ruling(
            "2020-01-24",
            "Once Nyx Herald's ability resolves, the creature keeps +1/+1 and trample even if it stops being " +
                "enchanted later in the turn (or, in some unusual cases, stops being an enchantment)."
        )
    }
}
