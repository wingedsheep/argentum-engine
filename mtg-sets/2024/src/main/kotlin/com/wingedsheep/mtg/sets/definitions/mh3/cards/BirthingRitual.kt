package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Birthing Ritual — Modern Horizons 3 #146
 * {1}{G} Enchantment · Mythic
 *
 * At the beginning of your end step, if you control a creature, look at the top seven cards of
 * your library. Then you may sacrifice a creature. If you do, you may put a creature card with
 * mana value X or less from among those cards onto the battlefield, where X is 1 plus the
 * sacrificed creature's mana value. Put the rest on the bottom of your library in a random order.
 *
 * Modeling notes:
 *  - "If you control a creature" is an intervening if (checked on trigger and on resolution).
 *  - The seven cards are looked at first; the sacrifice is a resolution-time "you may" offered
 *    as an up-to-one choice among your creatures (the Kethek, Crucible Goliath idiom).
 *  - X reads the sacrificed creature's last-known mana value through
 *    `EffectTarget.SacrificedAsCost(0)`, the snapshot the pipeline's sacrifice records.
 *  - Declining the sacrifice puts all seven on the bottom in a random order.
 */
val BirthingRitual = card("Birthing Ritual") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, if you control a creature, look at the top " +
        "seven cards of your library. Then you may sacrifice a creature. If you do, you may put a " +
        "creature card with mana value X or less from among those cards onto the battlefield, " +
        "where X is 1 plus the sacrificed creature's mana value. Put the rest on the bottom of " +
        "your library in a random order."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouControl(GameObjectFilter.Creature)
        effect = Effects.Pipeline {
            val ritualLooked = gather(CardSource.TopOfLibrary(7, Player.You))
            val ritualCreatures = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                )
            )
            val ritualSacrifice = chooseUpTo(
                1,
                from = ritualCreatures,
                useTargetingUI = true,
                prompt = "You may sacrifice a creature",
                selectedLabel = "Sacrifice",
            )
            ifNotEmpty(ritualSacrifice) {
                sacrifice(ritualSacrifice)
                val (ritualChosen, ritualRest) = chooseUpToSplit(
                    1,
                    from = ritualLooked,
                    filter = GameObjectFilter.Creature.manaValueAtMostDynamic(
                        DynamicAmounts.manaValueOf(EffectTarget.SacrificedAsCost(0)) + 1
                    ),
                    showAllCards = true,
                    prompt = "You may put a creature card with mana value X or less onto the " +
                        "battlefield",
                    selectedLabel = "Put onto the battlefield",
                    remainderLabel = "Put on the bottom",
                )
                move(ritualChosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                toLibraryBottom(ritualRest, order = CardOrder.Random)
            } orElse {
                toLibraryBottom(ritualLooked, order = CardOrder.Random)
            }
        }
        description = "At the beginning of your end step, if you control a creature, look at the " +
            "top seven cards of your library. Then you may sacrifice a creature. If you do, you " +
            "may put a creature card with mana value X or less from among those cards onto the " +
            "battlefield, where X is 1 plus the sacrificed creature's mana value. Put the rest on " +
            "the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "146"
        artist = "Drew Tucker"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/4820d223-4ea1-4850-931c-3d2ab5eb003b.jpg?1783911264"
        ruling("2024-06-07", "If Birthing Ritual's ability does trigger but you control no creatures when it tries to resolve, the ability will do nothing.")
        ruling("2024-06-07", "Use the mana value of the sacrificed creature as it last existed on the battlefield to determine the value of X.")
        ruling("2024-06-07", "If one of the revealed creature cards has {X} in its mana cost, X is 0 when determining that card's mana value.")
    }
}
