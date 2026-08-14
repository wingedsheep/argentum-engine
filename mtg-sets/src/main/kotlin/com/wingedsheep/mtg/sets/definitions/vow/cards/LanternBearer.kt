package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.disturb
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Lantern Bearer // Lanterns' Lift (Innistrad: Crimson Vow #66 — the card's earliest printing;
 * also reprinted in Innistrad Remastered)
 * {U} · Creature — Spirit 1/1 // Enchantment — Aura
 *
 * Front — Lantern Bearer ({U}, Creature — Spirit, 1/1)
 *   Flying
 *   Disturb {2}{U}
 *
 * Back — Lanterns' Lift (Enchantment — Aura, blue color indicator)
 *   Enchant creature
 *   Enchanted creature gets +1/+1 and has flying.
 *   If Lanterns' Lift would be put into a graveyard from anywhere, exile it instead.
 *
 * Implementation:
 *  - Disturb (CR 702.146) with an **Aura** back face: the spell goes on the stack back face up
 *    (CR 712.8c), so it is an Aura spell and the creature it will enchant is chosen as it is cast,
 *    from the back face's `auraTarget` — not from anything on the front face.
 *  - The Aura's two clauses are the standard pair of statics, [ModifyStats] and [GrantKeyword],
 *    exactly as on a printed Aura like Spectral Flight.
 *  - The exile-instead clause is [RedirectZoneChange] with `selfOnly = true`, so it functions in
 *    every zone (CR 614.12): the Aura is exiled when it is put into a graveyard for having nothing
 *    to enchant, and a countered disturb spell is exiled rather than returning to the graveyard.
 */
private val LanternBearerFront = card("Lantern Bearer") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Disturb {2}{U} (You may cast this card from your graveyard transformed for its disturb cost.)"

    keywords(Keyword.FLYING)
    disturb("{2}{U}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Zoltan Boros"
        flavorText = "Val didn't know what the silent geist was offering . . ."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4d3652a-6774-4b16-aa8b-cb11d72ec7aa.jpg?1783924899"
        ruling(
            "2021-09-24",
            "When you cast a spell using a card's disturb ability, the card is put onto the stack " +
                "with its back face up. The resulting spell has all the characteristics of that face."
        )
        ruling(
            "2021-09-24",
            "The mana value of a spell cast using disturb is determined by the mana cost on the " +
                "front face of the card, no matter what the total cost to cast the spell was."
        )
        ruling(
            "2021-09-24",
            "The back face of each card with disturb has an ability that instructs its controller " +
                "to exile it if it would be put into a graveyard from anywhere. This includes going " +
                "to the graveyard from the stack, so if the spell is countered after you cast it " +
                "using the disturb ability, it will be put into exile."
        )
    }
}

private val LanternsLift = card("Lanterns' Lift") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has flying.\n" +
        "If Lanterns' Lift would be put into a graveyard from anywhere, exile it instead."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 1)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Zoltan Boros"
        flavorText = ". . . but she knew it would be rude to refuse."
        imageUri = "https://cards.scryfall.io/normal/back/a/4/a4d3652a-6774-4b16-aa8b-cb11d72ec7aa.jpg?1783924899"
    }
}

val LanternBearer: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = LanternBearerFront,
    backFace = LanternsLift,
)
