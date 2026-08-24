package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Rag Man
 * {2}{B}{B}
 * Creature — Human Minion
 * 2/1
 * {B}{B}{B}, {T}: Target opponent reveals their hand and discards a creature card at random.
 * Activate only during your turn.
 *
 * The full Gather → Select → Move pipeline, with the selection made by the *engine* rather than by
 * a player: `SelectionMode.Random` is what "at random" means, and it draws from a collection already
 * narrowed to creature cards, so the randomness is over the creatures in hand and not over the whole
 * hand with a creature filter applied afterwards.
 *
 * An opponent with no creature cards in hand reveals and discards nothing — the gather comes up
 * empty and both later steps are silent no-ops, which is the printed behaviour.
 */
val RagMan = card("Rag Man") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Minion"
    power = 2
    toughness = 1
    oracleText = "{B}{B}{B}, {T}: Target opponent reveals their hand and discards a creature card " +
        "at random. Activate only during your turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}{B}{B}"), Costs.Tap)
        val victim = target("target opponent", TargetOpponent())
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn)
        effect = Effects.Composite(
            RevealHandEffect(victim),
            GatherCardsEffect(
                source = CardSource.FromZone(
                    zone = Zone.HAND,
                    player = Player.ContextPlayer(0),
                    filter = GameObjectFilter.Creature,
                ),
                storeAs = "ragManCandidates",
            ),
            SelectFromCollectionEffect(
                from = "ragManCandidates",
                selection = SelectionMode.Random(DynamicAmount.Fixed(1)),
                storeSelected = "ragManVictim",
            ),
            MoveCollectionEffect(
                from = "ragManVictim",
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                moveType = MoveType.Discard,
            ),
        )
        description = "{B}{B}{B}, {T}: Target opponent reveals their hand and discards a creature " +
            "card at random. Activate only during your turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Daniel Gelon"
        flavorText = "\"Aw, he's just a silly, dirty little man. What's to be afraid of?\""
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4c133b8-8383-433f-be96-c47a937287b7.jpg?1783947938"
    }
}
