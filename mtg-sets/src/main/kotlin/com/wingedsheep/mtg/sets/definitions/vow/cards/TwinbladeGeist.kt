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
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Twinblade Geist // Twinblade Invocation (Innistrad: Crimson Vow #40 — the card's earliest
 * printing; also reprinted in Innistrad Remastered)
 * {1}{W} · Creature — Spirit Warrior 1/1 // Enchantment — Aura
 *
 * Front — Twinblade Geist ({1}{W}, Creature — Spirit Warrior, 1/1)
 *   Double strike
 *   Disturb {2}{W}
 *
 * Back — Twinblade Invocation (Enchantment — Aura, white color indicator)
 *   Enchant creature
 *   Enchanted creature has double strike.
 *   If Twinblade Invocation would be put into a graveyard from anywhere, exile it instead.
 *
 * Implementation: the white sibling of [LanternBearer] — a disturb card (CR 702.146) whose back
 * face is an Aura, so the disturb cast is an Aura spell that picks what it enchants from the back
 * face's `auraTarget` (CR 712.8c). The Aura's single clause is a [GrantKeyword] static, and the
 * "would be put into a graveyard from anywhere, exile it instead" line is [RedirectZoneChange] with
 * `selfOnly = true` so it functions in every zone (CR 614.12).
 */
private val TwinbladeGeistFront = card("Twinblade Geist") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Warrior"
    power = 1
    toughness = 1
    oracleText = "Double strike\n" +
        "Disturb {2}{W} (You may cast this card from your graveyard transformed for its disturb cost.)"

    keywords(Keyword.DOUBLE_STRIKE)
    disturb("{2}{W}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Tuan Duong Chu"
        flavorText = "She faced down a howlpack alone, saving her town at the cost of her life."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1deb24b-3d8f-4251-a901-85eeb891f26f.jpg?1783924914"
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

private val TwinbladeInvocation = card("Twinblade Invocation") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has double strike.\n" +
        "If Twinblade Invocation would be put into a graveyard from anywhere, exile it instead."

    auraTarget = Targets.Creature

    staticAbility {
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE)
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Tuan Duong Chu"
        flavorText = "Since her passing, many more travelers have made it home alive."
        imageUri = "https://cards.scryfall.io/normal/back/f/1/f1deb24b-3d8f-4251-a901-85eeb891f26f.jpg?1783924914"
    }
}

val TwinbladeGeist: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TwinbladeGeistFront,
    backFace = TwinbladeInvocation,
)
