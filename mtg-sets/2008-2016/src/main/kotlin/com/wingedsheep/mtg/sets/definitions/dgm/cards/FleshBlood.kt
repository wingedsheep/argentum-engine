package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flesh // Blood
 * {3}{B}{G} Sorcery // {R}{G} Sorcery
 * Flesh: Exile target creature card from a graveyard. Put X +1/+1 counters on target creature,
 * where X is the power of the card you exiled.
 * Blood: Target creature you control deals damage equal to its power to any target.
 * Fuse
 *
 * X reads the exiled card's power in exile (the same card it was in the graveyard, CR 400.7j).
 * If the graveyard card became an illegal target it is dropped at resolution, nothing is exiled,
 * and X is 0. Fused, Flesh resolves first, so Blood's damage sees the new counters.
 */
val FleshBlood = card("Flesh // Blood") {
    layout = CardLayout.SPLIT
    colorIdentity = "BRG"
    keywords(Keyword.FUSE)

    face("Flesh") {
        manaCost = "{3}{B}{G}"
        typeLine = "Sorcery"
        oracleText = "Exile target creature card from a graveyard. Put X +1/+1 counters on target creature, " +
            "where X is the power of the card you exiled.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val exiled = target(TargetFilter.CreatureInGraveyard)
            val creature = target(TargetFilter.Creature)
            effect = Effects.Move(exiled, Zone.EXILE, fromZone = Zone.GRAVEYARD) then
                Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.powerOf(exiled), creature)
        }
    }

    face("Blood") {
        manaCost = "{R}{G}"
        typeLine = "Sorcery"
        oracleText = "Target creature you control deals damage equal to its power to any target.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val myCreature = target(TargetFilter.CreatureYouControl)
            val any = target(Targets.Any)
            effect = Effects.DealDamage(
                amount = DynamicAmounts.powerOf(myCreature),
                target = any,
                damageSource = myCreature
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02b40fe4-901a-4832-8d52-a6bb5cc07b63.jpg?1783940015"
        ruling("2013-04-15", "As Flesh is resolving, if the creature is an illegal target but the creature card is still a legal target, the creature card will be exiled but no counters will be put onto the creature. If the creature is the only legal target, Flesh will still resolve but will have no effect as you didn't exile a card.")
        ruling("2013-04-15", "The number of +1/+1 counters is based on the power of the creature card as it last existed in the graveyard.")
        ruling("2013-04-15", "If you cast Flesh // Blood as a fused split card, the +1/+1 counters will have been placed before the amount of damage is determined.")
        ruling("2013-04-15", "If you're casting a split card with fuse from any zone other than your hand, you can't cast both halves. You'll only be able to cast one half or the other.")
        ruling("2013-04-15", "To cast a fused split spell, pay both of its mana costs. While the spell is on the stack, its mana value is the total amount of mana in both costs.")
        ruling("2013-04-15", "You can choose the same object as the target of each half of a fused split spell, if appropriate.")
        ruling("2013-04-15", "When resolving a fused split spell with multiple targets, treat it as you would any spell with multiple targets. If all targets are illegal when the spell tries to resolve, the spell doesn't resolve and none of its effects happen. If at least one target is still legal at that time, the spell resolves, but an illegal target can't perform any actions or have any actions performed on it.")
        ruling("2013-04-15", "When a fused split spell resolves, follow the instructions of the left half first, then the instructions on the right half.")
        ruling("2013-04-15", "On the stack, a split spell that hasn't been fused has only that half's characteristics and mana value. The other half is treated as though it didn't exist.")
        ruling("2013-04-15", "Some split cards with fuse have two monocolor halves of different colors. If such a card is cast as a fused split spell, the resulting spell is multicolored. If only one half is cast, the spell is the color of that half. While not on the stack, such a card is multicolored.")
        ruling("2013-04-15", "Some split cards with fuse have two halves that are both multicolored. That card is multicolored no matter which half is cast, or if both halves are cast. It's also multicolored while not on the stack.")
        ruling("2013-04-15", "If a player names a card, the player may name either half of a split card, but not both. A split card has the chosen name if one of its two names matches the chosen name.")
        ruling("2013-04-15", "If you cast a split card with fuse from your hand without paying its mana cost, you can choose to use its fuse ability and cast both halves without paying their mana costs.")
        ruling("2013-04-15", "As Blood is resolving, if only one of the targets is legal, Blood will still resolve but will have no effect: If the first target is illegal, it can't deal damage to anything. If the second target is illegal, it can't be dealt damage.")
        ruling("2013-04-15", "The amount of damage dealt is based on the first target creature's power as Blood resolves.")
    }
}
