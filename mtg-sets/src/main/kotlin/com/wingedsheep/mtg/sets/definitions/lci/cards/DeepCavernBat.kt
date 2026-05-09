package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Deep-Cavern Bat
 * {1}{B}
 * Creature — Bat
 * 1/1
 *
 * Flying, lifelink
 * When this creature enters, look at target opponent's hand. You may exile a
 * nonland card from it until this creature leaves the battlefield.
 *
 * The exile-until-leaves linkage is wired via `MoveCollectionEffect.linkToSource`
 * (storing chosen IDs in the bat's `LinkedExileComponent`) and
 * `Effects.ReturnLinkedExileToHand()` on the LeavesBattlefield trigger.
 */
val DeepCavernBat = card("Deep-Cavern Bat") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bat"
    power = 1
    toughness = 1
    oracleText = "Flying, lifelink\n" +
        "When this creature enters, look at target opponent's hand. You may exile a " +
        "nonland card from it until this creature leaves the battlefield."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponent = target("target opponent", Targets.Opponent)
        effect = CompositeEffect(
            listOf(
                LookAtTargetHandEffect(opponent),
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "opponentHand"
                ),
                SelectFromCollectionEffect(
                    from = "opponentHand",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Nonland,
                    storeSelected = "exiledCard",
                    prompt = "You may exile a nonland card from target opponent's hand",
                    showAllCards = true,
                    alwaysPrompt = true
                ),
                MoveCollectionEffect(
                    from = "exiledCard",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0)),
                    linkToSource = true
                )
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileToHand()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Campbell White"
        flavorText = "The first shriek locates the prey. The second immobilizes it."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69c68c95-b788-43b1-9f22-1b22c5a00b25.jpg?1699044121"
        ruling("2023-11-10", "If Deep-Cavern Bat leaves the battlefield before its last ability resolves, you'll still look at the target opponent's hand, but you won't exile any cards from it.")
    }
}
