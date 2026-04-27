package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Celestial Reunion
 * {X}{G}
 * Sorcery
 *
 * As an additional cost to cast this spell, you may choose a creature type and behold two
 * creatures of that type.
 * Search your library for a creature card with mana value X or less, reveal it, put it into
 * your hand, then shuffle. If this spell's additional cost was paid and the revealed card is
 * the chosen type, put that card onto the battlefield instead of putting it into your hand.
 *
 * Implementation note: Oracle text frames the type-choice + behold as a cast-time additional
 * cost. We model the entire spell at resolution time as an optional MayEffect that chooses
 * a creature type and reveals two matching creatures from your battlefield + hand. Because
 * Behold has no cost component (it does not exile or pay anything), evaluating it at
 * resolution time produces equivalent gameplay: the chosen type is stored in
 * `chosenValues["chosenCreatureType"]`, and the conditional placement reads it via
 * `withSubtypeFromVariable`. If the player declined the may, the variable stays unset and
 * `CollectionContainsMatch` resolves to false, sending the revealed card to hand.
 */
val CelestialReunion = card("Celestial Reunion") {
    manaCost = "{X}{G}"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, you may choose a creature type and " +
        "behold two creatures of that type.\n" +
        "Search your library for a creature card with mana value X or less, reveal it, put it " +
        "into your hand, then shuffle. If this spell's additional cost was paid and the revealed " +
        "card is the chosen type, put that card onto the battlefield instead of putting it into " +
        "your hand."

    spell {
        effect = CompositeEffect(
            listOf(
                // Optional: choose a creature type and behold two creatures of that type.
                MayEffect(
                    description_override = "Choose a creature type and behold two creatures of that type?",
                    effect = CompositeEffect(
                        listOf(
                            ChooseOptionEffect(
                                optionType = OptionType.CREATURE_TYPE,
                                storeAs = "chosenCreatureType"
                            ),
                            GatherCardsEffect(
                                source = CardSource.FromMultipleZones(
                                    zones = listOf(Zone.BATTLEFIELD, Zone.HAND),
                                    player = Player.You,
                                    filter = GameObjectFilter.Creature
                                        .withSubtypeFromVariable("chosenCreatureType")
                                ),
                                storeAs = "beholdable"
                            ),
                            SelectFromCollectionEffect(
                                from = "beholdable",
                                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(2)),
                                storeSelected = "beheld",
                                prompt = "Behold two creatures of the chosen type"
                            ),
                            RevealCollectionEffect(from = "beheld")
                        )
                    )
                ),
                // Search library for a creature card with mana value X or less.
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Creature),
                    storeAs = "searchable"
                ),
                FilterCollectionEffect(
                    from = "searchable",
                    filter = CollectionFilter.ManaValueAtMost(DynamicAmount.XValue),
                    storeMatching = "mvOk"
                ),
                SelectFromCollectionEffect(
                    from = "mvOk",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "found",
                    prompt = "Search your library for a creature card with mana value X or less"
                ),
                // Searcher just picked the card — reveal it to opponents only.
                RevealCollectionEffect(from = "found", revealToSelf = false),
                // If beheld and revealed card matches the chosen type → battlefield, else → hand.
                ConditionalEffect(
                    condition = CollectionContainsMatch(
                        collection = "found",
                        filter = GameObjectFilter.Creature
                            .withSubtypeFromVariable("chosenCreatureType")
                    ),
                    effect = MoveCollectionEffect(
                        from = "found",
                        destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                    ),
                    elseEffect = MoveCollectionEffect(
                        from = "found",
                        destination = CardDestination.ToZone(Zone.HAND)
                    )
                ),
                ShuffleLibraryEffect()
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "170"
        artist = "Justin Gerard"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/583b2863-aca1-4dab-9196-ea453b5d9454.jpg?1767863436"
    }
}
