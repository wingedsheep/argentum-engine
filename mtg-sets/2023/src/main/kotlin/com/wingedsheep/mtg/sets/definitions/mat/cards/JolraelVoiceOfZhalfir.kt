package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Jolrael, Voice of Zhalfir
 * {2}{G}{U}
 * Legendary Creature — Human Druid
 * 3/3
 * At the beginning of combat on your turn, up to one target land you control becomes an X/X green
 * and blue Bird creature with flying and haste until end of turn, where X is the number of cards in
 * your hand. It's still a land.
 * Whenever a land creature you control deals combat damage to a player, draw a card.
 *
 * - `BecomeCreature`'s `power`/`toughness` are evaluated once as the ability resolves and stamped as
 *   a fixed set-P/T effect, so X locks in (ruling) rather than tracking the hand afterwards.
 * - The draw trigger watches any land creature you control, not only lands Jolrael animated (ruling).
 */
val JolraelVoiceOfZhalfir = card("Jolrael, Voice of Zhalfir") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Druid"
    power = 3
    toughness = 3
    oracleText = "At the beginning of combat on your turn, up to one target land you control becomes an X/X " +
        "green and blue Bird creature with flying and haste until end of turn, where X is the number of " +
        "cards in your hand. It's still a land.\n" +
        "Whenever a land creature you control deals combat damage to a player, draw a card."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()), optional = true)
        effect = Effects.BecomeCreature(
            target = land,
            power = DynamicAmounts.cardsInYourHand(),
            toughness = DynamicAmounts.cardsInYourHand(),
            keywords = setOf(Keyword.FLYING, Keyword.HASTE),
            creatureTypes = setOf("Bird"),
            colors = setOf(Color.GREEN.name, Color.BLUE.name),
            duration = Duration.EndOfTurn
        )
        description = "At the beginning of combat on your turn, up to one target land you control becomes an " +
            "X/X green and blue Bird creature with flying and haste until end of turn, where X is the number " +
            "of cards in your hand. It's still a land."
    }

    triggeredAbility {
        trigger = Triggers.a((GameObjectFilter.Land and GameObjectFilter.Creature).youControl())
            .dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Lorenzo Mastroianni"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcb93216-885c-4ec5-8b88-6cd51f593c8b.jpg?1783916516"
        ruling(
            "2023-05-12",
            "Use the number of cards in your hand as Jolrael's first ability resolves to determine the value of X. Once the ability resolves, that value is locked in. The power and toughness of the land creature won't change if the number of cards in your hand changes later in the turn."
        )
        ruling(
            "2023-05-12",
            "Jolrael's last ability applies to any land creature you control that deals combat damage to a player, not only lands that become land creatures because of Jolrael's first ability."
        )
    }
}
