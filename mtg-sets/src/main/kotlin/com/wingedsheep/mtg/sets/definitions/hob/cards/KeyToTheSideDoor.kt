package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect

/**
 * Key to the Side-Door — The Hobbit #175
 * {1} · Artifact · Uncommon
 *
 * {2}, {T}: Target creature can't be blocked this turn.
 * {1}, {T}, Discard a legendary card with the same name as a legendary permanent you control:
 *   Draw two cards.
 *
 * Modeling notes:
 *  - "Can't be blocked this turn" is a duration-bounded [AbilityFlag.CANT_BE_BLOCKED] grant on the
 *    target (the Rogue's Passage idiom), not the permanent [CantBeBlocked] static — the latter would
 *    outlive the turn.
 *  - The second ability's discard cost is the interesting half: the discarded card must be legendary
 *    *and* name-match a legendary permanent already on your battlefield. That cross-zone comparison
 *    is [CardPredicate.SharesNameWithPermanentYouControl], the name sibling of the existing
 *    `SharesColorWithPermanentYouControl` — the cost enumerators hand it a `PredicateContext` with
 *    the activating player as `controllerId`, so the battlefield side is correctly scoped to "you
 *    control". Without a matching pair in hand and play the ability simply isn't offered.
 *  - The legendary-permanent side reads projected state, so a permanent that only became legendary
 *    through a continuous effect still counts.
 */
val KeyToTheSideDoor = card("Key to the Side-Door") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Target creature can't be blocked this turn.\n" +
        "{1}, {T}, Discard a legendary card with the same name as a legendary permanent you " +
        "control: Draw two cards."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val creature = target("target creature", Targets.Creature)
        effect = GrantKeywordEffect(AbilityFlag.CANT_BE_BLOCKED.name, creature)
        description = "Target creature can't be blocked this turn."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.Discard(
                GameObjectFilter.Any
                    .legendary()
                    .sharingNameWithPermanentYouControl(GameObjectFilter.Permanent.legendary())
            )
        )
        effect = Effects.DrawCards(2)
        description = "Draw two cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Nathaniel Himawan"
        flavorText = "\"I forgot to mention that with the map went a key, a small and curious " +
            "key.\"\n—Gandalf, to Thorin"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/898c14a2-d897-4341-83ed-eee666df9648.jpg?1785412757"
    }
}
