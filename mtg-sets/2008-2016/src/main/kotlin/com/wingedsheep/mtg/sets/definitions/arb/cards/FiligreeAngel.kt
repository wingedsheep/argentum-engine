package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Filigree Angel
 * {5}{W}{W}{U}
 * Artifact Creature — Angel
 * 4/4
 * Flying
 * When this creature enters, you gain 3 life for each artifact you control.
 *
 * "3 life for each artifact you control" is a [DynamicAmount.Multiply] over a
 * [DynamicAmount.AggregateBattlefield] count of [GameObjectFilter.Artifact] for [Player.You] — the
 * player scopes the count, so the filter carries no controller predicate of its own. The count is
 * *not* `excludeSelf`: Filigree Angel counts itself, per its own ruling. [Effects.GainLife]'s target
 * defaults to the controller, so it is left unwritten.
 */
val FiligreeAngel = card("Filigree Angel") {
    manaCost = "{5}{W}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Artifact Creature — Angel"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, you gain 3 life for each artifact you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(
            DynamicAmount.Multiply(
                DynamicAmount.AggregateBattlefield(
                    player = Player.You,
                    filter = GameObjectFilter.Artifact
                ),
                3
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Richard Whitters"
        flavorText = "\"I craved enlightenment, and Crucius's etherium opened my eyes. I would share my sight with you, but first you must believe.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a0c7a2a-85c1-4b20-95b9-04aab0bd0d3b.jpg"
        ruling("2009-05-01", "Filigree Angel's \"enters\" ability counts Filigree Angel itself, assuming that it's still on the battlefield and still an artifact by the time the ability resolves.")
    }
}
