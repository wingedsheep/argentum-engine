package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stinging Study — Commander 2021 #44 (canonical / earliest printing)
 * {4}{B} · Instant
 *
 * You draw X cards and you lose X life, where X is the mana value of a commander you own on the
 * battlefield or in the command zone.
 *
 * "A commander you own" is gathered across every player's battlefield and command zone and
 * narrowed by `.commander().ownedByYou()` — so a commander of yours another player has taken
 * still counts (ruling), while an opponent's commander or a command-zone emblem never does. With
 * several such commanders you choose one as the spell resolves (ruling); X is that card's mana
 * value, stored once so the draw and the life loss agree. No commander at all makes X = 0.
 */
val StingingStudy = card("Stinging Study") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "You draw X cards and you lose X life, where X is the mana value of a commander you own " +
        "on the battlefield or in the command zone."

    spell {
        effect = Effects.Pipeline {
            val commanders = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.BATTLEFIELD, Zone.COMMAND),
                    player = Player.Each,
                    filter = GameObjectFilter.Any.commander().ownedByYou(),
                ),
            )
            val chosen = chooseExactly(
                count = 1,
                from = commanders,
                prompt = "Choose a commander you own — X is its mana value",
            )
            val x = storeNumber(DynamicAmounts.manaValueOf(chosen))
            run(Effects.DrawCards(x.amount))
            run(Effects.LoseLife(x.amount, EffectTarget.Controller))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Kieran Yanner"
        flavorText = "Silverquill first-years welcome discomfort in the classroom, knowing it will prepare them for the years ahead."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8840226-1693-44bc-a067-e50198c5e17e.jpg?1783927597"
        ruling(
            "2021-04-16",
            "If your commander is under the control of another player as Stinging Study resolves, it is " +
                "still on the battlefield and will still be used to determine the value of X."
        )
        ruling(
            "2021-04-16",
            "If you own multiple commanders on the battlefield and/or in the command zone, you choose one " +
                "of them as Stinging Study resolves to determine the value of X."
        )
    }
}
