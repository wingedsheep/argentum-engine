package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mardu Charm
 * {R}{W}{B}
 * Instant
 * Choose one —
 * • Mardu Charm deals 4 damage to target creature.
 * • Create two 1/1 white Warrior creature tokens. They gain first strike until end of turn.
 * • Target opponent reveals their hand. You choose a noncreature, nonland card from it.
 *   That player discards that card.
 */
val MarduCharm = card("Mardu Charm") {
    manaCost = "{R}{W}{B}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Mardu Charm deals 4 damage to target creature.\n• Create two 1/1 white Warrior creature tokens. They gain first strike until end of turn.\n• Target opponent reveals their hand. You choose a noncreature, nonland card from it. That player discards that card."

    spell {
        modal(chooseCount = 1) {
            mode("Mardu Charm deals 4 damage to target creature") {
                val t = target("target", TargetCreature())
                effect = DealDamageEffect(4, t)
            }
            mode("Create two 1/1 white Warrior creature tokens with first strike") {
                effect = CreateTokenEffect(
                    count = 2,
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Warrior"),
                    keywords = setOf(Keyword.FIRST_STRIKE),
                    imageUri = "https://cards.scryfall.io/normal/front/a/f/af4c9101-85bf-4a4f-a496-ff6db7b531b7.jpg?1562639979"
                )
            }
            mode("Target opponent reveals their hand, discard a noncreature, nonland card") {
                val t = target("target", TargetOpponent())
                effect = CompositeEffect(
                    listOf(
                        RevealHandEffect(t),
                        GatherCardsEffect(
                            source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                            storeAs = "hand"
                        ),
                        SelectFromCollectionEffect(
                            from = "hand",
                            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                            chooser = Chooser.Controller,
                            filter = GameObjectFilter.Noncreature and GameObjectFilter.Nonland,
                            storeSelected = "toDiscard",
                            prompt = "Choose a noncreature, nonland card to discard",
                            alwaysPrompt = true,
                            showAllCards = true
                        ),
                        MoveCollectionEffect(
                            from = "toDiscard",
                            destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                            moveType = MoveType.Discard
                        )
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c35fb7a3-7c7f-4470-b1ad-5b8709a608e6.jpg?1562793176"
    }
}
