package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbilityReach
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Zirda, the Dawnwaker — Ikoria: Lair of Behemoths #233
 * {1}{R/W}{R/W} · Legendary Creature — Elemental Fox · 3/3
 *
 * Companion — Each permanent card in your starting deck has an activated ability.
 * Abilities you activate that aren't mana abilities cost {2} less to activate. This effect can't
 * reduce the mana in that cost to less than one mana.
 * {1}, {T}: Target creature can't block this turn. Activate only as a sorcery.
 *
 * Companion (CR 702.139): every permanent card in the starting deck has an activated ability —
 * a basic land's intrinsic mana ability and cycling count (rulings). The deck validator checks it;
 * in the starting deck the ability does nothing (ruling).
 *
 * The reduction is [ActivatedAbilityReach.ACTIVATED_BY_YOU]: every ability its controller
 * activates, from any zone (the ruling names cycling), generic mana only and never below one mana.
 */
val ZirdaTheDawnwaker = card("Zirda, the Dawnwaker") {
    manaCost = "{1}{R/W}{R/W}"
    typeLine = "Legendary Creature — Elemental Fox"
    power = 3
    toughness = 3
    oracleText = "Companion — Each permanent card in your starting deck has an activated ability. " +
        "(If this card is your chosen companion, you may put it into your hand from outside the game " +
        "for {3} as a sorcery.)\n" +
        "Abilities you activate that aren't mana abilities cost {2} less to activate. This effect " +
        "can't reduce the mana in that cost to less than one mana.\n" +
        "{1}, {T}: Target creature can't block this turn. Activate only as a sorcery."

    keywordAbility(
        KeywordAbility.companion(
            CompanionCondition.EachCard(
                GameObjectFilter.Nonpermanent or GameObjectFilter.Any.withCardPredicate(CardPredicate.HasActivatedAbility)
            )
        )
    )

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Any),
            amount = DynamicAmounts.fixed(2),
            manaFloor = 1,
            excludeManaAbilities = true,
            reach = ActivatedAbilityReach.ACTIVATED_BY_YOU
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBlock(creature)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bd8e61c-2ee8-4243-a848-7008810db8a0.jpg?1783931007"
        ruling("2020-04-17", "An activated mana ability is one that produces mana as it resolves, not one that costs mana to activate.")
        ruling("2020-04-17", "Effects that reduce the generic mana cost of an activation cost can't reduce that cost's colored mana requirements. The activation cost is reduced by only {1} if doing so reduces that cost to one mana. For example, a cycling cost of {2} would become {1}, and one of {1}{R} would become {R}. The activation cost is unaffected if it already costs one or zero mana (such as the activation cost of Zirda's last ability).")
        ruling("2020-04-17", "Activating Zirda's last ability after a creature has blocked won't remove the blocking creature from combat or cause the creature it blocked to become unblocked.")
        ruling("2020-04-17", "The companion ability has no effect if the card is in your starting deck and creates no restriction on putting a card with a companion ability into your starting deck. For example, Zirda may be in your starting deck even if your other permanent cards don't all have activated abilities.")
    }
}
