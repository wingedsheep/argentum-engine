package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kefka, Dancing Mad
 * {5}{B}{R}
 * Legendary Creature — Human Wizard
 * 6/6
 * During your turn, Kefka has indestructible.
 * At the beginning of your end step, exile a card at random from each opponent's graveyard. You may
 * cast any number of spells from among cards exiled this way without paying their mana costs. Then
 * each player who owns a spell you cast this way loses life equal to its mana value.
 *
 * Each opponent's random card is picked per graveyard, then all of them are exiled together. The
 * free-cast loop records the spells it actually cast, and each one's owner loses life equal to that
 * spell's mana value — one life loss per spell, so an opponent who owned two spells loses life
 * twice. A free cast forces X = 0, which is the spell's mana value on the stack.
 */
val KefkaDancingMad = card("Kefka, Dancing Mad") {
    manaCost = "{5}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Wizard"
    power = 6
    toughness = 6
    oracleText = "During your turn, Kefka has indestructible.\n" +
        "At the beginning of your end step, exile a card at random from each opponent's graveyard. " +
        "You may cast any number of spells from among cards exiled this way without paying their " +
        "mana costs. Then each player who owns a spell you cast this way loses life equal to its mana value."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val (picked) = forEachPlayerCollecting(Player.EachOpponent) {
                listOf(chooseRandom(1, from = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))))
            }
            exile(picked)
            val castable = filter(picked, GameObjectFilter.Nonland)
            val cast = runStoringCollection { Effects.CastAnyNumberFromCollectionWithoutPayingCost(castable, storeCastTo = it) }
            run(
                Effects.ForEachInCollection(
                    cast,
                    Effects.LoseLife(
                        DynamicAmounts.manaValueOf(EffectTarget.IterationEntity),
                        EffectTarget.PlayerRef(Player.OwnerOfIterationEntity),
                    ),
                )
            )
        }
        description = "At the beginning of your end step, exile a card at random from each opponent's " +
            "graveyard. You may cast any number of spells from among cards exiled this way without " +
            "paying their mana costs. Then each player who owns a spell you cast this way loses life " +
            "equal to its mana value."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Anton Solovianchyk"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21cfc161-11b8-4253-a872-3b08a13b6991.jpg?1783906346"
        ruling("2025-06-06", "If a spell you cast has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2025-06-06", "Cards you choose not to cast this way (or can't cast this way) remain in exile.")
        ruling("2025-06-06", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If the card has any mandatory additional costs, those must be paid to cast the spell.")
        ruling("2025-06-06", "You cast the spells one at a time, choosing modes, targets, and so on. The last spell you cast will be the first one to resolve.")
        ruling("2025-06-06", "You choose which spells to cast (if any) as Kefka's last ability resolves. You can't wait to cast them later in the turn. Timing restrictions based on their types are ignored.")
    }
}
