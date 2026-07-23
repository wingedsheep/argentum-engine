package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kellan, Daring Traveler // Journey On
 * {1}{W} // {G}
 * Legendary Creature — Human Faerie Scout // Sorcery — Adventure
 * 2/3
 * Rare — The Lost Caverns of Ixalan #231
 *
 * Kellan, Daring Traveler:
 *   "Whenever Kellan attacks, reveal the top card of your library. If it's a creature card
 *    with mana value 3 or less, put it into your hand. Otherwise, you may put it into your
 *    graveyard."
 *
 * Journey On (Adventure — Sorcery):
 *   "Create X Map tokens, where X is one plus the number of opponents who control an
 *    artifact. (Then exile this card. You may cast the creature later from exile.)"
 *
 * The attack trigger gathers the top card into a `revealed` collection (with `revealed = true`
 * so every player sees it regardless of branch), then branches on whether that card is a
 * creature card with mana value ≤ 3 via [CollectionContainsMatch]. On a match it moves to
 * hand; otherwise the controller *may* move it to the graveyard (declining leaves it on top
 * of the library — no default move). Empty library reveals nothing and neither branch fires.
 *
 * Journey On's X is [DynamicAmount.Add] of one and [DynamicAmount.CountPlayersWith] over
 * [Player.EachOpponent] with the per-player "controls an artifact" test ([Conditions.ControlArtifact],
 * whose `Player.You` rebinds to each candidate opponent). Zero qualifying opponents → X = 1.
 * (CR 715: casting the Adventure exiles the card on resolution and lets the caster cast it as
 * the creature spell while it remains in exile.)
 */
val KellanDaringTraveler = card("Kellan, Daring Traveler") {
    manaCost = "{1}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Faerie Scout"
    oracleText = "Whenever Kellan attacks, reveal the top card of your library. If it's a " +
        "creature card with mana value 3 or less, put it into your hand. Otherwise, you may " +
        "put it into your graveyard."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.You),
                    storeAs = "revealed",
                    revealed = true
                ),
                ConditionalEffect(
                    condition = CollectionContainsMatch(
                        collection = "revealed",
                        filter = GameObjectFilter.Creature.manaValueAtMost(3)
                    ),
                    effect = MoveCollectionEffect(
                        from = "revealed",
                        destination = CardDestination.ToZone(Zone.HAND, Player.You)
                    ),
                    elseEffect = MayEffect(
                        MoveCollectionEffect(
                            from = "revealed",
                            destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You)
                        ),
                        descriptionOverride = "Put the revealed card into your graveyard?"
                    )
                )
            )
        )
    }

    adventure("Journey On") {
        manaCost = "{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Create X Map tokens, where X is one plus the number of opponents who " +
            "control an artifact. (Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.CreateMapToken(
                DynamicAmount.Add(
                    DynamicAmount.Fixed(1),
                    DynamicAmount.CountPlayersWith(
                        scope = Player.EachOpponent,
                        condition = Conditions.ControlArtifact
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01739030-c280-492b-a5c9-b3e9f6debc6d.jpg?1782694426"
    }
}
