package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Armament of Nyx (Journey into Nyx #4)
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has double strike as long as it's an enchantment. Otherwise, prevent all
 * damage that would be dealt by enchanted creature.
 *
 * Both halves key on the enchanted creature's *current* types, so they swap the moment it gains
 * or loses the enchantment type (the ruling): the double-strike grant is scoped to the attached
 * permanent only while it is an enchantment (layer 6 reads the layer-4 types), and the prevention
 * shield matches damage from the attached permanent only while it is not one — every kind of
 * damage, combat or not, read off projected state as the damage would be dealt.
 */
val ArmamentOfNyx = card("Armament of Nyx") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has double strike as long as it's an enchantment. Otherwise, prevent all " +
        "damage that would be dealt by enchanted creature. (A creature with double strike deals both " +
        "first-strike and regular combat damage.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(
            Keyword.DOUBLE_STRIKE,
            GroupFilter(GameObjectFilter.Enchantment, scope = Scope.AttachedTo),
        )
    }

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any
                    .withCardPredicate(CardPredicate.Not(CardPredicate.IsEnchantment))
                    .attachedToBySource(),
            ),
        ),
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4203df15-dd44-496b-889a-d7a8fe320330.jpg?1783939463"
        ruling(
            "2014-04-26",
            "Armament of Nyx continuously checks to see whether the creature it's enchanting is an " +
                "enchantment or not. If the creature isn't an enchantment and becomes one, or it is an " +
                "enchantment and stops being one, the effect will change.",
        )
    }
}
