package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gadget Technician — Murders at Karlov Manor #204
 * {2}{U}{R} · Creature — Goblin Artificer · 3/2
 *
 * When this creature enters or is turned face up, create a 1/1 colorless Thopter artifact creature
 * token with flying.
 * Disguise {U/R}{U/R}
 *
 * "Enters **or** is turned face up" is one triggered ability with two trigger conditions, so it is
 * [Triggers.or] over the two event patterns rather than two `triggeredAbility` blocks. The two
 * readings are indistinguishable in play — turning face up is a special action and not entering
 * (CR 701.34), so a permanent can never satisfy both at once — but the single ability is what the
 * card actually prints, and it keeps the client's ability list matching the oracle text.
 *
 * The disguise cost is hybrid {U/R}{U/R}, which is the whole point of the card in limited: a mono-U
 * or mono-R deck can still flip it, so the two-mana flip is available in either half of the pair
 * even though the hard cast needs both colors.
 */
val GadgetTechnician = card("Gadget Technician") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Goblin Artificer"
    oracleText = "When this creature enters or is turned face up, create a 1/1 colorless Thopter " +
        "artifact creature token with flying.\n" +
        "Disguise {U/R}{U/R} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)"
    power = 3
    toughness = 2

    disguise = "{U/R}{U/R}"

    triggeredAbility {
        trigger = Triggers.or(Triggers.EntersBattlefield, Triggers.TurnedFaceUp)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            name = "Thopter",
            imageUri = "https://cards.scryfall.io/normal/front/f/8/f80c5f0a-5573-4dc2-8791-35e35bdf4c78.jpg?1783912602"
        )
        description = "When this creature enters or is turned face up, create a 1/1 colorless " +
            "Thopter artifact creature token with flying."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "204"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b489a54-ee43-4962-be7b-16e0e28800e0.jpg?1783912848"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to. Only a face-down permanent can " +
                "be turned face up this way; a face-down spell cannot."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
        ruling(
            "2024-02-02",
            "If a face-down creature loses its abilities, it can't be turned face up with a " +
                "disguise ability because it will no longer have a disguise ability (or a " +
                "disguise cost) once face up."
        )
    }
}
