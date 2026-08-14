package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.webSlinging
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Arachne, Psionic Weaver (Marvel's Spider-Man, #2)
 * {2}{W}
 * Legendary Creature — Spider Human Hero
 * 3/3
 *
 * Web-slinging {W} (You may cast this spell for {W} if you also return a tapped creature you control
 * to its owner's hand.)
 * As Arachne enters, look at an opponent's hand, then choose a card type other than creature.
 * Spells of the chosen type cost {1} more to cast.
 *
 * Implementation:
 *  - **Web-slinging {W}** — the `webSlinging(cost)` alternative-cost keyword (CR 702.188).
 *  - **Choose a card type (durable)** — a "When Arachne enters" trigger running
 *    [Effects.ChooseCardTypeForSource] with `lookAtOpponentHand = true` and the seven non-creature
 *    card types. It reveals an opponent's hand to the controller, then writes the chosen type onto
 *    Arachne's `CastChoicesComponent` under [com.wingedsheep.sdk.scripting.ChoiceSlot.CARD_TYPE].
 *    (An ETB trigger + durable-slot write — the on-resolution analogue of an as-enters replacement,
 *    the same relationship `ChooseNumberForSource` has to the `NUMBER` enters-with choice — because
 *    the chosen type only feeds a continuous tax, so it need not be locked in strictly during entry.)
 *  - **The tax** — a symmetric [ModifySpellCost] on every caster whose spell is
 *    `ofChosenCardTypeComponent()` (matches the type stored on Arachne), adding {1} generic
 *    ([CostModification.IncreaseGeneric]) — the same shape as Thalia, keyed to the chosen type via
 *    [com.wingedsheep.sdk.scripting.predicates.CardPredicate.CardTypeEqualsChosenComponent].
 */
val ArachnePsionicWeaver = card("Arachne, Psionic Weaver") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 3
    toughness = 3
    oracleText = "Web-slinging {W} (You may cast this spell for {W} if you also return a tapped " +
        "creature you control to its owner's hand.)\n" +
        "As Arachne enters, look at an opponent's hand, then choose a card type other than creature.\n" +
        "Spells of the chosen type cost {1} more to cast."

    webSlinging("{W}")

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.ChooseCardTypeForSource(
            allowedCardTypes = listOf(
                "Artifact", "Battle", "Enchantment", "Instant", "Land", "Planeswalker", "Sorcery"
            ),
            lookAtOpponentHand = true,
            prompt = "Choose a card type other than creature"
        )
        description = "As Arachne enters, look at an opponent's hand, then choose a card type " +
            "other than creature."
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.AnyCaster(GameObjectFilter.Any.ofChosenCardTypeComponent()),
            modification = CostModification.IncreaseGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Steve Argyle"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c1f871a-bd85-402e-b474-1deb64c18a52.jpg?1783905365"
    }
}
