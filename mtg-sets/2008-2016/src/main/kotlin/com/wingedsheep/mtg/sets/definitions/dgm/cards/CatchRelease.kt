package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CategoryPickPurpose
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Catch // Release — Dragon's Maze #125
 *
 * Catch is Zealous Conscripts' threaten on any permanent. Release is the overlapping-category edict:
 * every player (APNAP) picks one permanent per listed type, a permanent answers one type only, and
 * each must pick as many as their board allows (`distinctPicks`), then all picks go at once. Fused,
 * Catch resolves first, so the stolen permanent can be one of Release's picks.
 */
val CatchRelease = card("Catch // Release") {
    layout = CardLayout.SPLIT
    colorIdentity = "URW"
    keywords(Keyword.FUSE)

    face("Catch") {
        manaCost = "{1}{U}{R}"
        typeLine = "Sorcery"
        oracleText = "Gain control of target permanent until end of turn. Untap it. It gains haste until end of turn.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val permanent = target(TargetFilter.Permanent)
            effect = Effects.GainControl(permanent, Duration.EndOfTurn) then
                Effects.Untap(permanent) then
                Effects.GrantKeyword(Keyword.HASTE, permanent)
        }
    }

    face("Release") {
        manaCost = "{4}{R}{W}"
        typeLine = "Sorcery"
        oracleText = "Each player sacrifices an artifact, a creature, an enchantment, a land, and a planeswalker of their choice.\n" +
            "Fuse (You may cast one or both halves of this card from your hand.)"
        spell {
            effect = Effects.Pipeline {
                val permanents = gather(GameObjectFilter.Permanent)
                val doomed = chooseOnePerCategory(
                    permanents,
                    listOf(
                        GameObjectFilter.Artifact,
                        GameObjectFilter.Creature,
                        GameObjectFilter.Enchantment,
                        GameObjectFilter.Land,
                        GameObjectFilter.Planeswalker,
                    ),
                    purpose = CategoryPickPurpose.SACRIFICE,
                    distinctPicks = true,
                )
                sacrifice(doomed)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29968873-56f3-4528-ab0b-f11dd67dd162.jpg?1783940017"
        ruling("2013-04-15", "Catch can target any permanent, including one that's untapped or one you already control.")
        ruling("2013-04-15", "If you cast Catch/Release as a fused split spell, you can sacrifice the permanent you just gained control of.")
        ruling("2013-04-15", "To resolve Release, the active player chooses a permanent they control for each of the listed types, then each other player in turn order does the same, then all the chosen permanents are sacrificed simultaneously.")
        ruling("2013-04-15", "If you control an artifact creature, a nonartifact creature, and no other artifacts or creatures, you must sacrifice both permanents. You must choose the artifact creature as the artifact and the nonartifact creature as the creature. The same is true for any combination of types—you sacrifice a number of permanents as close to five as possible.")
        ruling("2013-04-15", "If you're casting a split card with fuse from any zone other than your hand, you can't cast both halves. You'll only be able to cast one half or the other.")
        ruling("2013-04-15", "To cast a fused split spell, pay both of its mana costs. While the spell is on the stack, its mana value is the total amount of mana in both costs.")
        ruling("2013-04-15", "When a fused split spell resolves, follow the instructions of the left half first, then the instructions on the right half.")
    }
}
