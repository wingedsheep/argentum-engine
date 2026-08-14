package com.wingedsheep.mtg.sets.definitions.msh.cards

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
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Klaw, Sonic Subjugator — Marvel Super Heroes #103
 * {2}{B} · Legendary Creature — Human Rogue Villain · 2/2
 *
 * Sonic Attack — When Klaw enters, target player reveals a number of cards from their hand equal
 * to one plus the number of creature cards in your graveyard. You choose one of them. That player
 * discards that card.
 *
 * "Sonic Attack" is an ability word (CR 207.2c) — flavor only, no rules meaning.
 *
 * The body is the Blackmail / Cabal Interrogator reveal-choose-discard pipeline: gather the target
 * player's hand, have *them* pick the reveal set, have the controller pick one of those, then
 * discard it. The only twist is the reveal count, which is computed at resolution as
 * `1 + creature cards in your graveyard` ([DynamicAmount.Add] over [DynamicAmount.Count] on
 * `Player.You` / [Zone.GRAVEYARD]) — "your" is Klaw's controller, not the target player. A hand
 * smaller than that number simply reveals everything ([SelectionMode.ChooseExactly] auto-selects
 * the whole collection when it can't reach the requested count), and an empty hand makes both
 * selection steps and the discard no-ops.
 */
val KlawSonicSubjugator = card("Klaw, Sonic Subjugator") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Rogue Villain"
    power = 2
    toughness = 2
    oracleText = "Sonic Attack — When Klaw enters, target player reveals a number of cards from " +
        "their hand equal to one plus the number of creature cards in your graveyard. You choose " +
        "one of them. That player discards that card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target("target player", Targets.Player)
        effect = Effects.Composite(
            listOf(
                // 1. Gather the target player's hand.
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "klawHand"
                ),
                // 2. That player reveals 1 + creature cards in your graveyard of them.
                SelectFromCollectionEffect(
                    from = "klawHand",
                    selection = SelectionMode.ChooseExactly(
                        DynamicAmount.Add(
                            DynamicAmount.Fixed(1),
                            DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature)
                        )
                    ),
                    chooser = Chooser.TargetPlayer,
                    storeSelected = "klawRevealed",
                    prompt = "Choose cards to reveal"
                ),
                // 3. Klaw's controller chooses one of the revealed cards.
                SelectFromCollectionEffect(
                    from = "klawRevealed",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    storeSelected = "klawChosen",
                    prompt = "Choose a card that player discards"
                ),
                // 4. That player discards it.
                MoveCollectionEffect(
                    from = "klawChosen",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    moveType = MoveType.Discard
                )
            )
        )
        description = "Sonic Attack — When Klaw enters, target player reveals a number of cards " +
            "from their hand equal to one plus the number of creature cards in your graveyard. " +
            "You choose one of them. That player discards that card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Andreia Ugrai"
        flavorText = "\"Listen to the sound of death. Hear the sound of your own cells exploding.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c79a86f8-24e9-49a2-8b1c-72a72fed1985.jpg?1783902943"
    }
}
