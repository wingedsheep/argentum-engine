package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Electrodominance — Ravnica Allegiance #99 (canonical / earliest real printing, 2019)
 * {X}{R}{R} · Instant
 *
 * Electrodominance deals X damage to any target. You may cast a spell with mana value X or less
 * from your hand without paying its mana cost.
 *
 * Damage to `Targets.Any`, then Yue / Baral and Kari Zev's free-cast pipeline: gather nonland hand
 * cards ("a spell" — a land can't be cast, per the ruling) with mana value at most the spell's X
 * (Villainous Wealth's `manaValueAtMostDynamic(xValue())`), choose up to one ("you may"), and cast
 * it for free during this spell's resolution, so card-type timing is ignored. An illegal target
 * fizzles the whole spell, so no cast happens then either.
 */
val Electrodominance = card("Electrodominance") {
    manaCost = "{X}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Electrodominance deals X damage to any target. You may cast a spell with mana value " +
        "X or less from your hand without paying its mana cost."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), t) then Effects.Pipeline {
            val inHand = gather(CardSource.FromZone(Zone.HAND, Player.You, filter = GameObjectFilter.Nonland))
            val candidates = filter(inHand, GameObjectFilter.Any.manaValueAtMostDynamic(DynamicAmounts.xValue()))
            val chosen = chooseUpTo(1, from = candidates, selectedLabel = "Cast without paying its mana cost")
            run(Effects.CastFromCollectionWithoutPayingCost(chosen))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Dmitry Burmak"
        flavorText = "\"Basically, we turn a bunch of little lightnings into one big lightning.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c63877b-cdab-4ce4-a1c0-c088eb62a57a.jpg?1783933682"
        ruling("2024-04-12", "If the target is illegal as Electrodominance tries to resolve, it won't resolve and none of its effects will happen. You won't get to cast a spell from your hand.")
        ruling("2024-04-12", "Effects that allow you to cast a spell don't allow you to play a land.")
        ruling("2024-04-12", "If the target is dealt lethal damage this way, it will still be on the battlefield while you cast a spell from your hand. It won't die until after Electrodominance is finished resolving. If any of its abilities trigger while you cast that spell, those abilities will trigger but won't resolve until after the target has died.")
        ruling("2024-04-12", "You cast the spell during the resolution of Electrodominance. Ignore timing restrictions based on the spell's type.")
        ruling("2024-04-12", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If the spell has any mandatory additional costs, such as that of Bankrupt in Blood, those must be paid to cast the card.")
        ruling("2024-04-12", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
