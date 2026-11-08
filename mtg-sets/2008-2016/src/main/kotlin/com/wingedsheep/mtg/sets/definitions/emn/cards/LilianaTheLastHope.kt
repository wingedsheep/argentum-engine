package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Liliana, the Last Hope — Eldritch Moon #93
 * {1}{B}{B} · Legendary Planeswalker — Liliana · Starting loyalty 3
 *
 * +1: Up to one target creature gets -2/-1 until your next turn.
 * −2: Mill two cards, then you may return a creature card from your graveyard to your hand.
 * −7: You get an emblem with "At the beginning of your end step, create X 2/2 black Zombie
 *     creature tokens, where X is two plus the number of Zombies you control."
 *
 * The +1 is Dovin Baan's optional-target stat change on [Duration.UntilYourNextTurn]. The −2 is
 * Grapple with the Past's mill-then-pick: the return isn't targeted (ruling), so the cards just
 * milled are legal picks, and "you may" is a `chooseUpTo(1)` — declining is choosing none. The
 * emblem is Ajani, Adversary of Tyrants' end-step emblem trigger with a [DynamicAmounts.count]
 * of Zombie permanents you control plus two, counted as the trigger resolves (zero Zombies still
 * makes two tokens).
 */
val LilianaTheLastHope = card("Liliana, the Last Hope") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Liliana"
    startingLoyalty = 3
    oracleText = "+1: Up to one target creature gets -2/-1 until your next turn.\n" +
        "−2: Mill two cards, then you may return a creature card from your graveyard to your hand.\n" +
        "−7: You get an emblem with \"At the beginning of your end step, create X 2/2 black Zombie " +
        "creature tokens, where X is two plus the number of Zombies you control.\""

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(-2, -1, creature, Duration.UntilYourNextTurn)
        description = "Up to one target creature gets -2/-1 until your next turn."
    }

    loyaltyAbility(-2) {
        effect = Effects.Pipeline {
            // "Mill two cards, ..."
            run(Patterns.Library.mill(2))
            // "... then you may return a creature card from your graveyard to your hand."
            val graveyard = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature)
            )
            val chosen = chooseUpTo(
                1,
                from = graveyard,
                showAllCards = true,
                prompt = "You may return a creature card from your graveyard to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        }
        description = "Mill two cards, then you may return a creature card from your graveyard to your hand."
    }

    loyaltyAbility(-7) {
        val emblemText = "At the beginning of your end step, create X 2/2 black Zombie creature " +
            "tokens, where X is two plus the number of Zombies you control."
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.beginningOf(Step.END)
                effect = Effects.CreateToken(
                    count = 2 + DynamicAmounts.count(
                        Player.You,
                        Zone.BATTLEFIELD,
                        GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE)
                    ),
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie"),
                    imageUri = "https://cards.scryfall.io/normal/front/1/5/15f1f43a-cbdb-4d1b-a254-dadcb2469784.jpg?1783937418"
                )
                description = emblemText
            },
            emblem = true,
            descriptionOverride = emblemText
        )
        description = "You get an emblem with \"$emblemText\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "93"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa6ac7d2-0127-49a8-b7d2-a38308d3c27b.jpg?1783937482"

        ruling("2016-07-13", "You can activate Liliana's first ability without any targets just to add a loyalty counter to her.")
        ruling("2016-07-13", "Liliana's second ability doesn't target the card to return to your hand. After putting the top two cards of your library into your graveyard, you may choose from among any creature cards there.")
        ruling("2016-07-13", "While resolving the triggered ability of Liliana's emblem, if you control zero Zombies, you'll get two Zombie tokens.")
    }
}
