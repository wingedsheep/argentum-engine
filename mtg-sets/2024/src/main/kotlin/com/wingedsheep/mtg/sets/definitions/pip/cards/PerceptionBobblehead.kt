package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Perception Bobblehead — the Sunbird's Invocation shape with a private look instead of a reveal:
 * X (Bobbleheads you control) is counted once as the ability resolves; the cards stay in the
 * library while you look; the chosen spell (MV ≤ 3) is cast during resolution, and the rest go to
 * the bottom in a random order.
 */
val PerceptionBobblehead = card("Perception Bobblehead") {
    manaCost = "{3}"
    typeLine = "Artifact — Bobblehead"
    oracleText = "{T}: Add one mana of any color.\n" +
        "{3}, {T}: Look at the top X cards of your library, where X is the number of Bobbleheads you control. You may cast a spell with mana value 3 or less from among them without paying its mana cost. Put the rest on the bottom of your library in a random order."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        description = "Look at the top X cards of your library (X = Bobbleheads you control). You may cast a spell with mana value 3 or less from among them for free; put the rest on the bottom in a random order."
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.Pipeline {
            val looked = gather(
                CardSource.TopOfLibrary(
                    count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Bobblehead")).count(),
                    player = Player.You,
                ),
            )
            val (chosen, toBottom) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Nonland.manaValueAtMost(3),
                showAllCards = true,
                prompt = "You may cast a spell with mana value 3 or less from among these cards without paying its mana cost.",
                selectedLabel = "Cast for free",
                remainderLabel = "Put on bottom",
            )
            move(
                toBottom,
                CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                order = CardOrder.Random,
            )
            run(Effects.CastFromCollectionWithoutPayingCost(from = chosen))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Marina Ortega Lorente"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2a8c89c-2cd1-4f68-9758-449177be2880.jpg?1783912377"
        ruling("2024-03-08", "The value of X is determined only once, as Perception Bobblehead’s last ability resolves.")
        ruling("2024-03-08", "You choose whether to cast a spell from among the cards you’re looking at as Perception Bobblehead’s last ability resolves. You can’t wait to cast one later in the turn. Timing restrictions based on the card’s types are ignored.")
        ruling("2024-03-08", "If you cast a spell “without paying its mana cost,” you can’t choose to cast it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If the card has any mandatory additional costs, those must be paid to cast the spell.")
        ruling("2024-03-08", "If a spell you cast has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
