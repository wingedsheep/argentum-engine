package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Museum Nightwatch — Murders at Karlov Manor #25
 * {3}{W} · Creature — Centaur Soldier · 3/2
 *
 * When this creature dies, create a 2/2 white and blue Detective creature token.
 * Disguise {1}{W}
 *
 * A body that trades and then leaves a body behind, so it's never a bad block. The dies trigger is
 * unconditional — it fires on a combat trade, on removal, and on a sacrifice, and the Detective is
 * created even though the Nightwatch is already in the graveyard when it resolves.
 *
 * Disguise interacts with the death trigger the way it does with every ability on a face-down card:
 * a face-down Museum Nightwatch is a 2/2 with ward {2} and *no* abilities (CR 702.168a / 708.2), so
 * if it dies while face down no Detective arrives. Flipping it for {1}{W} is cheap precisely
 * because turning it face up is what arms the trigger.
 *
 * The token's art comes from the MKM `tokenArt` layer (a 2/2 white-and-blue Detective is one of the
 * set's printed tokens), so no `imageUri` is baked in here — same as Person of Interest.
 */
val MuseumNightwatch = card("Museum Nightwatch") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Centaur Soldier"
    oracleText = "When this creature dies, create a 2/2 white and blue Detective creature token.\n" +
        "Disguise {1}{W} (You may cast this card face down for {3} as a 2/2 creature with ward " +
        "{2}. Turn it face up any time for its disguise cost.)"
    power = 3
    toughness = 2
    disguise = "{1}{W}"

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE, Color.BLUE),
            creatureTypes = setOf("Detective")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Alix Branwyn"
        flavorText = "\"You there, halt! The gallery is closed. Wait—what are you doing?\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37860682-4973-4a0f-a43a-3056037bd2dc.jpg?1783912921"

        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
        ruling(
            "2024-02-02",
            "If a face-down permanent leaves the battlefield, you must reveal it."
        )
    }
}
