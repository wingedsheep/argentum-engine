package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities

/**
 * Linvala, Keeper of Silence — Rise of the Eldrazi #33
 * {2}{W}{W} · Legendary Creature — Angel · 3/4
 *
 * Flying
 * Activated abilities of creatures your opponents control can't be activated.
 *
 * Cursed Totem's lock narrowed to opponents' creatures; mana abilities are included (ruling), and
 * only creatures on the battlefield are affected, so unearth/bloodrush still work.
 */
val LinvalaKeeperOfSilence = card("Linvala, Keeper of Silence") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Angel"
    power = 3
    toughness = 4
    oracleText = "Flying\nActivated abilities of creatures your opponents control can't be activated."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = PreventActivatedAbilities(GameObjectFilter.Creature.opponentControls())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "33"
        artist = "Igor Kieryluk"
        flavorText = "Those who seek to disturb the harmony of life will see their instruments taken from them."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82b80a09-7e75-4091-a60e-04aff79339a3.jpg?1783942005"
        ruling("2017-03-14", "No abilities of creatures your opponents control can be activated, including mana abilities.")
        ruling("2017-03-14", "Linvala's last ability affects only creatures on the battlefield. Activated abilities that work in other zones (such as bloodrush or unearth) can still be activated. Triggered abilities (starting with \"when,\" \"whenever,\" or \"at\") are unaffected.")
    }
}
