package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Once and Future
 * {3}{G}
 * Instant
 * Return target card from your graveyard to your hand. Put up to one other target card from your
 * graveyard on top of your library. Exile Once and Future.
 * Adamant — If at least three green mana was spent to cast this spell, instead return those cards
 * to your hand and exile Once and Future.
 *
 * Which card goes where is fixed by the target slots at cast time (ruling). The second slot is a
 * [TargetOther] so it can't repeat the first. Adamant reads green mana spent on the whole cost via
 * [DynamicAmounts.manaOfColorSpent] (see Turn into a Pumpkin). The spell exiles itself either way;
 * if both targets are illegal it fizzles and goes to the graveyard instead.
 */
val OnceAndFuture = card("Once and Future") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Return target card from your graveyard to your hand. Put up to one other target " +
        "card from your graveyard on top of your library. Exile Once and Future.\n" +
        "Adamant — If at least three green mana was spent to cast this spell, instead return those " +
        "cards to your hand and exile Once and Future."

    spell {
        val graveyardCard = TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD)
        val toHand = target(graveyardCard)
        val second = target(TargetOther(TargetObject(filter = graveyardCard)), optional = true)
        effect = Effects.Move(toHand, Zone.HAND, fromZone = Zone.GRAVEYARD) then Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.manaOfColorSpent(Color.GREEN),
                ComparisonOperator.GTE,
                3,
            ),
            Effects.Move(second, Zone.HAND, fromZone = Zone.GRAVEYARD),
            Effects.Move(second, Zone.LIBRARY, ZonePlacement.Top, fromZone = Zone.GRAVEYARD),
        )
        selfExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c5dd4e8-4d5f-45a0-9cae-4f842751894d.jpg?1783932605"

        ruling(
            "2019-10-04",
            "You choose which card will be returned to your hand and which will be put on top of " +
                "your library as you choose those cards as targets, not as Once and Future resolves.",
        )
        ruling(
            "2019-10-04",
            "If one of the two target cards is an illegal target by the time Once and Future " +
                "resolves, you'll put the other in the appropriate zone and exile Once and Future. If " +
                "both targets are illegal, the spell won't resolve and you won't exile Once and Future.",
        )
    }
}
