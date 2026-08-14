package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.disturb
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Covetous Castaway // Ghostly Castigator (Innistrad: Midnight Hunt #45 — the card's earliest
 * printing; also reprinted in Innistrad Remastered)
 * {1}{U} · Creature — Human 1/3 // Creature — Spirit 3/4
 *
 * Front — Covetous Castaway ({1}{U}, Creature — Human, 1/3)
 *   When this creature dies, mill three cards.
 *   Disturb {3}{U}{U}
 *
 * Back — Ghostly Castigator (Creature — Spirit, 3/4, blue color indicator)
 *   Flying
 *   When this creature enters, you may shuffle up to three target cards from your graveyard into
 *   your library.
 *   If Ghostly Castigator would be put into a graveyard from anywhere, exile it instead.
 *
 * Implementation:
 *  - Disturb (CR 702.146) is the [disturb] keyword on the front face; the graveyard cast puts
 *    the Castigator on the stack back face up (CR 712.8c). The front's dies trigger is what stocks
 *    the graveyard the Castigator's own entry can then reshuffle.
 *  - "Up to three target cards from your graveyard" is a [TargetObject] with `count = 3,
 *    optional = true` over an owned-by-you graveyard filter, and [ForEachTargetEffect] moves each
 *    chosen card to its owner's library followed by one [ShuffleLibraryEffect] — the Gaea's Blessing
 *    shape. The "you may" is a separate resolution-time decision ([MayEffect]) because the targets
 *    were locked in when the trigger went on the stack.
 *  - The exile-instead clause is [RedirectZoneChange] with `selfOnly = true`, carried on the card
 *    entity so it functions in every zone (CR 614.12) — a countered disturb spell is exiled.
 */
private val CovetousCastawayFront = card("Covetous Castaway") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human"
    power = 1
    toughness = 3
    oracleText = "When this creature dies, mill three cards. (Put the top three cards of your " +
        "library into your graveyard.)\n" +
        "Disturb {3}{U}{U} (You may cast this card from your graveyard transformed for its disturb cost.)"

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Patterns.Library.mill(3)
        description = "When this creature dies, mill three cards."
    }

    disturb("{3}{U}{U}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03a3ea4b-d292-4602-985f-7a7971ca73ec.jpg?1783925651"
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

private val GhostlyCastigator = card("Ghostly Castigator") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, you may shuffle up to three target cards from your graveyard " +
        "into your library.\n" +
        "If Ghostly Castigator would be put into a graveyard from anywhere, exile it instead."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetObject(
            count = 3,
            optional = true,
            filter = TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD),
            id = "three target cards from your graveyard",
        )
        effect = MayEffect(
            ForEachTargetEffect(
                effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.LIBRARY))
            ).then(ShuffleLibraryEffect()),
            descriptionOverride = "Shuffle the targeted cards from your graveyard into your library?",
        )
        description = "When this creature enters, you may shuffle up to three target cards from " +
            "your graveyard into your library."
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
        collectorNumber = "45"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/back/0/3/03a3ea4b-d292-4602-985f-7a7971ca73ec.jpg?1783925651"
    }
}

val CovetousCastaway: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = CovetousCastawayFront,
    backFace = GhostlyCastigator,
)
