package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Mycotyrant
 * {1}{B}{G}
 * Legendary Creature — Elder Fungus
 * *|*
 *
 * Trample
 * The Mycotyrant's power and toughness are each equal to the number of creatures you
 * control that are Fungi and/or Saprolings.
 * At the beginning of your end step, create X 1/1 black Fungus creature tokens with
 * "This token can't block," where X is the number of times you descended this turn.
 * (You descend each time a permanent card is put into your graveyard from anywhere.)
 *
 * Modeled from three existing primitives, no engine changes:
 * - Trample via [Keyword.TRAMPLE].
 * - The characteristic-defining P/T is a self-referential count via [dynamicStats] over
 *   [DynamicAmount.AggregateBattlefield] — the same shape as Burrowguard Mentor / Scion of
 *   the Wild, filtered to creatures you control that are Fungi and/or Saprolings with
 *   [GameObjectFilter.withAnySubtype] (subtype Fungus OR Saproling under AND-with-creature).
 * - The end-step trigger reuses the 1/1 black Fungus "can't block" token minted by
 *   Broodrage Mycoid / Synapse Necromage. Here the token *count* is dynamic: X is the
 *   descend COUNT (not the descend-4/8 boolean) via [DynamicAmounts.descendedThisTurn]
 *   (CR 700.11), of which The Mycotyrant is the canonical user.
 */
val TheMycotyrant = card("The Mycotyrant") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Elder Fungus"
    power = 0
    toughness = 0
    oracleText = "Trample\n" +
        "The Mycotyrant's power and toughness are each equal to the number of creatures you control that are Fungi and/or Saprolings.\n" +
        "At the beginning of your end step, create X 1/1 black Fungus creature tokens with \"This token can't block,\" where X is the number of times you descended this turn. " +
        "(You descend each time a permanent card is put into your graveyard from anywhere.)"

    keywords(Keyword.TRAMPLE)
    dynamicStats(
        DynamicAmount.AggregateBattlefield(
            Player.You,
            GameObjectFilter.Creature.withAnySubtype("Fungus", "Saproling")
        )
    )

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = CreateTokenEffect(
            count = DynamicAmounts.descendedThisTurn(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Fungus"),
            staticAbilities = listOf(CantBlock(GroupFilter.source()))
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "235"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/caef93cc-70d0-4cce-9aaa-13c0931b2ef7.jpg?1782694423"
        ruling("2023-11-10", "The Mycotyrant's last ability counts the number of times you descended this turn, not just whether you descended. Each permanent card put into your graveyard from anywhere this turn counts once.")
        ruling("2023-11-10", "A permanent card is an artifact, battle, creature, enchantment, land, or planeswalker card. Tokens are not cards, so a token being put into your graveyard doesn't count as you descending.")
        ruling("2023-11-10", "The Mycotyrant's power and toughness are each equal to the number of creatures you control that are Fungi and/or Saprolings, including The Mycotyrant itself if it's a Fungus. This is a characteristic-defining ability and applies in all zones, though it's only relevant on the battlefield.")
    }
}
