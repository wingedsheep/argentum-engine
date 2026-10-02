package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kethek, Crucible Goliath
 * {2}{B}{R}
 * Legendary Creature — Phyrexian Beast
 * 4/4
 *
 * At the beginning of your end step, you may sacrifice another creature. If you do, reveal cards
 * from the top of your library until you reveal a nonlegendary creature card with lesser mana
 * value, put it onto the battlefield, then put the rest on the bottom of your library in a random
 * order.
 *
 * Modeling notes:
 *  - "You may sacrifice another creature" is the Rhovanion Rampager idiom: gather the other
 *    creatures you control and offer **up to one** on the battlefield-targeting UI. The sacrifice
 *    is a resolution-time action, not a cost.
 *  - "If you do" gates the whole reveal on the chosen collection being non-empty — declining
 *    (or having no other creature) reveals nothing.
 *  - "Lesser mana value" is a strict `<`, written as `manaValueAtMostDynamic(sacrificedMV - 1)`.
 *    The sacrificed creature's mana value is read through `EffectTarget.SacrificedAsCost(0)`, the
 *    snapshot the pipeline's sacrifice records. Sacrificing a mana-value-0 creature caps the search
 *    at -1, so no card qualifies: the whole library is revealed and goes to the bottom.
 *  - The rest is everything revealed minus the found card, put on the bottom in a random order.
 */
val KethekCrucibleGoliath = card("Kethek, Crucible Goliath") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Phyrexian Beast"
    power = 4
    toughness = 4
    oracleText = "At the beginning of your end step, you may sacrifice another creature. If you do, " +
        "reveal cards from the top of your library until you reveal a nonlegendary creature card " +
        "with lesser mana value, put it onto the battlefield, then put the rest on the bottom of " +
        "your library in a random order."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val others = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                    excludeSelf = true,
                )
            )
            val chosen = chooseUpTo(
                1,
                from = others,
                useTargetingUI = true,
                prompt = "You may sacrifice another creature",
                selectedLabel = "Sacrifice",
            )
            ifNotEmpty(chosen) {
                sacrifice(chosen)
                val lesserCreature = GameObjectFilter.Creature.nonlegendary().manaValueAtMostDynamic(
                    DynamicAmounts.manaValueOf(EffectTarget.SacrificedAsCost(0)) - 1
                )
                val (found, revealed) = gatherUntilMatch(lesserCreature)
                reveal(revealed, fromZone = Zone.LIBRARY, toZone = Zone.BATTLEFIELD)
                move(found, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                toLibraryBottom(exclude(revealed, found), order = CardOrder.Random)
            }
        }
        description = "At the beginning of your end step, you may sacrifice another creature. If " +
            "you do, reveal cards from the top of your library until you reveal a nonlegendary " +
            "creature card with lesser mana value, put it onto the battlefield, then put the rest " +
            "on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4152a5a8-e5a1-46b0-8f75-b9ac341da474.jpg?1783918000"
        ruling("2023-02-04", "If a creature on the battlefield has an {X} in its mana cost, X is 0.")
    }
}
