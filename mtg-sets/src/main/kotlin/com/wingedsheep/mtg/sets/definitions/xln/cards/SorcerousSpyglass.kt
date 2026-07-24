package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities

/**
 * Sorcerous Spyglass
 * Artifact — {2}
 *
 * As this artifact enters, look at an opponent's hand, then choose any card name.
 * Activated abilities of sources with the chosen name can't be activated unless they're mana abilities.
 *
 * Modeling notes:
 * - "As this artifact enters, look at an opponent's hand, then choose any card name" is a single
 *   as-enters replacement (CR 614.12). Modeled with [EntersWithChoice]`(ChoiceType.CARD_NAME)`:
 *     - `cardNamePool = CardNamePool.ANY` offers *every* registered card name (not just lands, the
 *       Petrified Hamlet default) — the "choose any card name" clause.
 *     - `lookAtOpponentHand = true` reveals an opponent's hand to this artifact's controller
 *       immediately before the choice (a durable reveal via `RevealedToComponent`; "look at" is not a
 *       keyword action — a player normally can't see an opponent's hand, CR 402.3). The look is purely
 *       informational — the chosen name is unrestricted, so an empty opposing hand still lets you name
 *       any card. Masking is automatic: the hand shows only to the controller.
 *   The pick is stored durably on the permanent's `CastChoicesComponent` under `ChoiceSlot.CARD_NAME`.
 * - "Activated abilities of sources with the chosen name can't be activated unless they're mana
 *   abilities" → [PreventActivatedAbilities]`(filter, nonManaAbilitiesOnly = true)`. "Sources" is any
 *   object, so the filter is the bare chosen-name predicate ([GameObjectFilter.namedFromChosenComponent],
 *   → `CardPredicate.NameEqualsChosenComponent`), which keys off this permanent's durable choice and
 *   is static-projection / activation-legality safe. Mana abilities of the named source still work;
 *   the lock covers every battlefield source sharing the chosen name (matched via projected state).
 * - Same static shape as Petrified Hamlet ("choose a land card name"); this card only differs by the
 *   wider name pool and the look-at-hand clause.
 */
val SorcerousSpyglass = card("Sorcerous Spyglass") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "As this artifact enters, look at an opponent's hand, then choose any card name.\n" +
        "Activated abilities of sources with the chosen name can't be activated unless they're mana abilities."

    // As this artifact enters, look at an opponent's hand, then choose any card name.
    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.ANY,
            lookAtOpponentHand = true,
        )
    )

    // Activated abilities of sources with the chosen name can't be activated unless mana abilities.
    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Any.namedFromChosenComponent(),
            nonManaAbilitiesOnly = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "248"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85506a24-8d60-475c-9f43-65994caca7d4.jpg?1783935702"
    }
}
