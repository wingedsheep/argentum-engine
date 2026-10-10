package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyKeywordActionAmount
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Water Crystal
 * {2}{U}{U}
 * Legendary Artifact
 *
 * Blue spells you cast cost {1} less to cast.
 * If an opponent would mill one or more cards, they mill that many cards plus four instead.
 * {4}{U}{U}, {T}: Each opponent mills cards equal to the number of cards in your hand.
 *
 * Notes:
 *  - The cost reduction reduces only generic mana in the total cost of blue spells you cast
 *    (Scryfall ruling 2025-06-06); modeled with [CostModification.ReduceGeneric], exactly like
 *    its cycle siblings (The Wind Crystal, etc.).
 *  - "they mill that many cards plus four instead" is the mill twin of The Wind Crystal's
 *    [com.wingedsheep.sdk.scripting.ModifyLifeGain]: a [ModifyKeywordActionAmount] additive replacement
 *    scoped to mills by an opponent ([EventPattern.MillEvent] with [Player.EachOpponent]).
 *    Applied at the mill announcement, so it boosts *any* source's opponent mill — not just this
 *    card's activated ability. Two copies add eight, three add twelve, etc. (Scryfall ruling
 *    2025-06-06), because each is a separate replacement summed in turn. A base mill of zero is
 *    untouched ("would mill one or more cards").
 *  - The activated ability mills each opponent. The amount is the number of cards in *your* hand,
 *    so the count is `DynamicAmount.Count(Player.You, HAND)` evaluated in the ability's own
 *    context and the mill target is `Player.EachOpponent` — the gather fans out across every
 *    opponent's library, and each milled card goes to its owner's graveyard. (Using a
 *    Player.You–anchored count rather than wrapping in ForEachPlayer keeps "your hand" pointing
 *    at the controller, not at each opponent.)
 */
val TheWaterCrystal = card("The Water Crystal") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact"
    oracleText = "Blue spells you cast cost {1} less to cast.\n" +
        "If an opponent would mill one or more cards, they mill that many cards plus four instead.\n" +
        "{4}{U}{U}, {T}: Each opponent mills cards equal to the number of cards in your hand."

    // Blue spells you cast cost {1} less to cast.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withColor(Color.BLUE)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    // If an opponent would mill one or more cards, they mill that many cards plus four instead.
    replacementEffect(
        ModifyKeywordActionAmount(
            appliesTo = EventPattern.MillEvent(player = Player.EachOpponent),
            modifier = 4,
        )
    )

    // {4}{U}{U}, {T}: Each opponent mills cards equal to the number of cards in your hand.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{U}{U}"), Costs.Tap)
        effect = Patterns.Library.mill(
            DynamicAmounts.cardsInYourHand(),
            EffectTarget.PlayerRef(Player.EachOpponent),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Pablo Mendoza"
        flavorText = "\"Crystal of water, may you regain your light.\"\n—Aria Benett"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0af8436-797b-4e1f-b21a-d8e93701c3c9.jpg?1748706080"
    }
}
