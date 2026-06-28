package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * North Wind Avatar
 * {2}{U}{U}{R}
 * Creature — Dragon Spirit Avatar
 * 5/5
 *
 * Flying
 * When this creature enters, if you cast it, you may put a card you own from
 * outside the game into your hand.
 *
 * A "wish" gated on an intervening-if cast clause: only the cast-from-hand (or any-zone) ETB
 * pays off, never a reanimation/token copy ([Conditions.WasCast], as on Sunderflock). The wish
 * itself is the Gather → Select → Move sideboard pipeline ([Patterns.Sideboard.wish]) over an
 * *any-card* filter — "a card you own from outside the game". Unlike the Burning Wish cycle this
 * card has **no "reveal that card" clause**, so the move is `revealed = false`; the chosen card
 * stays hidden as it goes to hand.
 */
val NorthWindAvatar = card("North Wind Avatar") {
    manaCost = "{2}{U}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Dragon Spirit Avatar"
    oracleText = "Flying\n" +
        "When this creature enters, if you cast it, you may put a card you own from outside the game into your hand."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasCast
        effect = Patterns.Sideboard.wish(GameObjectFilter.Any, revealed = false)
        description = "When this creature enters, if you cast it, you may put a card you own from outside the game into your hand."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "162"
        artist = "Andrey Kuzinskiy"
        flavorText = "Glyph has claimed to be a survivor of an alien war, a deposed minister of weather, and the last of the dinosaurs. Whatever the truth, his origin stories always bear a lesson."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41439cfe-bb3e-42f3-9d81-de9db537ce68.jpg?1769006359"
    }
}
