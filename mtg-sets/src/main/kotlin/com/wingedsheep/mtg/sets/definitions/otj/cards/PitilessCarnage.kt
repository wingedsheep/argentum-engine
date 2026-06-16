package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Pitiless Carnage {3}{B}
 * Sorcery
 *
 * Sacrifice any number of permanents you control, then draw that many cards.
 * Plot {1}{B}{B}
 *
 * "Sacrifice any number of permanents you control, then draw that many cards" is the gather →
 * choose-any-number → sacrifice → draw idiom: gather the permanents you control, let the
 * controller select any number (0..all) via on-battlefield targeting, sacrifice the selected ones
 * as a real [com.wingedsheep.sdk.scripting.effects.MoveType.Sacrifice] (so dies/sacrifice triggers
 * fire), then draw cards equal to the number actually sacrificed — counted off the selected
 * collection with [DynamicAmounts.distinctEntitiesIn]. Selecting zero is legal (sacrifice nothing,
 * draw zero).
 *
 * Plot is the standard [KeywordAbility.plot] exile-from-hand-and-cast-later mechanic.
 */
val PitilessCarnage = card("Pitiless Carnage") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Sacrifice any number of permanents you control, then draw that many cards.\n" +
        "Plot {1}{B}{B} (You may pay {1}{B}{B} and exile this card from your hand. Cast it as a " +
        "sorcery on a later turn without paying its mana cost. Plot only as a sorcery.)"

    spell {
        effect = Effects.Pipeline {
            val permanents = gather(GameObjectFilter.Permanent, player = Player.You)
            val sacrificed = chooseAnyNumber(
                from = permanents,
                useTargetingUI = true,
                prompt = "Choose any number of permanents to sacrifice"
            )
            sacrifice(sacrificed)
            run(Effects.DrawCards(DynamicAmounts.distinctEntitiesIn(sacrificed.key)))
        }
    }

    keywordAbility(KeywordAbility.plot("{1}{B}{B}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Richard Kane Ferguson"
        flavorText = "Akul saw his Hellspur followers as little more than ammunition to be expended."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa76fc45-a106-4dc9-9d44-a005eaa2784d.jpg?1748079690"
    }
}
