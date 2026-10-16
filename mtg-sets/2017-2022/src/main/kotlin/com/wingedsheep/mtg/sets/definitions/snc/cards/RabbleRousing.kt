package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rabble Rousing
 * {4}{W}
 * Enchantment
 *
 * Hideaway 5 (When this enchantment enters, look at the top five cards of your library, exile
 * one face down, then put the rest on the bottom in a random order.)
 * Whenever you attack with one or more creatures, create that many 1/1 green and white Citizen
 * creature tokens. Then if you control ten or more creatures, you may play the exiled card
 * without paying its mana cost.
 *
 * Hideaway is composed the same way as Fight Rigging. "That many" is the number of creatures you
 * attacked with: the attack trigger captures its declared attackers, read back through
 * [CollectionSlot.TriggerCaptured].
 */
val RabbleRousing = card("Rabble Rousing") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Hideaway 5 (When this enchantment enters, look at the top five cards of your " +
        "library, exile one face down, then put the rest on the bottom in a random order.)\n" +
        "Whenever you attack with one or more creatures, create that many 1/1 green and white " +
        "Citizen creature tokens. Then if you control ten or more creatures, you may play the " +
        "exiled card without paying its mana cost."

    keywordAbility(KeywordAbility.hideaway(5))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val rabbleTop = gather(CardSource.TopOfLibrary(count = 5, player = Player.You))
            val (rabblePicked, rabbleRest) = chooseExactlySplit(
                1,
                from = rabbleTop,
                prompt = "Choose a card to exile face down",
                selectedLabel = "Exile face down",
                remainderLabel = "Put on bottom of library"
            )
            exile(rabblePicked, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
            toLibraryBottom(rabbleRest, order = CardOrder.Random)
        }
    }

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.CreateToken(
            count = DynamicAmounts.distinctEntitiesIn(CollectionSlot.TriggerCaptured),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Citizen"),
        ) then Effects.If(
            condition = Conditions.YouControlAtLeast(10, GameObjectFilter.Creature),
            then = Effects.May(
                Effects.Pipeline {
                    val rabbleLinked = gather(CardSource.FromLinkedExile())
                    run(Effects.PlayFromCollectionWithoutPayingCost(rabbleLinked))
                },
                descriptionOverride = "Play the exiled card without paying its mana cost"
            )
        )
        description = "Whenever you attack with one or more creatures, create that many 1/1 green " +
            "and white Citizen creature tokens. Then if you control ten or more creatures, you may " +
            "play the exiled card without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/206c8529-56df-4ed9-97bc-6c9b5e2a04c4.jpg?1783923156"
        ruling(
            "2022-04-29",
            "\"Hideaway N\" means \"When this permanent enters the battlefield, look at the top N " +
                "cards of your library. Exile one of them face down and put the rest on the bottom " +
                "of your library in a random order. The exiled card gains 'The player who controls " +
                "the permanent that exiled this card may look at this card in the exile zone.'\""
        )
        ruling(
            "2022-04-29",
            "Any player who has controlled a permanent with a hideaway ability since a card was " +
                "exiled with it may look at that card."
        )
        ruling(
            "2022-04-29",
            "Hideaway now causes you to put the rest of the cards on the bottom of your library " +
                "in a random order instead of any order."
        )
    }
}
