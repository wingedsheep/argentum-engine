package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Path of Mettle // Metzali, Tower of Triumph (Rivals of Ixalan — the card's earliest printing)
 * {R}{W}
 * Legendary Enchantment // Legendary Land
 *
 * Front — Path of Mettle (Legendary Enchantment, {R}{W})
 *   When Path of Mettle enters, it deals 1 damage to each creature that doesn't have first strike,
 *   double strike, vigilance, or haste.
 *   Whenever you attack with at least two creatures that have first strike, double strike,
 *   vigilance, and/or haste, transform Path of Mettle.
 *
 * Back — Metzali, Tower of Triumph (Legendary Land)
 *   {T}: Add one mana of any color.
 *   {1}{R}, {T}: Metzali deals 2 damage to each opponent.
 *   {2}{W}, {T}: Choose a creature at random that attacked this turn. Destroy that creature.
 *
 * Implementation:
 *  - The ETB sweep is one 1-damage hit per creature lacking all four keywords (four chained
 *    `withoutKeyword`s, read off projected state), not one hit per missing keyword.
 *  - The transform is [Triggers.you] `attacks(with, minAttackers = 2)`: the attacker filter is an
 *    `Or` over the four keywords, so two attackers each with *any one* of them suffice (they needn't
 *    share one). Only declared attackers count, so a creature put onto the battlefield attacking
 *    never contributes.
 *  - The random destroy gathers every creature on the battlefield that was declared as an attacker
 *    this turn (by any player; removal from combat doesn't erase that) and picks one at random on
 *    resolution, then destroys it — no target, so hexproof doesn't stop it and indestructible
 *    survives it, as the rulings say.
 */

private val haveAnyMettleKeyword = CardPredicate.Or(
    listOf(
        CardPredicate.HasKeyword(Keyword.FIRST_STRIKE),
        CardPredicate.HasKeyword(Keyword.DOUBLE_STRIKE),
        CardPredicate.HasKeyword(Keyword.VIGILANCE),
        CardPredicate.HasKeyword(Keyword.HASTE),
    )
)

private val PathOfMettleFront = card("Path of Mettle") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Enchantment"
    oracleText = "When Path of Mettle enters, it deals 1 damage to each creature that doesn't have " +
        "first strike, double strike, vigilance, or haste.\n" +
        "Whenever you attack with at least two creatures that have first strike, double strike, " +
        "vigilance, and/or haste, transform Path of Mettle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Group.dealDamageToAll(
            1,
            GroupFilter.AllCreatures
                .withoutKeyword(Keyword.FIRST_STRIKE)
                .withoutKeyword(Keyword.DOUBLE_STRIKE)
                .withoutKeyword(Keyword.VIGILANCE)
                .withoutKeyword(Keyword.HASTE),
        )
        description = "When Path of Mettle enters, it deals 1 damage to each creature that doesn't " +
            "have first strike, double strike, vigilance, or haste."
    }

    triggeredAbility {
        trigger = Triggers.you.attacks(
            with = GameObjectFilter.Creature.withCardPredicate(haveAnyMettleKeyword),
            minAttackers = 2,
        )
        effect = Effects.Transform(EffectTarget.Self)
        description = "Whenever you attack with at least two creatures that have first strike, " +
            "double strike, vigilance, and/or haste, transform Path of Mettle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66d9d524-3611-48d9-86c9-48e509e8ae70.jpg?1783935278"
        ruling(
            "2018-01-19",
            "Path of Mettle's first ability deals 1 damage to each creature that doesn't have any of " +
                "the four listed abilities. It doesn't deal 1 damage to each creature for each ability " +
                "that creature doesn't have."
        )
        ruling(
            "2018-01-19",
            "For Path of Mettle's last ability to trigger, any two attacking creatures you control need " +
                "to each have any one of the four listed abilities. They don't need to share one of " +
                "those abilities."
        )
    }
}

private val MetzaliTowerOfTriumph = card("Metzali, Tower of Triumph") {
    manaCost = ""
    colorIdentity = "RW"
    typeLine = "Legendary Land"
    oracleText = "(Transforms from Path of Mettle.)\n" +
        "{T}: Add one mana of any color.\n" +
        "{1}{R}, {T}: Metzali deals 2 damage to each opponent.\n" +
        "{2}{W}, {T}: Choose a creature at random that attacked this turn. Destroy that creature."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Tap)
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "{1}{R}, {T}: Metzali deals 2 damage to each opponent."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap)
        effect = Effects.Pipeline {
            val attackers = gather(
                CardSource.BattlefieldMatching(filter = GameObjectFilter.Creature.attackedThisTurn())
            )
            val victim = chooseRandom(1, from = attackers)
            destroy(victim)
        }
        description = "{2}{W}, {T}: Choose a creature at random that attacked this turn. Destroy " +
            "that creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/back/6/6/66d9d524-3611-48d9-86c9-48e509e8ae70.jpg?1783935278"
        ruling(
            "2018-01-19",
            "A creature that attacked and was removed from combat still attacked, so it can be chosen " +
                "at random. A creature that was put onto the battlefield attacking didn't attack, so it " +
                "can't be chosen at random."
        )
        ruling(
            "2018-01-19",
            "A creature with indestructible can be chosen at random. It won't be destroyed. A creature " +
                "with hexproof can be chosen at random. It will be destroyed."
        )
    }
}

val PathOfMettle: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = PathOfMettleFront,
    backFace = MetzaliTowerOfTriumph,
)
