package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nissa, Vastwood Seer // Nissa, Sage Animist — Magic Origins #189
 * {2}{G} · Legendary Creature — Elf Scout 2/2 // Legendary Planeswalker — Nissa (loyalty 3)
 *
 * Front:
 *   When Nissa enters, you may search your library for a basic Forest card, reveal it, put it
 *   into your hand, then shuffle.
 *   Whenever a land you control enters, if you control seven or more lands, exile Nissa, then
 *   return her to the battlefield transformed under her owner's control.
 * Back:
 *   +1: Reveal the top card of your library. If it's a land card, put it onto the battlefield.
 *       Otherwise, put it into your hand.
 *   −2: Create Ashaya, the Awoken World, a legendary 4/4 green Elemental creature token.
 *   −7: Untap up to six target lands. They become 6/6 Elemental creatures. They're still lands.
 *
 * Modeling notes:
 *  - The flip is an intervening-if landfall trigger: the seven-lands count is checked both when a
 *    land enters and again on resolution. Nissa entering while you already control seven lands
 *    doesn't flip her — only a later land does (ruling 2015-06-22). When several lands enter at
 *    once, the first trigger to resolve flips her; the rest find their source a new object and do
 *    nothing.
 *  - −2 makes a named legendary token; a second Ashaya falls to the legend rule (ruling 2015-06-22).
 *  - −7 has no "until end of turn": the animation is [Duration.Permanent]. `BecomeCreature` adds
 *    the creature type and the Elemental subtype without removing Land, so they're still lands.
 */
private val NissaVastwoodSeerFront = card("Nissa, Vastwood Seer") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Scout"
    power = 2
    toughness = 2
    oracleText = "When Nissa enters, you may search your library for a basic Forest card, reveal it, " +
        "put it into your hand, then shuffle.\n" +
        "Whenever a land you control enters, if you control seven or more lands, exile Nissa, then " +
        "return her to the battlefield transformed under her owner's control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.FOREST),
            destination = SearchDestination.HAND,
            reveal = true,
        )
        description = "When Nissa enters, you may search your library for a basic Forest card, reveal it, " +
            "put it into your hand, then shuffle."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        interveningIf = Conditions.YouControlAtLeast(7, GameObjectFilter.Land)
        effect = Effects.ExileAndReturnTransformed(EffectTarget.Self)
        description = "Whenever a land you control enters, if you control seven or more lands, exile Nissa, " +
            "then return her to the battlefield transformed under her owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "189"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff0063da-ab6b-499d-8e87-8b34d46f0372.jpg?1783938322"

        ruling("2017-09-29", "You can control two of this permanent, one front-face up and the other back-face up, at the same time.")
        ruling("2015-06-22", "Nissa, Vastwood Seer is exiled as a result of her second triggered ability. If she enters the battlefield while you control seven or more lands, she won't automatically be exiled and transform.")
        ruling("2015-06-22", "A Magic Origins planeswalker that enters the battlefield because of the ability of its front face will enter with loyalty counters as normal.")
        ruling("2015-06-22", "In some rare cases, a spell or ability may cause one of these five cards to transform while it's a creature (front face up) on the battlefield. If this happens, the resulting planeswalker won't have any loyalty counters on it and will subsequently be put into its owner's graveyard.")
        ruling("2015-06-22", "You can activate one of the planeswalker's loyalty abilities the turn it enters the battlefield. However, you may do so only during one of your main phases when the stack is empty. For example, if the planeswalker enters the battlefield during combat, there will be an opportunity for your opponent to remove it before you can activate one of its abilities.")
    }
}

private val NissaSageAnimist = card("Nissa, Sage Animist") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Legendary Planeswalker — Nissa"
    startingLoyalty = 3
    oracleText = "+1: Reveal the top card of your library. If it's a land card, put it onto the " +
        "battlefield. Otherwise, put it into your hand.\n" +
        "−2: Create Ashaya, the Awoken World, a legendary 4/4 green Elemental creature token.\n" +
        "−7: Untap up to six target lands. They become 6/6 Elemental creatures. They're still lands."

    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1))
            reveal(top, fromZone = Zone.LIBRARY)
            val (lands, nonlands) = filterSplit(top, GameObjectFilter.Land)
            move(lands, CardDestination.ToZone(Zone.BATTLEFIELD))
            toHand(nonlands)
        }
        description = "Reveal the top card of your library. If it's a land card, put it onto the " +
            "battlefield. Otherwise, put it into your hand."
    }

    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elemental"),
            name = "Ashaya, the Awoken World",
            legendary = true,
            imageUri = "https://cards.scryfall.io/normal/front/0/a/0affd414-f774-48d1-af9e-bff74e58e1ca.jpg?1783938294",
        )
        description = "Create Ashaya, the Awoken World, a legendary 4/4 green Elemental creature token."
    }

    loyaltyAbility(-7) {
        targets(TargetFilter.Land, count = 6, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Untap(EffectTarget.ContextTarget(0)) then
                Effects.BecomeCreature(
                    target = EffectTarget.ContextTarget(0),
                    power = 6,
                    toughness = 6,
                    creatureTypes = setOf("Elemental"),
                    duration = Duration.Permanent,
                )
        )
        description = "Untap up to six target lands. They become 6/6 Elemental creatures. They're still lands."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "189"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/back/f/f/ff0063da-ab6b-499d-8e87-8b34d46f0372.jpg?1783938322"

        ruling("2015-06-22", "You can activate Nissa, Sage Animist's second ability even if you already control an Ashaya, the Awoken World. Just after the second token is created, you'll choose one to remain on the battlefield. The other will be put into your graveyard and subsequently cease to exist.")
        ruling("2015-06-22", "If a land becomes a creature but hasn't continuously been under its controller's control since that player's most recent turn began, it won't be able to attack and its abilities with {T} in the cost (including mana abilities) won't be able to be activated. In other words, look at how long the permanent itself has been under your control, not how long it's been a creature.")
    }
}

val NissaVastwoodSeer: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = NissaVastwoodSeerFront,
    backFace = NissaSageAnimist,
)
