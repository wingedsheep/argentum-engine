package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.withSubtypeFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.references.Player

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
 * cost. We model the entire spell at resolution time as an optional Effects.May that chooses
 * a creature type and reveals two matching creatures from your battlefield + hand. Because
 * Behold has no cost component (it does not exile or pay anything), evaluating it at
 * resolution time produces equivalent gameplay: the chosen type is stored in
 * `chosenValues["chosenCreatureType"]`, and the conditional placement reads it via
 * `withSubtypeFromVariable`. If the player declined the may, the variable stays unset and
 * `CollectionContainsMatch` resolves to false, sending the revealed card to hand.
 */
val CelestialReunion = card("Celestial Reunion") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, you may choose a creature type and " +
        "behold two creatures of that type.\n" +
        "Search your library for a creature card with mana value X or less, reveal it, put it " +
        "into your hand, then shuffle. If this spell's additional cost was paid and the revealed " +
        "card is the chosen type, put that card onto the battlefield instead of putting it into " +
        "your hand."

    spell {
        effect = Effects.Pipeline {
            // Optional: choose a creature type and behold two creatures of that type. The chosen
            // type is written inside the optional branch but read after it (unset if declined).
            val chosenType = runStoringChoice { chosenTypeKey ->
                Effects.May(
                    descriptionOverride = "Choose a creature type and behold two creatures of that type?",
                    effect = Effects.Pipeline {
                        run(Effects.ChooseOption(OptionType.CREATURE_TYPE, storeAs = chosenTypeKey))
                        val beholdable = gather(
                            CardSource.FromMultipleZones(
                                zones = listOf(Zone.BATTLEFIELD, Zone.HAND),
                                player = Player.You,
                                filter = GameObjectFilter.Creature.withSubtypeFromVariable(chosenTypeKey)
                            )
                        )
                        val beheld = chooseExactly(2, from = beholdable, prompt = "Behold two creatures of the chosen type")
                        reveal(beheld)
                    }
                )
            }
            // Search library for a creature card with mana value X or less.
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Creature),
                search = true
            )
            val mvOk = filter(searchable, GameObjectFilter.Any.manaValueAtMostDynamic(DynamicAmounts.xValue()))
            val found = chooseUpTo(
                1,
                from = mvOk,
                prompt = "Search your library for a creature card with mana value X or less"
            )
            // Searcher just picked the card — reveal it to opponents only.
            reveal(found, revealToSelf = false)
            // If beheld and revealed card matches the chosen type → battlefield, else → hand.
            run(Effects.If(
                condition = whenMatches(found, GameObjectFilter.Creature.withSubtypeFromVariable(chosenType)),
                then = Effects.Pipeline { move(found, CardDestination.ToZone(Zone.BATTLEFIELD)) },
                otherwise = Effects.Pipeline { toHand(found) }
            ))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "170"
        artist = "Justin Gerard"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/583b2863-aca1-4dab-9196-ea453b5d9454.jpg?1767863436"
    }
}
