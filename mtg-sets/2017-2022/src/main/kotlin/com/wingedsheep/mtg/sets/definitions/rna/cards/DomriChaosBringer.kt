package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Domri, Chaos Bringer — Ravnica Allegiance #166
 * {2}{R}{G} Legendary Planeswalker — Domri, loyalty 5
 *
 * +1: Add {R} or {G}. If that mana is spent on a creature spell, it gains riot.
 * −3: Look at the top four cards of your library. You may reveal up to two creature cards from
 *     among them and put them into your hand. Put the rest on the bottom of your library in a
 *     random order.
 * −8: You get an emblem with "At the beginning of each end step, create a 4/4 red and green Beast
 *     creature token with trample."
 *
 * The +1's riot is frozen onto the creature spell its mana pays for (`GrantsKeywordWhenSpent`,
 * no end date — "it gains haste indefinitely" per the ruling), and the spell's enters-with
 * counter-or-haste choice is synthesized from that grant as it resolves.
 */
val DomriChaosBringer = card("Domri, Chaos Bringer") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Domri"
    startingLoyalty = 5
    oracleText = "+1: Add {R} or {G}. If that mana is spent on a creature spell, it gains riot. " +
        "(It enters with your choice of a +1/+1 counter or haste.)\n" +
        "−3: Look at the top four cards of your library. You may reveal up to two creature cards from " +
        "among them and put them into your hand. Put the rest on the bottom of your library in a random order.\n" +
        "−8: You get an emblem with \"At the beginning of each end step, create a 4/4 red and green " +
        "Beast creature token with trample.\""

    loyaltyAbility(+1) {
        effect = Effects.AddManaOfChoice(
            colorSet = ManaColorSet.Specific(setOf(Color.RED, Color.GREEN)),
            riders = setOf(
                ManaSpellRider.GrantsKeywordWhenSpent(
                    keyword = Keyword.RIOT,
                    spellFilter = GameObjectFilter.Creature,
                    duration = Duration.Permanent,
                )
            ),
        )
    }

    loyaltyAbility(-3) {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(4))
            val (kept, rest) = chooseUpToSplit(
                2,
                from = looked,
                filter = GameObjectFilter.Creature,
                prompt = "You may reveal up to two creature cards to put into your hand.",
                showAllCards = true
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    loyaltyAbility(-8) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.anyPlayer.beginningOf(Step.END),
                effect = Effects.CreateToken(
                    power = 4,
                    toughness = 4,
                    colors = setOf(Color.RED, Color.GREEN),
                    creatureTypes = setOf("Beast"),
                    keywords = setOf(Keyword.TRAMPLE),
                    imageUri = "https://cards.scryfall.io/normal/front/8/7/87ec9ade-1e06-43c9-97df-55465cabdf32.jpg?1783933602"
                )
            ),
            descriptionOverride = "At the beginning of each end step, create a 4/4 red and green Beast creature token with trample."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "166"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f56bbf3-3884-495a-b9cd-6585d86f76f1.jpg?1783933653"
        ruling("2019-01-25", "Because it's a loyalty ability, Domri's first ability isn't a mana ability. It can be activated only any time you could cast a sorcery. It uses the stack and can be responded to.")
        ruling("2019-01-25", "Riot is a replacement effect. Players can't respond to your choice of +1/+1 counter or haste, and they can't take actions while the creature is on the battlefield without one or the other.")
        ruling("2019-01-25", "If a creature entering the battlefield has riot but can't have a +1/+1 counter put onto it, it gains haste.")
        ruling("2019-01-25", "If you choose for the creature to gain haste, it gains haste indefinitely. It won't lose it as the turn ends or as another player gains control of it.")
        ruling("2019-01-25", "If a creature enters the battlefield with two instances of riot, you may choose to have it get two +1/+1 counters, one +1/+1 counter and haste, or two instances of haste. Multiple instances of haste on the same creature are redundant, but we're not going to tell the Gruul how to live their lives.")
    }
}
