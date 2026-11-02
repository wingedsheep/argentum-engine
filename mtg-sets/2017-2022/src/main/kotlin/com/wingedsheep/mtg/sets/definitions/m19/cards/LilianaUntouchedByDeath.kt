package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Liliana, Untouched by Death
 * {2}{B}{B}
 * Legendary Planeswalker — Liliana
 * Loyalty: 4
 *
 * +1: Mill three cards. If at least one Zombie card is milled this way, each opponent loses 2 life
 * and you gain 2 life.
 * −2: Target creature gets -X/-X until end of turn, where X is the number of Zombies you control.
 * −3: You may cast Zombie spells from your graveyard this turn.
 *
 * The +1 mills into the "milled" collection and gates on it holding a Zombie card (Renegade Reaper
 * shape). The gain is a flat 2, not "life lost this way" — in Two-Headed Giant the opposing team
 * loses 4 and you still gain 2 (2018-07-13 ruling), so it is LoseLife + GainLife, not DrainLife.
 *
 * The −2 reads the Zombie count once, as it resolves. "Zombies you control" is the bare tribal noun,
 * so it counts every Zombie permanent, not only creatures.
 *
 * The −3 is a standing (not one-shot) permission anchored to the controller, so it outlives Liliana
 * and covers Zombie cards that reach the graveyard later in the turn. Casting through it pays the
 * card's costs and follows its normal timing.
 */
val LilianaUntouchedByDeath = card("Liliana, Untouched by Death") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Liliana"
    startingLoyalty = 4
    oracleText = "+1: Mill three cards. If at least one Zombie card is milled this way, each opponent " +
        "loses 2 life and you gain 2 life.\n" +
        "−2: Target creature gets -X/-X until end of turn, where X is the number of Zombies you control.\n" +
        "−3: You may cast Zombie spells from your graveyard this turn."

    loyaltyAbility(+1) {
        effect = Patterns.Library.mill(3) then
            Effects.If(
                condition = Conditions.CollectionContainsMatch(
                    Patterns.Library.milled,
                    GameObjectFilter.Any.withSubtype(Subtype.ZOMBIE)
                ),
                then = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
                    Effects.GainLife(2)
            )
    }

    loyaltyAbility(-2) {
        val creature = target(TargetFilter.Creature)
        val x = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE)
        ).count()
        effect = Effects.ModifyStats(power = -x, toughness = -x, target = creature)
    }

    loyaltyAbility(-3) {
        effect = Effects.GrantStaticAbility(
            MayCastFromGraveyard(filter = GameObjectFilter.Any.withSubtype(Subtype.ZOMBIE)),
            EffectTarget.Controller,
            Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "106"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7939170-f84e-44c6-b8bc-a180cf7f85d9.jpg?1783934566"
        ruling(
            "2018-07-13",
            "Once Liliana's third ability has resolved, any Zombie cards in your graveyard may be cast " +
                "this turn, including Zombie cards that enter your graveyard later in the turn."
        )
        ruling(
            "2018-07-13",
            "In a Two-Headed Giant game, Liliana's first ability causes the opposing team to lose 4 life " +
                "and you gain 2 life."
        )
        ruling(
            "2018-07-13",
            "The value of X is determined only as Liliana's second ability resolves. If the number of " +
                "Zombies you control changes later, the target creature's power and toughness won't change."
        )
        ruling(
            "2018-07-13",
            "Casting a Zombie card from your graveyard once Liliana's third ability has resolved follows " +
                "the normal rules for casting that card. You must pay its costs, and you must follow all " +
                "applicable timing rules."
        )
    }
}
