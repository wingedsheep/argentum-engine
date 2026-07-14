package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dread Fugue
 * {B}
 * Sorcery
 * Cleave {2}{B} (You may cast this spell for its cleave cost. If you do, remove the words in
 * square brackets.)
 * Target player reveals their hand. You choose a nonland card from it [with mana value 2 or less].
 * That player discards that card.
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * (cheaper) cast is a Duress-style targeted discard limited to cheap spells (nonland, mana value 2
 * or less); paying the cleave cost drops the mana-value cap so you can take any nonland card.
 *
 * The bracket lives inside the *effect* (the card filter the caster chooses under), not the target
 * line — the target ("target player") is identical in both modes. So `cleaveTarget` is left unset
 * and only the effect differs. Both effects are the same Duress-shaped composite (reveal hand →
 * caster chooses one card matching the filter → that player discards it); only the
 * `SelectFromCollectionEffect` filter changes between the modes.
 *
 * `chooser = Chooser.Controller` means the *caster* picks the card (CR "you choose"), and
 * `Player.ContextPlayer(0)` refers back to the shared targeted player for both the gather and the
 * discard destination.
 */
val DreadFugue = card("Dread Fugue") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Cleave {2}{B} (You may cast this spell for its cleave cost. If you do, remove " +
        "the words in square brackets.)\nTarget player reveals their hand. You choose a nonland " +
        "card from it [with mana value 2 or less]. That player discards that card."

    keywordAbility(KeywordAbility.cleave("{2}{B}"))

    spell {
        target = TargetPlayer()

        // Printed (brackets present): choose a nonland card with mana value 2 or less to discard.
        effect = Effects.Composite(
            RevealHandEffect(),
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                storeAs = "revealedHand",
            ),
            SelectFromCollectionEffect(
                from = "revealedHand",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland.manaValueAtMost(2),
                storeSelected = "toDiscard",
                prompt = "Choose a nonland card with mana value 2 or less to discard",
                alwaysPrompt = true,
                showAllCards = true,
            ),
            MoveCollectionEffect(
                from = "toDiscard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                moveType = MoveType.Discard,
            ),
        )

        // Cleaved (brackets removed): choose any nonland card to discard (no mana-value cap).
        cleaveEffect = Effects.Composite(
            RevealHandEffect(),
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                storeAs = "revealedHand",
            ),
            SelectFromCollectionEffect(
                from = "revealedHand",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                storeSelected = "toDiscard",
                prompt = "Choose a nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true,
            ),
            MoveCollectionEffect(
                from = "toDiscard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                moveType = MoveType.Discard,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ad4c472-b8ce-4ae0-a6f0-726ea74722c5.jpg?1783924866"
    }
}
